package com.robsen.robsen.quotation;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuotationController {
    private final QuotationService service;
    public QuotationController(QuotationService service) { this.service=service; }

    @GetMapping("/api/quotation-requests")
    public List<QuotationService.RequestView> requests() { return service.requests(); }
    @PostMapping("/api/quotation-requests") @ResponseStatus(HttpStatus.CREATED)
    public QuotationService.RequestView create(@RequestBody QuotationService.CreateRequest request) { return service.create(request); }
    @PostMapping("/api/quotation-requests/{id}/publish")
    public QuotationService.RequestView publish(@PathVariable Long id) { return service.publish(id); }
    @GetMapping("/api/quotation-requests/{id}/quotations")
    public List<QuotationService.QuotationView> quotations(@PathVariable Long id) { return service.quotations(id); }
    @PostMapping("/api/quotation-requests/{id}/quotations") @ResponseStatus(HttpStatus.CREATED)
    public QuotationService.QuotationView submit(@PathVariable Long id, @RequestBody QuotationService.CreateQuotation quotation) {
        return service.submit(id,quotation);
    }
    @PostMapping("/api/quotation-requests/{id}/accept")
    public QuotationService.OrderView accept(@PathVariable Long id, @RequestBody AcceptQuotation request) {
        return service.accept(id,request.quotationId());
    }
    @GetMapping("/api/supplier-quotations")
    public List<QuotationService.QuotationView> myQuotations() { return service.myQuotations(); }
    public record AcceptQuotation(Long quotationId) {}
}
