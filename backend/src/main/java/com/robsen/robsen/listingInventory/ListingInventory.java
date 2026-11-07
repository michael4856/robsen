package com.robsen.robsen.listingInventory;

/** Read model over ProductListing.availableQuantity; the listing remains the single stock source. */
public record ListingInventory(
        Long id,
        Long businessId,
        Long productListingId,
        String productName,
        String sku,
        int quantityOnHand,
        int quantityReserved,
        int availableQuantity,
        int reorderLevel,
        String stockStatus) {}
