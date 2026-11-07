package com.robsen.robsen.quotation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface QuotationRequestRepo extends JpaRepository<QuotationRequest,Long> {
 List<QuotationRequest> findByBuyerBusinessIdIn(List<Long> ids);
 List<QuotationRequest> findByStatus(String status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from QuotationRequest r where r.id=:id") java.util.Optional<QuotationRequest> findByIdForUpdate(@Param("id") Long id);
}
