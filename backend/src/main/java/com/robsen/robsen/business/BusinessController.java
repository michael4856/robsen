package com.robsen.robsen.business;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController 
@RequestMapping("/api/business")
public class BusinessController {
    private final BusinessService businessService;

    public BusinessController(BusinessService businessService){
        this.businessService = businessService;
    }

    @PostMapping({"", "/create"})
    @ResponseStatus(HttpStatus.CREATED)
    public Business createBusiness(@Valid @RequestBody CreateBusinessRequest request){
        Business business = new Business();
        business.setLegalName(request.legalName().trim());
        business.setTradeName(trimToNull(request.tradeName()));
        business.setDescription(trimToNull(request.description()));
        business.setBusinessType(trimToNull(request.businessType()));
        business.setEmail(trimToNull(request.email()));
        business.setPhoneNumber(trimToNull(request.phoneNumber()));
        business.setAddress(trimToNull(request.address()));
        business.setTinNumber(trimToNull(request.tinNumber()));
        return businessService.createBusiness(business);
    }

    @GetMapping("/my")
    public List<BusinessService.MyBusiness> getMyBusinesses() {
        return businessService.getMyBusinesses();
    }

    @GetMapping ("/{id}")
    public Business getBusinessById(@PathVariable Long id){
        return businessService.getBusinessById(id);
     }

    @GetMapping("/{id}/dashboard")
    public BusinessDashboardService.DashboardStats getDashboard(@PathVariable Long id) {
        return businessService.getDashboard(id);
    }
     @GetMapping ("/all")
     public List<Business> getAllBusinesses(){
        return businessService.getAllBusinesses();
     }
     @GetMapping ("/active-businesses")
     public List<Business> getAllActiveBusinesses(){
        return businessService.getAllActiveBusinesses();
     }

    @PutMapping("/{id}")
    public ResponseEntity<Business> updateBusiness(@PathVariable Long id, @RequestBody Business updatedBusiness) {
        return businessService.updateBusiness(id, updatedBusiness);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusiness(@PathVariable Long id) {
        return businessService.deleteBusiness(id);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateBusinessRequest(
            @NotBlank @Size(max = 255) String legalName,
            @Size(max = 255) String tradeName,
            @Size(max = 2000) String description,
            @Size(max = 100) String businessType,
            @Email @Size(max = 255) String email,
            @Size(max = 50) String phoneNumber,
            @Size(max = 500) String address,
            @Size(max = 100) String tinNumber) {}
}
