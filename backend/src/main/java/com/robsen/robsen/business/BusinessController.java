package com.robsen.robsen.business;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController 
@RequestMapping("/api/business")
public class BusinessController {
    private final BusinessService businessService;

    public BusinessController(BusinessService businessService){
        this.businessService = businessService;
    }

    @PostMapping({"", "/create"})
    @ResponseStatus(HttpStatus.CREATED)
    public Business createBusiness(@RequestBody Business business){
        return businessService.createBusiness(business);
    }

    @GetMapping ("/{id}")
    public Business getBusinessById(@PathVariable Long id){
        return businessService.getBusinessById(id);
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
}
