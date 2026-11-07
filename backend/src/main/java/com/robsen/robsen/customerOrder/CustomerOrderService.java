package com.robsen.robsen.customerOrder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.common.Status;
import com.robsen.robsen.orderItem.OrderItemRepo;
import com.robsen.robsen.productListing.ProductListingRepo;
import com.robsen.robsen.listingInventory.InventoryService;
import com.robsen.robsen.business.MarketplaceVerificationPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CustomerOrderService {
    private final CustomerOrderRepo orderRepo;
    private final BusinessAccessService businessAccess;
    private final BusinessRepo businessRepo;
    private final OrderItemRepo orderItemRepo;
    private final ProductListingRepo listingRepo;
    private final InventoryService inventory;
    private final OrderStatusHistoryRepo historyRepo;
    private final MarketplaceVerificationPolicy verificationPolicy;

    public CustomerOrderService(
            CustomerOrderRepo orderRepo,
            BusinessAccessService businessAccess,
            BusinessRepo businessRepo,
            OrderItemRepo orderItemRepo,
            ProductListingRepo listingRepo) {
        this(orderRepo, businessAccess, businessRepo, orderItemRepo, listingRepo, null, null, null);
    }

    @Autowired
    public CustomerOrderService(CustomerOrderRepo orderRepo, BusinessAccessService businessAccess,
            BusinessRepo businessRepo, OrderItemRepo orderItemRepo, ProductListingRepo listingRepo,
            InventoryService inventory, OrderStatusHistoryRepo historyRepo, MarketplaceVerificationPolicy verificationPolicy) {
        this.orderRepo = orderRepo;
        this.businessAccess = businessAccess;
        this.businessRepo = businessRepo;
        this.orderItemRepo = orderItemRepo;
        this.listingRepo = listingRepo;
        this.inventory = inventory;
        this.historyRepo = historyRepo;
        this.verificationPolicy = verificationPolicy;
    }

    @Transactional
    public CustomerOrder create(CustomerOrder order) {
        if (order.getBuyerBusiness() == null || order.getBuyerBusiness().getId() == null
                || order.getSupplierBusiness() == null || order.getSupplierBusiness().getId() == null
                || order.getBuyerBusiness().getId().equals(order.getSupplierBusiness().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose separate buyer and supplier businesses.");
        }
        order.setId(null);
        Long buyerId = order.getBuyerBusiness().getId();
        Long supplierId = order.getSupplierBusiness().getId();
        businessAccess.requireAccess(buyerId, "orders:CREATE");
        Business buyer = businessRepo.findById(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer business not found."));
        Business supplier = businessRepo.findById(supplierId)
                .filter(Business::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active supplier business not found."));
        if (verificationPolicy != null && !verificationPolicy.canAcceptOrders(supplier)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The supplier business is not approved for wholesale orders.");
        }
        order.setBuyerBusiness(buyer);
        order.setSupplierBusiness(supplier);
        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber("RB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        order.setSubtotal(BigDecimal.ZERO);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        CustomerOrder saved = orderRepo.save(order);
        recordStatus(saved, null, "PENDING", "Order placed");
        return saved;
    }

    @Transactional(readOnly = true)
    public CustomerOrder get(Long id) {
        CustomerOrder order = orderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
        requireVisible(order, "orders:READ");
        return order;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> getAll() {
        if (businessAccess.isSuperAdmin()) return orderRepo.findAll();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return orderRepo.findByBuyerBusinessIdInOrSupplierBusinessIdIn(businessIds, businessIds).stream()
                .filter(this::canRead)
                .toList();
    }

    @Transactional
    public CustomerOrder update(Long id, CustomerOrder input) {
        CustomerOrder order = get(id);
        if (input.getStatus() == null || input.getStatus().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An order status is required.");
        }
        String next = input.getStatus().trim().toUpperCase();
        String current = order.getStatus();
        boolean supplierAction = List.of("CONFIRMED", "REJECTED", "PROCESSING", "READY_FOR_DISPATCH", "SHIPPED")
                .contains(next);
        boolean buyerAction = List.of("CANCELLED", "DELIVERED", "COMPLETED").contains(next);
        boolean authorizedActor = supplierAction
                ? businessAccess.canAccess(order.getSupplierBusiness().getId(), "orders:UPDATE")
                : buyerAction && businessAccess.canAccess(order.getBuyerBusiness().getId(), "orders:UPDATE");
        if (!authorizedActor) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the relevant buyer or supplier can perform this order action.");
        }
        boolean allowed = switch (current) {
            case "PENDING" -> List.of("CONFIRMED", "REJECTED", "CANCELLED").contains(next);
            case "CONFIRMED" -> List.of("PROCESSING", "CANCELLED").contains(next);
            case "PROCESSING" -> "READY_FOR_DISPATCH".equals(next);
            case "READY_FOR_DISPATCH" -> "SHIPPED".equals(next);
            case "SHIPPED" -> "DELIVERED".equals(next);
            case "DELIVERED" -> "COMPLETED".equals(next);
            default -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order cannot move from " + current + " to " + next + ".");
        }
        if ("REJECTED".equals(next) && (input.getRejectionReason() == null || input.getRejectionReason().isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rejection reason is required.");
        if (List.of("CANCELLED", "REJECTED").contains(next)) releaseReservedItems(order);
        if ("DELIVERED".equals(next) && inventory != null) orderItemRepo.findByOrderId(order.getId())
                .forEach(item -> inventory.fulfillment(item.getProductListing().getId(), item.getQuantity(), order.getId()));
        String previous = order.getStatus();
        order.setStatus(next);
        if ("REJECTED".equals(next)) order.setRejectionReason(input.getRejectionReason().trim());
        if (input.getProcessingRemarks() != null) order.setProcessingRemarks(input.getProcessingRemarks());
        CustomerOrder saved = orderRepo.save(order);
        recordStatus(saved, previous, next, input.getProcessingRemarks() != null ? input.getProcessingRemarks() : input.getRejectionReason());
        return saved;
    }

    @Transactional
    public CustomerOrder cancel(Long id) {
        CustomerOrder order = get(id);
        CustomerOrder input = new CustomerOrder();
        input.setStatus("CANCELLED");
        return update(id, input);
    }

    private void requireOrderBusinesses(CustomerOrder order, String permission) {
        if (order.getBuyerBusiness() == null || order.getSupplierBusiness() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Buyer and supplier businesses are required.");
        }
        if (!businessAccess.canAccess(order.getBuyerBusiness().getId(), permission)
                && !businessAccess.canAccess(order.getSupplierBusiness().getId(), permission)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "An active membership in the buyer or supplier business is required.");
        }
    }

    private void requireVisible(CustomerOrder order, String permission) {
        if (!businessAccess.canAccess(order.getBuyerBusiness().getId(), permission)
                && !businessAccess.canAccess(order.getSupplierBusiness().getId(), permission)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this order.");
        }
    }

    private boolean canRead(CustomerOrder order) {
        return businessAccess.canAccess(order.getBuyerBusiness().getId(), "orders:READ")
                || businessAccess.canAccess(order.getSupplierBusiness().getId(), "orders:READ");
    }

    private void releaseReservedItems(CustomerOrder order) {
        orderItemRepo.findByOrderId(order.getId()).forEach(item -> {
            if (inventory != null) inventory.reservation(item.getProductListing().getId(), item.getQuantity(), true,
                    order.getId(), "Order cancelled or rejected");
            else listingRepo.findByIdForUpdate(item.getProductListing().getId()).ifPresent(listing -> {
                listing.setAvailableQuantity(listing.getAvailableQuantity() + item.getQuantity()); listingRepo.save(listing);
            });
        });
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryView> history(Long id) {
        CustomerOrder order = get(id);
        if (historyRepo == null) return List.of();
        return historyRepo.findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(h -> new StatusHistoryView(h.getPreviousStatus(), h.getNewStatus(), h.getChangedByUserId(), h.getRemarks(), h.getCreatedAt()))
                .toList();
    }

    private void recordStatus(CustomerOrder order, String previous, String next, String remarks) {
        if (historyRepo == null) return;
        OrderStatusHistory history = new OrderStatusHistory(); history.setOrder(order); history.setPreviousStatus(previous);
        history.setNewStatus(next); history.setChangedByUserId(businessAccess.currentUserId()); history.setRemarks(remarks);
        historyRepo.save(history);
    }

    public record StatusHistoryView(String previousStatus, String newStatus, Long changedByUserId,
            String remarks, LocalDateTime createdAt) {}
}
