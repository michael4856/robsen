package com.robsen.robsen.productListing;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductListingService {
    private final ProductListingRepo listingRepo;

    public ProductListingService(ProductListingRepo listingRepo) {
        this.listingRepo = listingRepo;
    }

    @Transactional
    public ProductListing create(ProductListing listing) {
        listing.setId(null);
        listing.setActive(true);
        return listingRepo.save(listing);
    }

    @Transactional(readOnly = true)
    public ProductListing get(Long id) {
        return listingRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProductListing> getAll() {
        return listingRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<ProductListing> getActive() {
        return listingRepo.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<ProductListing> getByBusiness(Long businessId) {
        return listingRepo.findByBusinessId(businessId);
    }

    @Transactional
    public ProductListing update(Long id, ProductListing input) {
        ProductListing listing = get(id);
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
        listing.setActive(false);
        listingRepo.save(listing);
    }
}
