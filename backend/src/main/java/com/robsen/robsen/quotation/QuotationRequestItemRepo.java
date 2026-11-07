package com.robsen.robsen.quotation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuotationRequestItemRepo extends JpaRepository<QuotationRequestItem,Long> { List<QuotationRequestItem> findByRequestId(Long id); }
