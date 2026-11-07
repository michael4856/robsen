package com.robsen.robsen.quotation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SupplierQuotationItemRepo extends JpaRepository<SupplierQuotationItem,Long> { List<SupplierQuotationItem> findByQuotationId(Long id); }
