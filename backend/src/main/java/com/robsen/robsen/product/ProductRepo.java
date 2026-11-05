package com.robsen.robsen.product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    List<Product> findByProductCategoryId(Long categoryId);
    List<Product> findByBusinessIdAndActiveTrue(Long businessId);
}
