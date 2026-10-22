package com.robsen.robsen.customerOrder;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerOrderService {
    private final CustomerOrderRepo orderRepo;

    public CustomerOrderService(CustomerOrderRepo orderRepo) {
        this.orderRepo = orderRepo;
    }

    @Transactional
    public CustomerOrder create(CustomerOrder order) {
        order.setId(null);
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
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> getAll() {
        return orderRepo.findAll();
    }

    @Transactional
    public CustomerOrder update(Long id, CustomerOrder input) {
        CustomerOrder order = get(id);
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
        order.setStatus("CANCELLED");
        return orderRepo.save(order);
    }
}
