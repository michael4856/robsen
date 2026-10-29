package com.robsen.robsen.productListing;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-listings")
public class ProductListingController {
    private final ProductListingService listingService;

    public ProductListingController(ProductListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductListing create(@RequestBody ProductListing listing) {
        return listingService.create(listing);
    }

    @GetMapping("/{id}")
    public ProductListing get(@PathVariable Long id) {
        return listingService.get(id);
    }

    @GetMapping
    public List<ProductListing> getAll() {
        return listingService.getAll();
    }

    @GetMapping("/active")
    public List<ProductListing> getActive() {
        return listingService.getActive();
    }

    @GetMapping("/marketplace")
    public List<ProductListingService.MarketplaceListing> getMarketplace() {
        return listingService.getMarketplace();
    }

    @GetMapping("/business/{businessId}")
    public List<ProductListing> getByBusiness(@PathVariable Long businessId) {
        return listingService.getByBusiness(businessId);
    }

    @GetMapping("/businesses")
    public List<ProductListingService.BusinessOption> getBusinessesWithReadableListings() {
        return listingService.getBusinessesWithReadableListings();
    }

    @PutMapping("/{id}")
    public ProductListing update(@PathVariable Long id, @RequestBody ProductListing listing) {
        return listingService.update(id, listing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        listingService.delete(id);
    }
}
