package com.robsen.robsen.productCategory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class ProductCategoryService {
    private final ProductCategoryRepo categoryRepo;
    private final BusinessAccessService businessAccess;

    public ProductCategoryService(ProductCategoryRepo categoryRepo, BusinessAccessService businessAccess) {
        this.categoryRepo = categoryRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public ProductCategory create(ProductCategory category) {
        if (category.getBusiness() == null || category.getBusiness().getId() == null) requirePlatformAdmin();
        else businessAccess.requireAccess(category.getBusiness().getId(), "product-categories:CREATE");
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

    @Transactional(readOnly = true)
    public List<ProductCategory> getForBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "product-categories:READ");
        java.util.LinkedHashMap<Long, ProductCategory> categories = new java.util.LinkedHashMap<>();
        categoryRepo.findByBusinessIsNullAndActiveTrue().forEach(category -> categories.put(category.getId(), category));
        categoryRepo.findByBusinessIdAndActiveTrue(businessId).forEach(category -> categories.put(category.getId(), category));
        return List.copyOf(categories.values());
    }

    @Transactional
    public ProductCategory update(Long id, ProductCategory input) {
        ProductCategory category = get(id);
        if (category.getBusiness() == null) requirePlatformAdmin();
        else {
            businessAccess.requireAccess(category.getBusiness().getId(), "product-categories:UPDATE");
            if (input.getBusiness() != null && input.getBusiness().getId() != null
                    && !input.getBusiness().getId().equals(category.getBusiness().getId()))
                throw new org.springframework.security.access.AccessDeniedException("A category cannot be moved to another business.");
        }
        category.setName(input.getName());
        category.setDescription(input.getDescription());
        category.setActive(input.isActive());
        return categoryRepo.save(category);
    }

    @Transactional
    public void delete(Long id) {
        requirePlatformAdmin();
        ProductCategory category = get(id);
        category.setActive(false);
        categoryRepo.save(category);
    }

    private void requirePlatformAdmin() {
        if (!businessAccess.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "The shared product category catalog can only be changed by a platform administrator.");
        }
    }
}
