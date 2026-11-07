package com.robsen.robsen.business;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.robsen.robsen.common.Status;

class MarketplaceVerificationPolicyTest {
    @Test void requiresApprovalForPublishingAndOrdersByDefault() {
        MarketplaceVerificationPolicy policy = new MarketplaceVerificationPolicy(true, true);
        Business business = new Business();
        business.setVerificationStatus(Status.PENDING);
        assertFalse(policy.canPublish(business));
        assertFalse(policy.canAcceptOrders(business));
        business.setVerificationStatus(Status.APPROVED);
        assertTrue(policy.canPublish(business));
        assertTrue(policy.canAcceptOrders(business));
    }

    @Test void allowsConfiguredPolicyToPermitUnverifiedMarketplaceActions() {
        MarketplaceVerificationPolicy policy = new MarketplaceVerificationPolicy(false, false);
        Business business = new Business();
        business.setVerificationStatus(Status.PENDING);
        assertTrue(policy.canPublish(business));
        assertTrue(policy.canAcceptOrders(business));
    }
}
