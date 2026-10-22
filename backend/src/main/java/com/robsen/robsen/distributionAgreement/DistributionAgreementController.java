package com.robsen.robsen.distributionAgreement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.robsen.robsen.common.AgreementStatus;

@RestController
@RequestMapping("/api/distribution-agreements")
public class DistributionAgreementController {
    private final DistributionAgreementService agreementService;

    public DistributionAgreementController(DistributionAgreementService agreementService) {
        this.agreementService = agreementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DistributionAgreement create(@RequestBody DistributionAgreement agreement) {
        return agreementService.create(agreement);
    }

    @GetMapping("/{id}")
    public DistributionAgreement get(@PathVariable Long id) {
        return agreementService.get(id);
    }

    @GetMapping
    public List<DistributionAgreement> getAll() {
        return agreementService.getAll();
    }

    @GetMapping("/business/{businessId}")
    public List<DistributionAgreement> getByBusiness(@PathVariable Long businessId) {
        return agreementService.getByBusiness(businessId);
    }

    @GetMapping("/status/{status}")
    public List<DistributionAgreement> getByStatus(@PathVariable AgreementStatus status) {
        return agreementService.getByStatus(status);
    }

    @PutMapping("/{id}")
    public DistributionAgreement update(@PathVariable Long id, @RequestBody DistributionAgreement agreement) {
        return agreementService.update(id, agreement);
    }

    @DeleteMapping("/{id}")
    public DistributionAgreement terminate(@PathVariable Long id) {
        return agreementService.terminate(id);
    }
}
