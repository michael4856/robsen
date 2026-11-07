package com.robsen.robsen.business;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.robsen.robsen.common.Status;

@Component
public class MarketplaceVerificationPolicy {
    private final boolean verifiedToPublish;
    private final boolean verifiedToAcceptOrders;
    public MarketplaceVerificationPolicy(
            @Value("${app.marketplace.require-verified-to-publish:true}") boolean verifiedToPublish,
            @Value("${app.marketplace.require-verified-to-accept-orders:true}") boolean verifiedToAcceptOrders) {
        this.verifiedToPublish=verifiedToPublish; this.verifiedToAcceptOrders=verifiedToAcceptOrders;
    }
    public boolean canPublish(Business business) { return !verifiedToPublish || business.getVerificationStatus()==Status.APPROVED; }
    public boolean canAcceptOrders(Business business) { return !verifiedToAcceptOrders || business.getVerificationStatus()==Status.APPROVED; }
}
