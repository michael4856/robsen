package com.robsen.robsen.customerOrder;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepo extends JpaRepository<CustomerOrder, Long> {
    boolean existsByOrderNumber(String orderNumber);
}
