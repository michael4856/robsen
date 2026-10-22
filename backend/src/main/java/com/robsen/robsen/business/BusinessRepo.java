package com.robsen.robsen.business;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository 
public interface BusinessRepo extends JpaRepository<Business, Long>{
    java.util.List<Business> findByActiveTrue();
}
