package com.robsen.robsen.quotation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.business.MarketplaceVerificationPolicy;
import com.robsen.robsen.common.Status;
import com.robsen.robsen.customerOrder.*;
import com.robsen.robsen.listingInventory.InventoryService;
import com.robsen.robsen.orderItem.OrderItem;
import com.robsen.robsen.orderItem.OrderItemRepo;
import com.robsen.robsen.product.ProductRepo;
import com.robsen.robsen.productListing.ProductListing;
import com.robsen.robsen.productListing.ProductListingRepo;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class QuotationService {
    private final QuotationRequestRepo requestRepo;
    private final QuotationRequestItemRepo requestItemRepo;
    private final SupplierQuotationRepo quotationRepo;
    private final SupplierQuotationItemRepo quotationItemRepo;
    private final BusinessRepo businessRepo;
    private final ProductRepo productRepo;
    private final ProductListingRepo listingRepo;
    private final BusinessAccessService access;
    private final CustomerOrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final OrderStatusHistoryRepo historyRepo;
    private final InventoryService inventory;
    private final MarketplaceVerificationPolicy verificationPolicy;

    public QuotationService(QuotationRequestRepo requestRepo, QuotationRequestItemRepo requestItemRepo,
            SupplierQuotationRepo quotationRepo, SupplierQuotationItemRepo quotationItemRepo, BusinessRepo businessRepo,
            ProductRepo productRepo, ProductListingRepo listingRepo, BusinessAccessService access,
            CustomerOrderRepo orderRepo, OrderItemRepo orderItemRepo, OrderStatusHistoryRepo historyRepo,
            InventoryService inventory, MarketplaceVerificationPolicy verificationPolicy) {
        this.requestRepo=requestRepo; this.requestItemRepo=requestItemRepo; this.quotationRepo=quotationRepo;
        this.quotationItemRepo=quotationItemRepo; this.businessRepo=businessRepo; this.productRepo=productRepo;
        this.listingRepo=listingRepo; this.access=access; this.orderRepo=orderRepo; this.orderItemRepo=orderItemRepo;
        this.historyRepo=historyRepo; this.inventory=inventory; this.verificationPolicy=verificationPolicy;
    }

    @Transactional
    public RequestView create(CreateRequest input) {
        access.requireAccess(input.buyerBusinessId(), "quotation-requests:CREATE");
        if (input.title()==null || input.title().isBlank() || input.items()==null || input.items().isEmpty()) bad("An RFQ title and at least one item are required.");
        if (input.responseDeadline()!=null && !input.responseDeadline().isAfter(LocalDateTime.now())) bad("The response deadline must be in the future.");
        Business buyer=businessRepo.findById(input.buyerBusinessId()).orElseThrow(()->notFound("Buyer business not found."));
        QuotationRequest rfq=new QuotationRequest(); rfq.setBuyerBusiness(buyer); rfq.setTitle(input.title().trim());
        rfq.setDescription(input.description()); rfq.setDeliveryLocation(input.deliveryLocation()); rfq.setResponseDeadline(input.responseDeadline());
        rfq.setRfqNumber("RFQ-"+UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase());
        rfq.setCreatedByUserId(access.currentUserId()); rfq.setStatus("DRAFT"); rfq=requestRepo.save(rfq);
        for (RequestItemInput item:input.items()) {
            if(item.quantity()<=0) bad("Requested quantities must be positive.");
            QuotationRequestItem line=new QuotationRequestItem(); line.setRequest(rfq);
            line.setProduct(productRepo.findById(item.productId()).filter(com.robsen.robsen.product.Product::isActive).orElseThrow(()->notFound("Active product not found.")));
            line.setQuantity(item.quantity()); line.setSpecifications(item.specifications()); requestItemRepo.save(line);
        }
        return requestView(rfq);
    }

    @Transactional
    public RequestView publish(Long id) {
        QuotationRequest rfq=requestRepo.findByIdForUpdate(id).orElseThrow(()->notFound("RFQ not found."));
        access.requireAccess(rfq.getBuyerBusiness().getId(), "quotation-requests:UPDATE");
        if(!"DRAFT".equals(rfq.getStatus())) conflict("Only a draft RFQ can be published.");
        if(requestItemRepo.findByRequestId(id).isEmpty()) bad("An RFQ must contain at least one product.");
        if(rfq.getResponseDeadline()!=null && !rfq.getResponseDeadline().isAfter(LocalDateTime.now())) conflict("The response deadline has passed.");
        rfq.setStatus("OPEN"); return requestView(requestRepo.save(rfq));
    }

    @Transactional(readOnly=true)
    public List<RequestView> requests() {
        if(access.isSuperAdmin()) return requestRepo.findAll().stream().map(this::requestView).toList();
        List<Long> ids=access.accessibleBusinessIds(); if(ids.isEmpty()) return List.of();
        List<RequestView> own=requestRepo.findByBuyerBusinessIdIn(ids).stream()
                .filter(r->access.canAccess(r.getBuyerBusiness().getId(),"quotation-requests:READ"))
                .map(this::requestView).toList();
        List<RequestView> open=requestRepo.findByStatus("OPEN").stream()
                .filter(r->!ids.contains(r.getBuyerBusiness().getId()))
                .filter(r->r.getResponseDeadline()==null || r.getResponseDeadline().isAfter(LocalDateTime.now()))
                .filter(r->ids.stream().anyMatch(b->access.canAccess(b,"supplier-quotations:READ")))
                .map(this::requestView).toList();
        return java.util.stream.Stream.concat(own.stream(),open.stream()).distinct().toList();
    }

    @Transactional
    public QuotationView submit(Long requestId, CreateQuotation input) {
        QuotationRequest rfq=requestRepo.findById(requestId).orElseThrow(()->notFound("RFQ not found."));
        access.requireAccess(input.supplierBusinessId(),"supplier-quotations:CREATE");
        Business supplier=businessRepo.findById(input.supplierBusinessId()).filter(Business::isActive)
                .orElseThrow(()->notFound("Active supplier business not found."));
        if(!verificationPolicy.canAcceptOrders(supplier)) conflict("Supplier verification is required to submit a quotation.");
        if(!"OPEN".equals(rfq.getStatus()) || (rfq.getResponseDeadline()!=null && !rfq.getResponseDeadline().isAfter(LocalDateTime.now()))) conflict("This RFQ is not accepting quotations.");
        if(input.deliveryLeadDays()<0) bad("Delivery lead days cannot be negative.");
        if(input.validUntil()!=null && !input.validUntil().isAfter(LocalDateTime.now())) bad("Quotation validity must be in the future.");
        if(input.items()==null || input.items().isEmpty()) bad("A quotation must include at least one RFQ item.");
        SupplierQuotation quote=new SupplierQuotation(); quote.setRequest(rfq); quote.setSupplierBusiness(supplier);
        quote.setQuotationNumber("QT-"+UUID.randomUUID().toString().replace("-", "").substring(0,12).toUpperCase());
        quote.setCreatedByUserId(access.currentUserId()); quote.setStatus("SUBMITTED"); quote.setSubmittedAt(LocalDateTime.now());
        quote.setValidUntil(input.validUntil()); quote.setDeliveryLeadDays(input.deliveryLeadDays());
        quote.setPaymentTerms(input.paymentTerms()); quote.setDeliveryTerms(input.deliveryTerms()); quote.setRemarks(input.remarks());
        List<SupplierQuotation> old=quotationRepo.findByRequestId(requestId).stream().filter(q->supplier.getId().equals(q.getSupplierBusiness().getId()) && "SUBMITTED".equals(q.getStatus())).toList();
        if(!old.isEmpty()) quote.setPreviousVersionId(old.getLast().getId());
        quote=quotationRepo.save(quote);
        List<QuotationRequestItem> requested=requestItemRepo.findByRequestId(requestId);
        java.util.Set<Long> covered = new java.util.HashSet<>();
        for(QuotationItemInput line:input.items()) {
            if (!covered.add(line.requestItemId())) bad("A requested product can only appear once in a quotation.");
            QuotationRequestItem requestedItem=requested.stream().filter(row->row.getId().equals(line.requestItemId())).findFirst()
                    .orElseThrow(()->bad("Quotation contains an item outside this RFQ."));
            ProductListing listing=listingRepo.findById(line.productListingId()).filter(ProductListing::isActive)
                    .orElseThrow(()->notFound("Active product listing not found."));
            if(!supplier.getId().equals(listing.getBusiness().getId()) || !requestedItem.getProduct().getId().equals(listing.getProduct().getId())) bad("Listing must belong to the supplier and match the requested product.");
            if(line.unitPrice()==null || line.unitPrice().signum()<0) bad("Quoted prices cannot be negative.");
            if(line.offeredQuantity()<requestedItem.getQuantity() || line.minimumOrderQuantity()<1
                    || line.minimumOrderQuantity()>line.offeredQuantity()) bad("Offered quantity and minimum order quantity are invalid.");
            SupplierQuotationItem item=new SupplierQuotationItem(); item.setQuotation(quote); item.setRequestItem(requestedItem);
            item.setProductListing(listing); item.setOfferedQuantity(line.offeredQuantity()); item.setMinimumOrderQuantity(line.minimumOrderQuantity()); item.setUnitPrice(line.unitPrice()); quotationItemRepo.save(item);
        }
        if(!covered.equals(requested.stream().map(QuotationRequestItem::getId).collect(java.util.stream.Collectors.toSet()))) bad("A quotation must cover every requested product.");
        return quotationView(quote);
    }

    @Transactional(readOnly=true)
    public List<QuotationView> quotations(Long requestId) {
        QuotationRequest rfq=requestRepo.findById(requestId).orElseThrow(()->notFound("RFQ not found."));
        if(access.canAccess(rfq.getBuyerBusiness().getId(),"quotation-requests:READ")) return quotationRepo.findByRequestId(requestId).stream().map(this::quotationView).toList();
        return quotationRepo.findByRequestId(requestId).stream().filter(q->access.canAccess(q.getSupplierBusiness().getId(),"supplier-quotations:READ"))
                .map(this::quotationView).toList();
    }

    @Transactional
    public OrderView accept(Long requestId, Long quotationId) {
        QuotationRequest rfq=requestRepo.findByIdForUpdate(requestId).orElseThrow(()->notFound("RFQ not found."));
        access.requireAccess(rfq.getBuyerBusiness().getId(),"quotation-requests:UPDATE");
        if(!"OPEN".equals(rfq.getStatus()) || rfq.getAcceptedQuotationId()!=null
                || (rfq.getResponseDeadline()!=null && !rfq.getResponseDeadline().isAfter(LocalDateTime.now()))) conflict("This RFQ has already closed or passed its response deadline.");
        SupplierQuotation quote=quotationRepo.findById(quotationId).filter(q->q.getRequest().getId().equals(requestId))
                .orElseThrow(()->notFound("Quotation does not belong to this RFQ."));
        if(!"SUBMITTED".equals(quote.getStatus()) || (quote.getValidUntil()!=null && !quote.getValidUntil().isAfter(LocalDateTime.now()))) conflict("Quotation is not valid for acceptance.");
        if(!quote.getSupplierBusiness().isActive() || !verificationPolicy.canAcceptOrders(quote.getSupplierBusiness())) conflict("Supplier can no longer accept wholesale orders.");
        List<SupplierQuotationItem> lines=quotationItemRepo.findByQuotationId(quote.getId());
        if(lines.isEmpty()) bad("Quotation contains no items.");
        CustomerOrder order=new CustomerOrder(); order.setBuyerBusiness(rfq.getBuyerBusiness()); order.setSupplierBusiness(quote.getSupplierBusiness());
        order.setOrderNumber("RB-"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase()); order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now()); order.setQuotationId(quote.getId()); order.setSubtotal(BigDecimal.ZERO); order=orderRepo.save(order);
        BigDecimal subtotal=BigDecimal.ZERO;
        for(SupplierQuotationItem line:lines) {
            int quantity=line.getOfferedQuantity();
            if(quantity<line.getMinimumOrderQuantity() || quantity>line.getProductListing().getAvailableQuantity()) conflict("Quoted quantity is no longer available.");
            inventory.reservation(line.getProductListing().getId(),quantity,false,order.getId(),"RFQ quotation accepted");
            OrderItem orderItem=new OrderItem(); orderItem.setOrder(order); orderItem.setProductListing(line.getProductListing());
            orderItem.setQuantity(quantity); orderItem.setUnitPrice(line.getUnitPrice());
            orderItem.setLineTotal(line.getUnitPrice().multiply(BigDecimal.valueOf(quantity))); subtotal=subtotal.add(orderItem.getLineTotal()); orderItemRepo.save(orderItem);
        }
        order.setSubtotal(subtotal); order=orderRepo.save(order);
        OrderStatusHistory history=new OrderStatusHistory(); history.setOrder(order); history.setNewStatus("PENDING");
        history.setChangedByUserId(access.currentUserId()); history.setRemarks("Created from accepted supplier quotation "+quote.getQuotationNumber()); historyRepo.save(history);
        quote.setStatus("ACCEPTED"); quotationRepo.save(quote);
        quotationRepo.findByRequestId(requestId).stream().filter(other->!other.getId().equals(quote.getId()) && "SUBMITTED".equals(other.getStatus())).forEach(other->{other.setStatus("REJECTED");quotationRepo.save(other);});
        rfq.setStatus("ACCEPTED"); rfq.setAcceptedQuotationId(quote.getId()); requestRepo.save(rfq);
        return new OrderView(order.getId(),order.getOrderNumber(),order.getStatus(),order.getSubtotal(),quote.getQuotationNumber());
    }

    @Transactional(readOnly=true)
    public List<QuotationView> myQuotations() {
        if(access.isSuperAdmin()) return quotationRepo.findAll().stream().map(this::quotationView).toList();
        List<Long> ids=access.accessibleBusinessIds(); if(ids.isEmpty()) return List.of();
        return quotationRepo.findBySupplierBusinessIdIn(ids).stream()
                .filter(q->access.canAccess(q.getSupplierBusiness().getId(),"supplier-quotations:READ"))
                .map(this::quotationView).toList();
    }

    private RequestView requestView(QuotationRequest r) { return new RequestView(r.getId(),r.getRfqNumber(),r.getBuyerBusiness().getId(),r.getBuyerBusiness().getLegalName(),r.getTitle(),r.getDescription(),r.getStatus(),r.getResponseDeadline(),r.getDeliveryLocation(),r.getAcceptedQuotationId(),requestItemRepo.findByRequestId(r.getId()).stream().map(i->new RequestItemView(i.getId(),i.getProduct().getId(),i.getProduct().getName(),i.getQuantity(),i.getSpecifications())).toList()); }
    private QuotationView quotationView(SupplierQuotation q) { return new QuotationView(q.getId(),q.getQuotationNumber(),q.getRequest().getId(),q.getSupplierBusiness().getId(),q.getSupplierBusiness().getTradeName()==null?q.getSupplierBusiness().getLegalName():q.getSupplierBusiness().getTradeName(),q.getSupplierBusiness().getVerificationStatus().name(),q.getStatus(),q.getValidUntil(),q.getDeliveryLeadDays(),q.getPaymentTerms(),q.getDeliveryTerms(),q.getRemarks(),q.getPreviousVersionId(),quotationItemRepo.findByQuotationId(q.getId()).stream().map(i->new QuotationLineView(i.getRequestItem().getId(),i.getProductListing().getId(),i.getRequestItem().getProduct().getName(),i.getOfferedQuantity(),i.getMinimumOrderQuantity(),i.getUnitPrice())).toList()); }
    private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private ResponseStatusException notFound(String message){return new ResponseStatusException(HttpStatus.NOT_FOUND,message);}
    private void conflict(String message){throw new ResponseStatusException(HttpStatus.CONFLICT,message);}
    public record RequestItemInput(Long productId,int quantity,String specifications){}
    public record CreateRequest(Long buyerBusinessId,String title,String description,LocalDateTime responseDeadline,String deliveryLocation,List<RequestItemInput> items){}
    public record QuotationItemInput(Long requestItemId,Long productListingId,int offeredQuantity,int minimumOrderQuantity,BigDecimal unitPrice){}
    public record CreateQuotation(Long supplierBusinessId,LocalDateTime validUntil,int deliveryLeadDays,String paymentTerms,String deliveryTerms,String remarks,List<QuotationItemInput> items){}
    public record RequestItemView(Long id,Long productId,String productName,int quantity,String specifications){}
    public record RequestView(Long id,String rfqNumber,Long buyerBusinessId,String buyerName,String title,String description,String status,LocalDateTime responseDeadline,String deliveryLocation,Long acceptedQuotationId,List<RequestItemView> items){}
    public record QuotationLineView(Long requestItemId,Long productListingId,String productName,int offeredQuantity,int minimumOrderQuantity,BigDecimal unitPrice){}
    public record QuotationView(Long id,String quotationNumber,Long requestId,Long supplierBusinessId,String supplierName,String verificationStatus,String status,LocalDateTime validUntil,int deliveryLeadDays,String paymentTerms,String deliveryTerms,String remarks,Long previousVersionId,List<QuotationLineView> items){}
    public record OrderView(Long orderId,String orderNumber,String status,BigDecimal subtotal,String quotationNumber){}
}
