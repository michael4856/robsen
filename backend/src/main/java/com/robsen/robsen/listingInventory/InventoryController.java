package com.robsen.robsen.listingInventory;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }

    @GetMapping("/api/inventory/{businessId}")
    public Page<ListingInventory> inventory(@PathVariable Long businessId,
            @RequestParam(required = false) String q, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(businessId, q, status, page, size);
    }
    @PostMapping("/api/inventory/listings/{listingId}/stock-in")
    public ListingInventory stockIn(@PathVariable Long listingId, @RequestBody InventoryService.StockRequest request) {
        return service.stockIn(listingId, request.quantity(), request.reason(), request.remarks());
    }
    @PostMapping("/api/inventory/listings/{listingId}/adjust")
    public ListingInventory adjust(@PathVariable Long listingId, @RequestBody InventoryService.AdjustmentRequest request) {
        return service.adjust(listingId, request.targetAvailable(), request.reason(), request.remarks());
    }
    @PutMapping("/api/inventory/listings/{listingId}/reorder-level")
    public ListingInventory reorderLevel(@PathVariable Long listingId, @RequestBody InventoryService.ReorderLevelRequest request) {
        return service.setReorderLevel(listingId, request.reorderLevel());
    }
    @GetMapping("/api/stock-movements/business/{businessId}")
    public Page<InventoryService.MovementView> history(@PathVariable Long businessId,
            @RequestParam(required = false) Long listingId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.history(businessId, listingId, page, size); }
}
