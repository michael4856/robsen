package com.robsen.robsen.productListing;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class ProductListingService {
    private final ProductListingRepo listingRepo;
    private final BusinessAccessService businessAccess;

    public ProductListingService(ProductListingRepo listingRepo, BusinessAccessService businessAccess) {
        this.listingRepo = listingRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public ProductListing create(ProductListing listing) {
        listing.setId(null);
        businessAccess.requireAccess(listing.getBusiness().getId(), "product-listings:CREATE");
        listing.setActive(true);
        return listingRepo.save(listing);
    }

    @Transactional(readOnly = true)
    public ProductListing get(Long id) {
        ProductListing listing = listingRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found: " + id));
        businessAccess.requireAccess(listing.getBusiness().getId(), "product-listings:READ");
        return listing;
    }

    @Transactional(readOnly = true)
    public List<ProductListing> getAll() {
        return listingRepo.findAll().stream()
                .filter(listing -> businessAccess.canAccess(
                        listing.getBusiness().getId(), "product-listings:READ"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductListing> getActive() {
        return listingRepo.findByActiveTrue().stream()
                .filter(listing -> businessAccess.canAccess(
                        listing.getBusiness().getId(), "product-listings:READ"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductListing> getByBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "product-listings:READ");
        return listingRepo.findByBusinessId(businessId);
    }

    @Transactional
    public ProductListing update(Long id, ProductListing input) {
        ProductListing listing = get(id);
        businessAccess.requireAccess(listing.getBusiness().getId(), "product-listings:UPDATE");
        businessAccess.requireAccess(input.getBusiness().getId(), "product-listings:UPDATE");
        listing.setBusiness(input.getBusiness());
        listing.setProduct(input.getProduct());
        listing.setAvailableQuantity(input.getAvailableQuantity());
        listing.setMinimumOrderQuantity(input.getMinimumOrderQuantity());
        listing.setWholesalePrice(input.getWholesalePrice());
        listing.setDescription(input.getDescription());
        listing.setActive(input.isActive());
        return listingRepo.save(listing);
    }

    @Transactional
    public void delete(Long id) {
        ProductListing listing = get(id);
        businessAccess.requireAccess(listing.getBusiness().getId(), "product-listings:DELETE");
        listing.setActive(false);
        listingRepo.save(listing);
    }
}
