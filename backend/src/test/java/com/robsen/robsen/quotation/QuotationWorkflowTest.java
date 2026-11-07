package com.robsen.robsen.quotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.business.MarketplaceVerificationPolicy;
import com.robsen.robsen.common.Status;
import com.robsen.robsen.customerOrder.CustomerOrder;
import com.robsen.robsen.customerOrder.CustomerOrderRepo;
import com.robsen.robsen.customerOrder.OrderStatusHistory;
import com.robsen.robsen.customerOrder.OrderStatusHistoryRepo;
import com.robsen.robsen.listingInventory.InventoryService;
import com.robsen.robsen.orderItem.OrderItem;
import com.robsen.robsen.orderItem.OrderItemRepo;
import com.robsen.robsen.product.Product;
import com.robsen.robsen.product.ProductRepo;
import com.robsen.robsen.productListing.ProductListing;
import com.robsen.robsen.productListing.ProductListingRepo;
import com.robsen.robsen.security.BusinessAccessService;

class QuotationWorkflowTest {
    private final QuotationRequestRepo requestRepo = mock(QuotationRequestRepo.class);
    private final QuotationRequestItemRepo requestItemRepo = mock(QuotationRequestItemRepo.class);
    private final SupplierQuotationRepo quotationRepo = mock(SupplierQuotationRepo.class);
    private final SupplierQuotationItemRepo quotationItemRepo = mock(SupplierQuotationItemRepo.class);
    private final BusinessRepo businessRepo = mock(BusinessRepo.class);
    private final ProductRepo productRepo = mock(ProductRepo.class);
    private final ProductListingRepo listingRepo = mock(ProductListingRepo.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final CustomerOrderRepo orderRepo = mock(CustomerOrderRepo.class);
    private final OrderItemRepo orderItemRepo = mock(OrderItemRepo.class);
    private final OrderStatusHistoryRepo historyRepo = mock(OrderStatusHistoryRepo.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final Business buyer = business(1L, "Buyer", Status.APPROVED);
    private final Business supplier = business(2L, "Supplier", Status.APPROVED);
    private final Product product = new Product();
    private final ProductListing listing = new ProductListing();
    private final QuotationRequest request = new QuotationRequest();
    private final QuotationRequestItem requestedItem = new QuotationRequestItem();
    private final SupplierQuotation quote = new SupplierQuotation();
    private final SupplierQuotationItem quoteItem = new SupplierQuotationItem();
    private final List<SupplierQuotation> quotes = new ArrayList<>();
    private final QuotationService service = new QuotationService(requestRepo, requestItemRepo, quotationRepo,
            quotationItemRepo, businessRepo, productRepo, listingRepo, access, orderRepo, orderItemRepo,
            historyRepo, inventory, new MarketplaceVerificationPolicy(true, true));

    @BeforeEach
    void setUp() {
        product.setId(5L);
        product.setName("Widget");
        product.setActive(true);
        listing.setId(8L);
        listing.setBusiness(supplier);
        listing.setProduct(product);
        listing.setActive(true);
        listing.setAvailableQuantity(12);
        request.setId(50L);
        request.setBuyerBusiness(buyer);
        request.setStatus("DRAFT");
        requestedItem.setId(60L);
        requestedItem.setRequest(request);
        requestedItem.setProduct(product);
        requestedItem.setQuantity(4);
        quote.setId(70L);
        quote.setRequest(request);
        quote.setSupplierBusiness(supplier);
        quote.setQuotationNumber("QT-70");
        quote.setStatus("SUBMITTED");
        quoteItem.setQuotation(quote);
        quoteItem.setRequestItem(requestedItem);
        quoteItem.setProductListing(listing);
        quoteItem.setOfferedQuantity(4);
        quoteItem.setMinimumOrderQuantity(2);
        quoteItem.setUnitPrice(new BigDecimal("25.00"));

        when(access.currentUserId()).thenReturn(17L);
        when(businessRepo.findById(1L)).thenReturn(Optional.of(buyer));
        when(businessRepo.findById(2L)).thenReturn(Optional.of(supplier));
        when(productRepo.findById(5L)).thenReturn(Optional.of(product));
        when(requestRepo.save(any(QuotationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(requestRepo.findById(50L)).thenReturn(Optional.of(request));
        when(requestRepo.findByIdForUpdate(50L)).thenReturn(Optional.of(request));
        when(requestItemRepo.save(any(QuotationRequestItem.class))).thenAnswer(invocation -> {
            QuotationRequestItem item = invocation.getArgument(0);
            item.setId(60L);
            return item;
        });
        when(requestItemRepo.findByRequestId(50L)).thenReturn(List.of(requestedItem));
        when(listingRepo.findById(8L)).thenReturn(Optional.of(listing));
        when(quotationRepo.findByRequestId(50L)).thenAnswer(invocation -> List.copyOf(quotes));
        when(quotationRepo.save(any(SupplierQuotation.class))).thenAnswer(invocation -> {
            SupplierQuotation saved = invocation.getArgument(0);
            saved.setId(70L);
            if (!quotes.contains(saved)) quotes.add(saved);
            return saved;
        });
        when(quotationRepo.findById(70L)).thenReturn(Optional.of(quote));
        when(quotationItemRepo.save(any(SupplierQuotationItem.class))).thenAnswer(invocation -> {
            SupplierQuotationItem item = invocation.getArgument(0);
            item.setId(71L);
            return item;
        });
        when(quotationItemRepo.findByQuotationId(70L)).thenReturn(List.of(quoteItem));
        when(orderRepo.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(90L);
            return saved;
        });
        when(orderItemRepo.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void buyerPublishesRfqSupplierQuotesAndAcceptanceCreatesReservedPricedOrder() {
        var created = service.create(new QuotationService.CreateRequest(1L, "Widget supply", "Urgent",
                null, "Dock 3", List.of(new QuotationService.RequestItemInput(5L, 4, "Blue"))));
        assertEquals("DRAFT", created.status());

        assertEquals("OPEN", service.publish(50L).status());

        var submitted = service.submit(50L, new QuotationService.CreateQuotation(2L, null, 5,
                "Net 30", "FOB", "Ready", List.of(
                        new QuotationService.QuotationItemInput(60L, 8L, 4, 2, new BigDecimal("25.00")))));
        assertEquals("SUBMITTED", submitted.status());
        assertEquals(1, submitted.items().size());

        var accepted = service.accept(50L, 70L);

        assertEquals("PENDING", accepted.status());
        assertEquals(new BigDecimal("100.00"), accepted.subtotal());
        assertEquals("ACCEPTED", quote.getStatus());
        assertEquals("ACCEPTED", request.getStatus());
        assertEquals(70L, request.getAcceptedQuotationId());
        verify(inventory).reservation(8L, 4, false, 90L, "RFQ quotation accepted");
        verify(orderItemRepo).save(any(OrderItem.class));
        verify(historyRepo).save(any(OrderStatusHistory.class));
    }

    private static Business business(Long id, String legalName, Status verificationStatus) {
        Business business = new Business();
        business.setId(id);
        business.setLegalName(legalName);
        business.setActive(true);
        business.setVerificationStatus(verificationStatus);
        return business;
    }
}
