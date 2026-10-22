package com.robsen.robsen.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepo productRepo;

    public ProductService(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    @Transactional
    public Product create(Product product) {
        product.setId(null);
        product.setActive(true);
        return productRepo.save(product);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> getActive() {
        return productRepo.findByActiveTrue();
    }

    @Transactional
    public Product update(Long id, Product input) {
        Product product = get(id);
        product.setProductCategory(input.getProductCategory());
        product.setName(input.getName());
        product.setDescription(input.getDescription());
        product.setPrice(input.getPrice());
        product.setActive(input.isActive());
        return productRepo.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = get(id);
        product.setActive(false);
        productRepo.save(product);
    }
}
