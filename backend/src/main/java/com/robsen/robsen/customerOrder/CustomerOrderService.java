package com.robsen.robsen.customerOrder;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class CustomerOrderService {
    private final CustomerOrderRepo orderRepo;
    private final BusinessAccessService businessAccess;

    public CustomerOrderService(CustomerOrderRepo orderRepo, BusinessAccessService businessAccess) {
        this.orderRepo = orderRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public CustomerOrder create(CustomerOrder order) {
        order.setId(null);
        requireOrderBusinesses(order, "orders:CREATE");
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        if (order.getStatus() == null || order.getStatus().isBlank()) {
            order.setStatus("PENDING");
        }
        return orderRepo.save(order);
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
        requireOrderBusinesses(input, "orders:UPDATE");
        if (!order.getBuyerBusiness().getId().equals(input.getBuyerBusiness().getId())) {
            businessAccess.requireAccess(input.getBuyerBusiness().getId(), "orders:UPDATE");
        }
        if (!order.getSupplierBusiness().getId().equals(input.getSupplierBusiness().getId())) {
            businessAccess.requireAccess(input.getSupplierBusiness().getId(), "orders:UPDATE");
        }
        order.setBuyerBusiness(input.getBuyerBusiness());
        order.setSupplierBusiness(input.getSupplierBusiness());
        order.setOrderNumber(input.getOrderNumber());
        order.setStatus(input.getStatus());
        order.setSubtotal(input.getSubtotal());
        return orderRepo.save(order);
    }

    @Transactional
    public CustomerOrder cancel(Long id) {
        CustomerOrder order = get(id);
        requireOrderBusinesses(order, "orders:DELETE");
        order.setStatus("CANCELLED");
        return orderRepo.save(order);
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
}
