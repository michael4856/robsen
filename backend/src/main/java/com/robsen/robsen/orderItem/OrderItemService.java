package com.robsen.robsen.orderItem;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderItemService {
    private final OrderItemRepo orderItemRepo;

    public OrderItemService(OrderItemRepo orderItemRepo) {
        this.orderItemRepo = orderItemRepo;
    }

    @Transactional
    public OrderItem create(OrderItem item) {
        validateItem(item);
        item.setId(null);
        item.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return orderItemRepo.save(item);
    }

    @Transactional(readOnly = true)
    public OrderItem get(Long id) {
        return orderItemRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getAll() {
        return orderItemRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<OrderItem> getByOrder(Long orderId) {
        return orderItemRepo.findByOrderId(orderId);
    }

    @Transactional
    public OrderItem update(Long id, OrderItem input) {
        validateItem(input);
        OrderItem item = get(id);
        item.setOrder(input.getOrder());
        item.setProductListing(input.getProductListing());
        item.setQuantity(input.getQuantity());
        item.setUnitPrice(input.getUnitPrice());
        item.setLineTotal(input.getUnitPrice().multiply(BigDecimal.valueOf(input.getQuantity())));
        return orderItemRepo.save(item);
    }

    @Transactional
    public void delete(Long id) {
        orderItemRepo.delete(get(id));
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
