package com.robsen.robsen.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class ProductService {
    private final ProductRepo productRepo;
    private final BusinessAccessService businessAccess;

    public ProductService(ProductRepo productRepo, BusinessAccessService businessAccess) {
        this.productRepo = productRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public Product create(Product product) {
        requirePlatformAdmin();
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
        requirePlatformAdmin();
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
        requirePlatformAdmin();
        Product product = get(id);
        product.setActive(false);
        productRepo.save(product);
    }

    private void requirePlatformAdmin() {
        if (!businessAccess.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "The shared product catalog can only be changed by a platform administrator.");
        }
    }
}
