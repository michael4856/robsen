package com.robsen.robsen.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;

class BusinessBranchServiceTest {
    private final BusinessRepo businessRepo = mock(BusinessRepo.class);
    private final BusinessBranchRepo branchRepo = mock(BusinessBranchRepo.class);
    private final BusinessAccessService businessAccess = mock(BusinessAccessService.class);
    private final BusinessBranchService service =
            new BusinessBranchService(businessRepo, branchRepo, businessAccess);

    @Test
    void createsBranchUnderSelectedBusiness() {
        Business business = new Business();
        business.setId(8L);
        when(businessRepo.findById(8L)).thenReturn(Optional.of(business));
        when(branchRepo.save(org.mockito.ArgumentMatchers.any(BusinessBranch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        BusinessBranchController.BranchRequest request = new BusinessBranchController.BranchRequest(
                "East Branch", "42 Market Road", "Addis Ababa", "Addis Ababa", "+251 900000000", null);

        BusinessBranchService.BranchView result = service.create(8L, request);

        assertEquals(8L, result.businessId());
        assertEquals("East Branch", result.name());
        assertEquals("42 Market Road", result.address());
        verify(businessAccess).requireAccess(8L, "businesses:UPDATE");
    }

    @Test
    void cannotUpdateBranchThroughAnotherBusiness() {
        when(branchRepo.findByIdAndBusinessId(5L, 8L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.update(
                8L,
                5L,
                new BusinessBranchController.BranchRequest("Branch", "Address", null, null, null, null)));
        verify(businessAccess).requireAccess(8L, "businesses:UPDATE");
    }
}
