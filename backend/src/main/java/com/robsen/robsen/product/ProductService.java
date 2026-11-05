package com.robsen.robsen.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.productListing.ProductListingRepo;
import com.robsen.robsen.productCategory.ProductCategoryRepo;
import com.robsen.robsen.productCategory.ProductCategory;

@Service
public class ProductService {
    private final ProductRepo productRepo;
    private final BusinessAccessService businessAccess;
    private final ProductListingRepo listingRepo;
    private final ProductCategoryRepo categoryRepo;

    public ProductService(
            ProductRepo productRepo,
            BusinessAccessService businessAccess,
            ProductListingRepo listingRepo,
            ProductCategoryRepo categoryRepo) {
        this.productRepo = productRepo;
        this.businessAccess = businessAccess;
        this.listingRepo = listingRepo;
        this.categoryRepo = categoryRepo;
    }

    @Transactional
    public Product create(Product product) {
        if (product.getBusiness() == null || product.getBusiness().getId() == null) requirePlatformAdmin();
        else businessAccess.requireAccess(product.getBusiness().getId(), "products:CREATE");
        validateCategoryOwnership(product);
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

    @Transactional(readOnly = true)
    public List<Product> getRegisteredForBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "product-listings:READ");
        return listingRepo.findByBusinessId(businessId).stream()
                .filter(listing -> listing.isActive() && listing.getProduct() != null
                        && listing.getProduct().isActive())
                .map(listing -> listing.getProduct())
                .collect(java.util.stream.Collectors.toMap(
                        Product::getId,
                        product -> product,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new))
                .values().stream()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> getCatalogForBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "products:READ");
        return productRepo.findByBusinessIdAndActiveTrue(businessId);
    }

    @Transactional
    public Product update(Long id, Product input) {
        Product product = get(id);
        if (product.getBusiness() == null) requirePlatformAdmin();
        else {
            businessAccess.requireAccess(product.getBusiness().getId(), "products:UPDATE");
            if (input.getBusiness() != null && input.getBusiness().getId() != null
                    && !input.getBusiness().getId().equals(product.getBusiness().getId()))
                throw new org.springframework.security.access.AccessDeniedException("A product cannot be moved to another business.");
        }
        product.setProductCategory(input.getProductCategory());
        validateCategoryOwnership(product);
        product.setName(input.getName());
        product.setDescription(input.getDescription());
        product.setImageUrl(input.getImageUrl());
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

    private void validateCategoryOwnership(Product product) {
        if (product.getProductCategory() == null || product.getProductCategory().getId() == null) return;
        ProductCategory category = categoryRepo.findById(product.getProductCategory().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found."));
        if (category.getBusiness() != null && (product.getBusiness() == null
                || !category.getBusiness().getId().equals(product.getBusiness().getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "A product can only use a category from the same business or the shared catalog.");
        }
    }
}
