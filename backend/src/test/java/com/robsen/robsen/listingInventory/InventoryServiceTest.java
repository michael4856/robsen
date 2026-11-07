package com.robsen.robsen.listingInventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.robsen.robsen.business.Business;
import com.robsen.robsen.common.MovementType;
import com.robsen.robsen.customerOrder.CustomerOrderRepo;
import com.robsen.robsen.orderItem.OrderItemRepo;
import com.robsen.robsen.product.Product;
import com.robsen.robsen.productListing.ProductListing;
import com.robsen.robsen.productListing.ProductListingRepo;
import com.robsen.robsen.security.BusinessAccessService;
import com.robsen.robsen.stockMovement.StockMovement;
import com.robsen.robsen.stockMovement.StockMovementRepo;

class InventoryServiceTest {
    private final ProductListingRepo listings = mock(ProductListingRepo.class);
    private final StockMovementRepo movements = mock(StockMovementRepo.class);
    private final OrderItemRepo items = mock(OrderItemRepo.class);
    private final CustomerOrderRepo orders = mock(CustomerOrderRepo.class);
    private final BusinessAccessService access = mock(BusinessAccessService.class);
    private final InventoryService service = new InventoryService(listings, movements, items, orders, access);
    private final ProductListing listing = new ProductListing();

    @BeforeEach
    void setUp() {
        Business business = new Business();
        business.setId(12L);
        Product product = new Product();
        product.setName("Widget");
        listing.setId(34L);
        listing.setBusiness(business);
        listing.setProduct(product);
        listing.setAvailableQuantity(3);
        listing.setReorderLevel(4);
        when(listings.findByIdForUpdate(34L)).thenReturn(Optional.of(listing));
        when(listings.save(any(ProductListing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orders.findByBuyerBusinessIdInOrSupplierBusinessIdIn(anyList(), anyList())).thenReturn(List.of());
        when(items.findByOrderId(any(Long.class))).thenReturn(List.of());
        when(access.currentUserId()).thenReturn(9L);
    }

    @Test
    void stockInThenReservationAndReleaseKeepQuantityAndMovementHistoryConsistent() {
        ListingInventory stockIn = service.stockIn(34L, 5, "Purchase receipt", "PO-15");

        assertEquals(8, stockIn.availableQuantity());
        assertEquals(8, stockIn.quantityOnHand());
        assertEquals("IN_STOCK", stockIn.stockStatus());

        service.reservation(34L, 2, false, 70L, "Order placed");
        assertEquals(6, listing.getAvailableQuantity());
        service.reservation(34L, 2, true, 70L, "Order cancelled");
        assertEquals(8, listing.getAvailableQuantity());

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movements, org.mockito.Mockito.times(3)).save(captor.capture());
        assertEquals(List.of(MovementType.STOCK_IN, MovementType.RESERVATION, MovementType.RESERVATION_RELEASE),
                captor.getAllValues().stream().map(StockMovement::getMovementType).toList());
        assertEquals(3, captor.getAllValues().getFirst().getPreviousQuantity());
        assertEquals(8, captor.getAllValues().getFirst().getNewQuantity());
        assertEquals(70L, captor.getAllValues().get(1).getReferenceId());
    }

    @Test
    void invalidStockOperationsDoNotMutateTheListing() {
        assertThrows(ResponseStatusException.class,
                () -> service.stockIn(34L, 0, "Count", null));
        assertThrows(ResponseStatusException.class,
                () -> service.adjust(34L, -1, "Count correction", null));

        assertEquals(3, listing.getAvailableQuantity());
        org.mockito.Mockito.verify(listings, org.mockito.Mockito.never()).save(any(ProductListing.class));
        org.mockito.Mockito.verify(movements, org.mockito.Mockito.never()).save(any(StockMovement.class));
    }
}
