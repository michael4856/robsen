package com.robsen.robsen.orderItem;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.customerOrder.CustomerOrder;
import com.robsen.robsen.customerOrder.CustomerOrderRepo;
import com.robsen.robsen.productListing.ProductListing;
import com.robsen.robsen.productListing.ProductListingRepo;

@Service
public class OrderItemService {
    private final OrderItemRepo orderItemRepo;
    private final CustomerOrderRepo orderRepo;
    private final BusinessAccessService businessAccess;
    private final ProductListingRepo listingRepo;

    public OrderItemService(
            OrderItemRepo orderItemRepo,
            CustomerOrderRepo orderRepo,
            BusinessAccessService businessAccess,
            ProductListingRepo listingRepo) {
        this.orderItemRepo = orderItemRepo;
        this.orderRepo = orderRepo;
        this.businessAccess = businessAccess;
        this.listingRepo = listingRepo;
    }

    @Transactional
    public OrderItem create(OrderItem item) {
        if (item.getOrder() == null || item.getOrder().getId() == null
                || item.getProductListing() == null || item.getProductListing().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order and product listing are required.");
        }
        CustomerOrder order = findOrder(item.getOrder().getId());
        requireBuyer(order, "orders:CREATE");
        requireOrderPending(order);
        ProductListing listing = findListingForUpdate(item.getProductListing().getId());
        validateAvailableListing(order, listing, item.getQuantity());
        listing.setAvailableQuantity(listing.getAvailableQuantity() - item.getQuantity());
        listingRepo.save(listing);

        OrderItem saved = new OrderItem();
        saved.setOrder(order);
        saved.setProductListing(listing);
        saved.setQuantity(item.getQuantity());
        saved.setUnitPrice(listing.getWholesalePrice());
        saved.setLineTotal(listing.getWholesalePrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        OrderItem result = orderItemRepo.save(saved);
        recalculateSubtotal(order);
        return result;
    }

    @Transactional(readOnly = true)
    public OrderItem get(Long id) {
        OrderItem item = orderItemRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found: " + id));
        requireOrderVisible(item, "order-items:READ");
        return item;
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getAll() {
        if (businessAccess.isSuperAdmin()) return orderItemRepo.findAll();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return orderItemRepo.findByOrderBuyerBusinessIdInOrOrderSupplierBusinessIdIn(businessIds, businessIds).stream()
                .filter(item -> canRead(item))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getByOrder(Long orderId) {
        CustomerOrder order = findOrder(orderId);
        requireOrderVisible(order, "order-items:READ");
        return orderItemRepo.findByOrderId(orderId);
    }

    @Transactional
    public OrderItem update(Long id, OrderItem input) {
        OrderItem item = get(id);
        requireBuyer(item.getOrder(), "orders:UPDATE");
        requireOrderPending(item.getOrder());
        if (input.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero.");
        }
        ProductListing listing = findListingForUpdate(item.getProductListing().getId());
        if (input.getQuantity() < listing.getMinimumOrderQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is below the listing minimum.");
        }
        int additional = input.getQuantity() - item.getQuantity();
        if (additional > listing.getAvailableQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough quantity is available.");
        }
        listing.setAvailableQuantity(listing.getAvailableQuantity() - additional);
        listingRepo.save(listing);
        item.setQuantity(input.getQuantity());
        item.setUnitPrice(listing.getWholesalePrice());
        item.setLineTotal(listing.getWholesalePrice().multiply(BigDecimal.valueOf(input.getQuantity())));
        OrderItem saved = orderItemRepo.save(item);
        recalculateSubtotal(item.getOrder());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        OrderItem item = get(id);
        requireBuyer(item.getOrder(), "orders:UPDATE");
        requireOrderPending(item.getOrder());
        ProductListing listing = findListingForUpdate(item.getProductListing().getId());
        listing.setAvailableQuantity(listing.getAvailableQuantity() + item.getQuantity());
        listingRepo.save(listing);
        orderItemRepo.delete(item);
        recalculateSubtotal(item.getOrder());
    }

    private void validateAvailableListing(CustomerOrder order, ProductListing listing, int quantity) {
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero.");
        }
        if (!listing.isActive()
                || !order.getSupplierBusiness().getId().equals(listing.getBusiness().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Choose an active listing belonging to the order's supplier.");
        }
        if (quantity < listing.getMinimumOrderQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity is below the listing minimum.");
        }
        if (listing.getWholesalePrice() == null || listing.getWholesalePrice().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The listing does not have a valid price.");
        }
        if (quantity > listing.getAvailableQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough quantity is available.");
        }
    }

    private void requireOrderVisible(OrderItem item, String permission) {
        requireOrderVisible(item.getOrder(), permission);
    }

    private void requireOrderVisible(CustomerOrder order, String permission) {
        if (businessAccess.isSuperAdmin()
                || businessAccess.canAccess(order.getBuyerBusiness().getId(), permission)
                || businessAccess.canAccess(order.getSupplierBusiness().getId(), permission)) return;
        throw new org.springframework.security.access.AccessDeniedException("You do not have access to this order.");
    }

    private boolean canRead(OrderItem item) {
        return businessAccess.canAccess(item.getOrder().getBuyerBusiness().getId(), "order-items:READ")
                || businessAccess.canAccess(item.getOrder().getSupplierBusiness().getId(), "order-items:READ");
    }

    private CustomerOrder findOrder(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
    }

    private ProductListing findListingForUpdate(Long id) {
        return listingRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product listing not found."));
    }

    private void requireBuyer(CustomerOrder order, String permission) {
        businessAccess.requireAccess(order.getBuyerBusiness().getId(), permission);
    }

    private void requireOrderPending(CustomerOrder order) {
        if (!"PENDING".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Items can only be changed while an order is pending.");
        }
    }

    private void recalculateSubtotal(CustomerOrder order) {
        BigDecimal subtotal = orderItemRepo.findByOrderId(order.getId()).stream()
                .map(OrderItem::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(subtotal.doubleValue());
        orderRepo.save(order);
    }
}
