package com.robsen.robsen.productCategory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductCategoryService {
    private final ProductCategoryRepo categoryRepo;

    public ProductCategoryService(ProductCategoryRepo categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    @Transactional
    public ProductCategory create(ProductCategory category) {
        category.setId(null);
        category.setActive(true);
        return categoryRepo.save(category);
    }

    @Transactional(readOnly = true)
    public ProductCategory get(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProductCategory> getAll() {
        return categoryRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<ProductCategory> getActive() {
        return categoryRepo.findByActiveTrue();
    }

    @Transactional
    public ProductCategory update(Long id, ProductCategory input) {
        ProductCategory category = get(id);
        category.setName(input.getName());
        category.setDescription(input.getDescription());
        category.setActive(input.isActive());
        return categoryRepo.save(category);
    }

    @Transactional
    public void delete(Long id) {
        ProductCategory category = get(id);
        category.setActive(false);
        categoryRepo.save(category);
    }
}
