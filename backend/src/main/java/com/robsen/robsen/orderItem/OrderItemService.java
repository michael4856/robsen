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

@Service
public class OrderItemService {
    private final OrderItemRepo orderItemRepo;
    private final CustomerOrderRepo orderRepo;
    private final BusinessAccessService businessAccess;

    public OrderItemService(
            OrderItemRepo orderItemRepo, CustomerOrderRepo orderRepo, BusinessAccessService businessAccess) {
        this.orderItemRepo = orderItemRepo;
        this.orderRepo = orderRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public OrderItem create(OrderItem item) {
        validateItem(item);
        validateOrderListing(item, "order-items:CREATE");
        item.setId(null);
        item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return orderItemRepo.save(item);
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
        requireOrderVisible(orderId, "order-items:READ");
        return orderItemRepo.findByOrderId(orderId);
    }

    @Transactional
    public OrderItem update(Long id, OrderItem input) {
        validateItem(input);
        OrderItem item = get(id);
        requireOrderVisible(item, "order-items:UPDATE");
        validateOrderListing(input, "order-items:UPDATE");
        item.setOrder(input.getOrder());
        item.setProductListing(input.getProductListing());
        item.setQuantity(input.getQuantity());
        item.setUnitPrice(input.getUnitPrice());
        item.setLineTotal(input.getUnitPrice().multiply(BigDecimal.valueOf(input.getQuantity())));
        return orderItemRepo.save(item);
    }

    @Transactional
    public void delete(Long id) {
        OrderItem item = get(id);
        requireOrderVisible(item, "order-items:DELETE");
        orderItemRepo.delete(item);
    }

    private void validateOrderListing(OrderItem item, String permission) {
        if (item.getOrder() == null || item.getProductListing() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order and product listing are required.");
        }
        requireOrderVisible(item.getOrder().getId(), permission);
        Long supplierBusinessId = item.getOrder().getSupplierBusiness().getId();
        if (!supplierBusinessId.equals(item.getProductListing().getBusiness().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The product listing must belong to the order's supplier.");
        }
    }

    private void requireOrderVisible(Long orderId, String permission) {
        CustomerOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + orderId));
        if (businessAccess.isSuperAdmin()
                || businessAccess.canAccess(order.getBuyerBusiness().getId(), permission)
                || businessAccess.canAccess(order.getSupplierBusiness().getId(), permission)) {
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "You do not have access to this order.");
    }

    private void requireOrderVisible(OrderItem item, String permission) {
        requireOrderVisible(item.getOrder().getId(), permission);
    }

    private boolean canRead(OrderItem item) {
        return businessAccess.canAccess(item.getOrder().getBuyerBusiness().getId(), "order-items:READ")
                || businessAccess.canAccess(item.getOrder().getSupplierBusiness().getId(), "order-items:READ");
    }

    private void validateItem(OrderItem item) {
        if (item.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }
        if (item.getUnitPrice() == null || item.getUnitPrice().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unit price must be zero or greater");
        }
    }
}
