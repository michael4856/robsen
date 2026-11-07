package com.robsen.robsen.quotation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SupplierQuotationRepo extends JpaRepository<SupplierQuotation,Long> {
 List<SupplierQuotation> findByRequestId(Long id);
 List<SupplierQuotation> findBySupplierBusinessIdIn(List<Long> ids);
 boolean existsByRequestIdAndSupplierBusinessIdAndStatus(Long requestId,Long supplierId,String status);
}
