package com.robsen.robsen.listingInventory;

import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.common.MovementType;
import com.robsen.robsen.common.ReferenceType;
import com.robsen.robsen.customerOrder.CustomerOrder;
import com.robsen.robsen.customerOrder.CustomerOrderRepo;
import com.robsen.robsen.orderItem.OrderItemRepo;
import com.robsen.robsen.productListing.ProductListing;
import com.robsen.robsen.productListing.ProductListingRepo;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.stockMovement.StockMovement;
import com.robsen.robsen.stockMovement.StockMovementRepo;

@Service
public class InventoryService {
    private final ProductListingRepo listings;
    private final StockMovementRepo movements;
    private final OrderItemRepo items;
    private final CustomerOrderRepo orders;
    private final BusinessAccessService access;

    public InventoryService(ProductListingRepo listings, StockMovementRepo movements, OrderItemRepo items,
            CustomerOrderRepo orders, BusinessAccessService access) {
        this.listings = listings; this.movements = movements; this.items = items; this.orders = orders; this.access = access;
    }

    @Transactional(readOnly = true)
    public Page<ListingInventory> list(Long businessId, String q, String stockStatus, int page, int size) {
        access.requireAccess(businessId, "product-listings:READ");
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<ListingInventory> all = listings.findByBusinessId(businessId).stream()
                .map(this::inventory)
                .filter(i -> query.isEmpty() || i.productName().toLowerCase(Locale.ROOT).contains(query)
                        || (i.sku() != null && i.sku().toLowerCase(Locale.ROOT).contains(query)))
                .filter(i -> stockStatus == null || stockStatus.isBlank() || i.stockStatus().equalsIgnoreCase(stockStatus))
                .toList();
        int safeSize = Math.min(Math.max(1, size), 100), start = Math.min(Math.max(0, page) * safeSize, all.size());
        return new PageImpl<>(all.subList(start, Math.min(start + safeSize, all.size())), PageRequest.of(Math.max(0, page), safeSize), all.size());
    }

    @Transactional
    public ListingInventory stockIn(Long listingId, int quantity, String reason, String remarks) {
        ProductListing listing = lockedListing(listingId);
        access.requireAccess(listing.getBusiness().getId(), "product-listings:UPDATE");
        if (quantity <= 0) throw bad("Quantity must be greater than zero.");
        if (reason == null || reason.isBlank()) throw bad("A stock-in reason is required.");
        int previous = listing.getAvailableQuantity();
        if (quantity > Integer.MAX_VALUE - previous) throw bad("The resulting stock quantity is too large.");
        listing.setAvailableQuantity(previous + quantity);
        listings.save(listing);
        record(listing, MovementType.STOCK_IN, quantity, previous, listing.getAvailableQuantity(), reason, remarks, null, null);
        return inventory(listing);
    }

    @Transactional
    public ListingInventory adjust(Long listingId, int targetAvailable, String reason, String remarks) {
        ProductListing listing = lockedListing(listingId);
        access.requireAccess(listing.getBusiness().getId(), "product-listings:UPDATE");
        if (targetAvailable < 0) throw bad("Available stock cannot be negative.");
        if (reason == null || reason.isBlank()) throw bad("An adjustment reason is required.");
        int previous = listing.getAvailableQuantity();
        if (previous == targetAvailable) throw bad("The target stock must differ from the current quantity.");
        listing.setAvailableQuantity(targetAvailable);
        listings.save(listing);
        record(listing, MovementType.ADJUSTMENT, Math.abs(targetAvailable - previous), previous, targetAvailable, reason, remarks, null, null);
        return inventory(listing);
    }

    @Transactional
    public ListingInventory setReorderLevel(Long listingId, int reorderLevel) {
        ProductListing listing = lockedListing(listingId);
        access.requireAccess(listing.getBusiness().getId(), "product-listings:UPDATE");
        if (reorderLevel < 0) throw bad("Reorder level cannot be negative.");
        listing.setReorderLevel(reorderLevel);
        listings.save(listing);
        return inventory(listing);
    }

