package com.robsen.robsen.business;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business/{businessId}/branches")
public class BusinessBranchController {
    private final BusinessBranchService branchService;

    public BusinessBranchController(BusinessBranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    public List<BusinessBranchService.BranchView> list(@PathVariable Long businessId) {
        return branchService.list(businessId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessBranchService.BranchView create(@PathVariable Long businessId, @Valid @RequestBody BranchRequest request) {
        return branchService.create(businessId, request);
    }

    @PutMapping("/{branchId}")
    public BusinessBranchService.BranchView update(
            @PathVariable Long businessId,
            @PathVariable Long branchId,
            @Valid @RequestBody BranchRequest request) {
        return branchService.update(businessId, branchId, request);
    }

    @DeleteMapping("/{branchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long businessId, @PathVariable Long branchId) {
        branchService.deactivate(businessId, branchId);
    }

    public record BranchRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 500) String address,
            @Size(max = 120) String city,
            @Size(max = 120) String region,
            @Size(max = 50) String phoneNumber,
            @Email @Size(max = 255) String email) {}
}
