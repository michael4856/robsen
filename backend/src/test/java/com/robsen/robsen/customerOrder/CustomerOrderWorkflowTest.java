package com.robsen.robsen.customerOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.business.BusinessRepo;
import com.robsen.robsen.business.MarketplaceVerificationPolicy;
import com.robsen.robsen.common.Status;
import com.robsen.robsen.listingInventory.InventoryService;
import com.robsen.robsen.orderItem.OrderItemRepo;
import com.robsen.robsen.productListing.ProductListingRepo;
import com.robsen.robsen.security.BusinessAccessService;

class CustomerOrderWorkflowTest {
    private final CustomerOrderRepo orderRepo = mock(CustomerOrderRepo.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final BusinessRepo businessRepo = mock(BusinessRepo.class);
    private final OrderItemRepo itemRepo = mock(OrderItemRepo.class);
    private final ProductListingRepo listingRepo = mock(ProductListingRepo.class);
    private final InventoryService inventory = mock(InventoryService.class);
    private final OrderStatusHistoryRepo historyRepo = mock(OrderStatusHistoryRepo.class);
    private final CustomerOrderService service = new CustomerOrderService(orderRepo, access, businessRepo, itemRepo,
            listingRepo, inventory, historyRepo, new MarketplaceVerificationPolicy(true, true));
    private final Business buyer = business(1L, Status.APPROVED);
    private final Business supplier = business(2L, Status.APPROVED);
    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        when(access.canAccess(1L, "orders:READ")).thenReturn(true);
        when(access.canAccess(2L, "orders:READ")).thenReturn(true);
        when(access.canAccess(1L, "orders:UPDATE")).thenReturn(true);
        when(access.canAccess(2L, "orders:UPDATE")).thenReturn(true);
        when(access.currentUserId()).thenReturn(44L);
        when(businessRepo.findById(1L)).thenReturn(Optional.of(buyer));
        when(businessRepo.findById(2L)).thenReturn(Optional.of(supplier));
        when(orderRepo.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(99L);
            order = saved;
            return saved;
        });
        when(orderRepo.findById(99L)).thenAnswer(invocation -> Optional.ofNullable(order));
        when(itemRepo.findByOrderId(99L)).thenReturn(List.of());
    }

    @Test
    void orderMovesThroughValidLifecycleAndRecordsEachTransition() {
        CustomerOrder input = new CustomerOrder();
        input.setBuyerBusiness(business(1L, Status.APPROVED));
        input.setSupplierBusiness(business(2L, Status.APPROVED));
        input.setSubtotal(new java.math.BigDecimal("25.00"));

        CustomerOrder created = service.create(input);
        assertEquals("PENDING", created.getStatus());
        assertEquals(java.math.BigDecimal.ZERO, created.getSubtotal());

        for (String next : List.of("CONFIRMED", "PROCESSING", "READY_FOR_DISPATCH", "SHIPPED", "DELIVERED", "COMPLETED")) {
            CustomerOrder change = new CustomerOrder();
            change.setStatus(next);
            order = service.update(99L, change);
            assertEquals(next, order.getStatus());
        }

        verify(historyRepo, org.mockito.Mockito.times(7)).save(any(OrderStatusHistory.class));
    }

    @Test
    void rejectsInvalidTransitionsAndRejectionsWithoutReasons() {
        CustomerOrder input = new CustomerOrder();
        input.setBuyerBusiness(business(1L, Status.APPROVED));
        input.setSupplierBusiness(business(2L, Status.APPROVED));
        order = service.create(input);

        CustomerOrder invalid = new CustomerOrder();
        invalid.setStatus("DELIVERED");
        var transitionError = assertThrows(ResponseStatusException.class, () -> service.update(99L, invalid));
        assertEquals(409, transitionError.getStatusCode().value());
        assertEquals("PENDING", order.getStatus());

        CustomerOrder rejected = new CustomerOrder();
        rejected.setStatus("REJECTED");
        var reasonError = assertThrows(ResponseStatusException.class, () -> service.update(99L, rejected));
        assertEquals(400, reasonError.getStatusCode().value());
        assertEquals("PENDING", order.getStatus());
    }

    private static Business business(Long id, Status verificationStatus) {
        Business business = new Business();
        business.setId(id);
        business.setActive(true);
        business.setVerificationStatus(verificationStatus);
        return business;
    }
}
