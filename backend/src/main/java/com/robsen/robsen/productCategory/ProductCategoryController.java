package com.robsen.robsen.productCategory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-categories")
public class ProductCategoryController {
    private final ProductCategoryService categoryService;

    public ProductCategoryController(ProductCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCategory create(@RequestBody ProductCategory category) {
        return categoryService.create(category);
    }

    @GetMapping("/{id}")
    public ProductCategory get(@PathVariable Long id) {
        return categoryService.get(id);
    }

    @GetMapping
    public List<ProductCategory> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/active")
    public List<ProductCategory> getActive() {
        return categoryService.getActive();
    }

    @GetMapping("/business/{businessId}")
    public List<ProductCategory> getForBusiness(@PathVariable Long businessId) {
        return categoryService.getForBusiness(businessId);
    }

    @PutMapping("/{id}")
    public ProductCategory update(@PathVariable Long id, @RequestBody ProductCategory category) {
        return categoryService.update(id, category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
