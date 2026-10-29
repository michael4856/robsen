package com.robsen.robsen.productListing;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.product.Product;
import com.robsen.robsen.product.ProductRepo;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.business.Business;
import com.robsen.robsen.common.Status;

@Service
public class ProductListingService {
    private final ProductListingRepo listingRepo;
    private final BusinessAccessService businessAccess;
    private final ProductRepo productRepo;
    private final BusinessRepo businessRepo;

    public ProductListingService(
            ProductListingRepo listingRepo,
            BusinessAccessService businessAccess,
            ProductRepo productRepo,
            BusinessRepo businessRepo) {
        this.listingRepo = listingRepo;
        this.businessAccess = businessAccess;
        this.productRepo = productRepo;
        this.businessRepo = businessRepo;
    }

    @Transactional
    public ProductListing create(ProductListing listing) {
        validateInput(listing);
        listing.setId(null);
        businessAccess.requireAccess(listing.getBusiness().getId(), "product-listings:CREATE");
        listing.setProduct(loadActiveProduct(listing.getProduct().getId()));
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
    public List<MarketplaceListing> getMarketplace() {
        return listingRepo.findByActiveTrue().stream()
                .filter(listing -> listing.getProduct() != null && listing.getProduct().isActive())
                .filter(listing -> businessRepo.findById(listing.getBusiness().getId())
                        .filter(Business::isActive)
                        .filter(business -> business.getVerificationStatus() == Status.APPROVED)
                        .isPresent())
                .map(listing -> {
                    var supplier = listing.getBusiness();
                    var product = listing.getProduct();
                    return new MarketplaceListing(
                            listing.getId(), product.getId(), product.getName(), product.getDescription(), product.getImageUrl(),
                            product.getProductCategory() == null ? null : product.getProductCategory().getName(),
                            supplier.getId(), supplier.getTradeName() == null ? supplier.getLegalName() : supplier.getTradeName(),
                            supplier.getBusinessType(), listing.getWholesalePrice(),
                            listing.getMinimumOrderQuantity(), listing.getAvailableQuantity(), listing.getDescription());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductListing> getByBusiness(Long businessId) {
        businessAccess.requireAccess(businessId, "product-listings:READ");
        return listingRepo.findByBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public List<BusinessOption> getBusinessesWithReadableListings() {
        List<Long> businessIds = businessAccess.isSuperAdmin()
                ? businessRepo.findByActiveTrue().stream().map(Business::getId).toList()
                : businessAccess.accessibleBusinessIds();
        return businessIds.stream()
                .filter(id -> businessAccess.canAccess(id, "product-listings:READ")
                        || businessAccess.canAccess(id, "products:READ")
                        || businessAccess.canAccess(id, "products:CREATE")
                        || businessAccess.canAccess(id, "products:UPDATE")
                        || businessAccess.canAccess(id, "product-categories:READ")
                        || businessAccess.canAccess(id, "product-categories:CREATE")
                        || businessAccess.canAccess(id, "product-categories:UPDATE"))
                .map(businessRepo::findById)
                .flatMap(java.util.Optional::stream)
                .filter(Business::isActive)
                .map(business -> new BusinessOption(
                        business.getId(), business.getLegalName(), business.getTradeName(),
                        businessAccess.canAccess(business.getId(), "products:CREATE"),
                        businessAccess.canAccess(business.getId(), "products:UPDATE"),
                        businessAccess.canAccess(business.getId(), "product-listings:READ"),
                        businessAccess.canAccess(business.getId(), "product-categories:READ"),
                        businessAccess.canAccess(business.getId(), "product-categories:CREATE"),
                        businessAccess.canAccess(business.getId(), "product-categories:UPDATE")))
                .toList();
    }

    @Transactional
    public ProductListing update(Long id, ProductListing input) {
        validateInput(input);
        ProductListing listing = get(id);
        businessAccess.requireAccess(listing.getBusiness().getId(), "product-listings:UPDATE");
        businessAccess.requireAccess(input.getBusiness().getId(), "product-listings:UPDATE");
        listing.setBusiness(input.getBusiness());
        listing.setProduct(loadActiveProduct(input.getProduct().getId()));
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

    private void validateInput(ProductListing listing) {
        if (listing.getBusiness() == null || listing.getBusiness().getId() == null
                || listing.getProduct() == null || listing.getProduct().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business and product are required.");
        }
        if (listing.getWholesalePrice() == null || listing.getWholesalePrice().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid wholesale price is required.");
        }
        if (listing.getAvailableQuantity() < 0 || listing.getMinimumOrderQuantity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Available quantity cannot be negative and minimum order quantity must be positive.");
        }
    }

    private Product loadActiveProduct(Long productId) {
        return productRepo.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active product not found."));
    }

    public record MarketplaceListing(
            Long listingId,
            Long productId,
            String productName,
            String productDescription,
            String imageUrl,
            String category,
            Long supplierBusinessId,
            String supplierName,
            String supplierType,
            java.math.BigDecimal wholesalePrice,
            int minimumOrderQuantity,
            int availableQuantity,
            String listingDescription) {}

    public record BusinessOption(
            Long id, String legalName, String tradeName,
            boolean canCreateProducts, boolean canUpdateProducts,
            boolean canReadProductListings, boolean canReadProductCategories,
            boolean canCreateProductCategories, boolean canUpdateProductCategories) {}
}