    @Transactional(readOnly = true)
    public Page<MovementView> history(Long businessId, Long listingId, int page, int size) {
        access.requireAccess(businessId, "product-listings:READ");
        int safeSize = Math.min(Math.max(1, size), 100);
        Page<StockMovement> result = listingId == null
                ? movements.findByProductListingBusinessId(businessId, PageRequest.of(Math.max(0, page), safeSize))
                : movements.findByProductListingId(listingId, PageRequest.of(Math.max(0, page), safeSize));
        if (listingId != null && result.hasContent()
                && !businessId.equals(result.getContent().getFirst().getProductListing().getBusiness().getId()))
            throw new org.springframework.security.access.AccessDeniedException("Listing is outside this business.");
        return result.map(m -> new MovementView(m.getId(), m.getProductListing().getId(), m.getProductListing().getProduct().getName(),
                m.getMovementType(), m.getQuantity(), m.getPreviousQuantity(), m.getNewQuantity(), m.getReason(),
                m.getRemarks(), m.getPerformedByUserId(), m.getCreatedAt()));
    }

    @Transactional
    public void reservation(Long listingId, int quantity, boolean release, Long orderId, String reason) {
        ProductListing listing = lockedListing(listingId);
        int previous = listing.getAvailableQuantity();
        if (release) listing.setAvailableQuantity(Math.addExact(previous, quantity));
        else {
            if (quantity > previous) throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough available stock.");
            listing.setAvailableQuantity(previous - quantity);
        }
        listings.save(listing);
        record(listing, release ? MovementType.RESERVATION_RELEASE : MovementType.RESERVATION,
                quantity, previous, listing.getAvailableQuantity(), reason, null, ReferenceType.ORDER, orderId);
    }

    @Transactional
    public void fulfillment(Long listingId, int quantity, Long orderId) {
        ProductListing listing = lockedListing(listingId);
        int current = listing.getAvailableQuantity();
        record(listing, MovementType.ORDER_FULFILLMENT, quantity, current, current,
                "Order fulfilled", null, ReferenceType.ORDER, orderId);
    }

    private ListingInventory inventory(ProductListing listing) {
        List<CustomerOrder> businessOrders = orders.findByBuyerBusinessIdInOrSupplierBusinessIdIn(
                List.of(listing.getBusiness().getId()), List.of(listing.getBusiness().getId()));
        int reserved = businessOrders.stream().filter(o -> listing.getBusiness().getId().equals(o.getSupplierBusiness().getId()))
                .filter(o -> List.of("PENDING", "CONFIRMED", "PROCESSING", "READY_FOR_DISPATCH", "SHIPPED", "PARTIALLY_DELIVERED").contains(o.getStatus()))
                .flatMap(o -> items.findByOrderId(o.getId()).stream())
                .filter(item -> listing.getId().equals(item.getProductListing().getId()))
                .mapToInt(com.robsen.robsen.orderItem.OrderItem::getQuantity).sum();
        int available = listing.getAvailableQuantity();
        int onHand = Math.addExact(available, reserved);
        int reorder = listing.getReorderLevel() == null ? 0 : listing.getReorderLevel();
        String state = available <= 0 ? "OUT_OF_STOCK" : available <= reorder ? "LOW_STOCK" : "IN_STOCK";
        return new ListingInventory(listing.getId(), listing.getBusiness().getId(), listing.getId(),
                listing.getProduct().getName(), null, onHand, reserved, available, reorder, state);
    }

    private ProductListing lockedListing(Long id) { return listings.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product listing not found.")); }
    private void record(ProductListing listing, MovementType type, int quantity, int previous, int next,
            String reason, String remarks, ReferenceType referenceType, Long referenceId) {
        StockMovement movement = new StockMovement(); movement.setProductListing(listing); movement.setMovementType(type);
        movement.setQuantity(quantity); movement.setPreviousQuantity(previous); movement.setNewQuantity(next);
        movement.setReason(reason.trim()); movement.setRemarks(remarks); movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId); movement.setPerformedByUserId(access.currentUserId()); movements.save(movement);
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    public record StockRequest(int quantity, String reason, String remarks) {}
    public record AdjustmentRequest(int targetAvailable, String reason, String remarks) {}
    public record ReorderLevelRequest(int reorderLevel) {}
    public record MovementView(Long id, Long productListingId, String productName, MovementType type,
            int quantity, int previousQuantity, int newQuantity, String reason, String remarks,
            Long performedByUserId, java.time.LocalDateTime createdAt) {}
}
