package com.robsen.robsen.customerOrder;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class CustomerOrderController {
    private final CustomerOrderService orderService;

    public CustomerOrderController(CustomerOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrder create(@RequestBody CustomerOrder order) {
        return orderService.create(order);
    }

    @GetMapping("/{id}")
    public CustomerOrder get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @GetMapping
    public List<CustomerOrder> getAll() {
        return orderService.getAll();
    }

    @PutMapping("/{id}")
    public CustomerOrder update(@PathVariable Long id, @RequestBody CustomerOrder order) {
        return orderService.update(id, order);
    }

    @DeleteMapping("/{id}")
    public CustomerOrder cancel(@PathVariable Long id) {
        return orderService.cancel(id);
    }
}
