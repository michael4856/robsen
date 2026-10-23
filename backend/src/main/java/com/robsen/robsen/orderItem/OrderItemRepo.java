package com.robsen.robsen.orderItem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    List<OrderItem> findByOrderBuyerBusinessIdInOrOrderSupplierBusinessIdIn(
            java.util.Collection<Long> buyerBusinessIds, java.util.Collection<Long> supplierBusinessIds);
}
