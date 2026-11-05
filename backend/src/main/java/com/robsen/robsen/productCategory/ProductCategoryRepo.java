package com.robsen.robsen.productCategory;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepo extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findByActiveTrue();
    List<ProductCategory> findByBusinessIdAndActiveTrue(Long businessId);
    List<ProductCategory> findByBusinessIsNullAndActiveTrue();
}
