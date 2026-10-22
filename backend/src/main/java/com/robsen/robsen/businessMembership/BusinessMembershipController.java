package com.robsen.robsen.businessMembership;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business-memberships")
public class BusinessMembershipController {
    private final BusinessMembershipService membershipService;

    public BusinessMembershipController(BusinessMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessMembership create(@RequestBody BusinessMembership membership) {
        return membershipService.create(membership);
    }

    @GetMapping("/{id}")
    public BusinessMembership get(@PathVariable Long id) {
        return membershipService.get(id);
    }

    @GetMapping
    public List<BusinessMembership> getAll() {
        return membershipService.getAll();
    }

    @GetMapping("/user/{userId}")
    public List<BusinessMembership> getByUser(@PathVariable Long userId) {
        return membershipService.getByUser(userId);
    }

    @GetMapping("/business/{businessId}")
    public List<BusinessMembership> getByBusiness(@PathVariable Long businessId) {
        return membershipService.getByBusiness(businessId);
    }

    @PutMapping("/{id}")
    public BusinessMembership update(@PathVariable Long id, @RequestBody BusinessMembership membership) {
        return membershipService.update(id, membership);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        membershipService.delete(id);
    }
}
