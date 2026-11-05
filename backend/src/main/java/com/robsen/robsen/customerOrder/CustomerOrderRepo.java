package com.robsen.robsen.customerOrder;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepo extends JpaRepository<CustomerOrder, Long> {
    boolean existsByOrderNumber(String orderNumber);
    java.util.List<CustomerOrder> findByBuyerBusinessIdInOrSupplierBusinessIdIn(
            java.util.Collection<Long> buyerBusinessIds, java.util.Collection<Long> supplierBusinessIds);
}
