package com.robsen.robsen.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@RequestBody Product product) {
        return productService.create(product);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return productService.get(id);
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.getAll();
    }

    @GetMapping("/active")
    public List<Product> getActive() {
        return productService.getActive();
    }

    @GetMapping("/business/{businessId}")
    public List<Product> getRegisteredForBusiness(@PathVariable Long businessId) {
        return productService.getRegisteredForBusiness(businessId);
    }

    @GetMapping("/catalog/business/{businessId}")
    public List<Product> getCatalogForBusiness(@PathVariable Long businessId) {
        return productService.getCatalogForBusiness(businessId);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @RequestBody Product product) {
        return productService.update(id, product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}
