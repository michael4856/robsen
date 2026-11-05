package com.robsen.robsen.business;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.robsen.robsen.businessMembership.BusinessMembershipRepo;
import com.robsen.robsen.common.AgreementStatus;
import com.robsen.robsen.common.ApplicationStatus;
import com.robsen.robsen.customerOrder.CustomerOrder;
import com.robsen.robsen.customerOrder.CustomerOrderRepo;
import com.robsen.robsen.distributionAgreement.DistributionAgreementRepo;
import com.robsen.robsen.distributionOpportunity.DistributionOpportunityRepo;
import com.robsen.robsen.orderItem.OrderItemRepo;
import com.robsen.robsen.partnerApplication.PartnerApplicationRepo;
import com.robsen.robsen.productListing.ProductListingRepo;
import com.robsen.robsen.territory.TerritoryRepo;

@Service
public class BusinessDashboardService {
    private final BusinessMembershipRepo membershipRepo;
    private final ProductListingRepo listingRepo;
    private final CustomerOrderRepo orderRepo;
    private final OrderItemRepo orderItemRepo;
    private final DistributionOpportunityRepo opportunityRepo;
    private final PartnerApplicationRepo applicationRepo;
    private final DistributionAgreementRepo agreementRepo;
    private final TerritoryRepo territoryRepo;

    public BusinessDashboardService(
            BusinessMembershipRepo membershipRepo,
            ProductListingRepo listingRepo,
            CustomerOrderRepo orderRepo,
            OrderItemRepo orderItemRepo,
            DistributionOpportunityRepo opportunityRepo,
            PartnerApplicationRepo applicationRepo,
            DistributionAgreementRepo agreementRepo,
            TerritoryRepo territoryRepo) {
        this.membershipRepo = membershipRepo;
        this.listingRepo = listingRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.opportunityRepo = opportunityRepo;
        this.applicationRepo = applicationRepo;
        this.agreementRepo = agreementRepo;
        this.territoryRepo = territoryRepo;
    }

    @Transactional(readOnly = true)
    public DashboardStats get(Long businessId, Predicate<String> canRead) {
        List<Long> scope = List.of(businessId);
        List<com.robsen.robsen.productListing.ProductListing> activeListings = canRead.test("product-listings:READ")
                ? listingRepo.findByBusinessId(businessId).stream().filter(listing -> listing.isActive()
                        && listing.getProduct() != null && listing.getProduct().isActive()).toList()
                : List.of();
        long availableProducts = activeListings.stream().map(listing -> listing.getProduct().getId())
                .distinct().count();
        long productCategories = canRead.test("product-categories:READ")
                ? activeListings.stream().map(listing -> listing.getProduct().getProductCategory())
                        .filter(java.util.Objects::nonNull).map(category -> category.getId()).distinct().count()
                : 0;
        List<CustomerOrder> orders = canRead.test("orders:READ")
                ? orderRepo.findByBuyerBusinessIdInOrSupplierBusinessIdIn(scope, scope) : List.of();
        List<com.robsen.robsen.partnerApplication.PartnerApplication> applications =
                canRead.test("partner-applications:READ")
                        ? applicationRepo.findByApplicantBusinessIdInOrOpportunityBusinessIdIn(scope, scope)
                        : List.of();
        long incomingOrders = orders.stream()
                .filter(order -> businessId.equals(order.getSupplierBusiness().getId()))
                .filter(order -> !List.of("CANCELLED", "REJECTED", "DELIVERED").contains(order.getStatus()))
                .count();
        long awaitingAction = orders.stream()
                .filter(order -> businessId.equals(order.getSupplierBusiness().getId()))
                .filter(order -> "PENDING".equals(order.getStatus()))
                .count();
        long purchases = orders.stream()
                .filter(order -> businessId.equals(order.getBuyerBusiness().getId()))
                .count();
        long orderItems = canRead.test("order-items:READ")
                ? orderItemRepo.findByOrderBuyerBusinessIdInOrOrderSupplierBusinessIdIn(scope, scope)
                .stream()
                .filter(item -> businessId.equals(item.getOrder().getBuyerBusiness().getId())
                        || businessId.equals(item.getOrder().getSupplierBusiness().getId()))
                .count() : 0;
        return new DashboardStats(
                canRead.test("memberships:READ") ? membershipRepo.findByBusinessId(businessId).stream()
                        .filter(member -> member.getStatus() == com.robsen.robsen.common.MembershipStatus.ACTIVE)
                        .count() : 0,
                activeListings.size(),
                availableProducts,
                productCategories,
                incomingOrders,
                awaitingAction,
                purchases,
                orderItems,
                canRead.test("opportunities:READ") ? opportunityRepo.findByBusinessId(businessId).stream()
                        .filter(opportunity -> "OPEN".equalsIgnoreCase(opportunity.getStatus())).count()
                        : 0,
                applications.stream().filter(application -> application.getStatus() == ApplicationStatus.PENDING)
                        .filter(application -> businessId.equals(application.getApplicantBusiness().getId())
                                || businessId.equals(application.getOpportunity().getBusiness().getId()))
                        .count(),
                canRead.test("agreements:READ") ? agreementRepo.findByPartnerBusinessId(businessId).stream()
                        .filter(agreement -> agreement.getStatus() == AgreementStatus.ACTIVE).count()
                        : 0,
                canRead.test("territories:READ") ? territoryRepo.findByBusinessId(businessId).size() : 0);
    }

    public record DashboardStats(
            long activeMembers,
            long activeListings,
            long availableProducts,
            long productCategories,
            long incomingOrders,
            long ordersAwaitingAction,
            long purchaseHistory,
            long orderItems,
            long openOpportunities,
            long pendingApplications,
            long activeAgreements,
            long territories) {}
}
