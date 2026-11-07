package com.robsen.robsen.quotation;

import java.math.BigDecimal;
import com.robsen.robsen.productListing.ProductListing;
import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name="supplier_quotation_items", indexes=@Index(name="idx_quotation_items_quotation", columnList="quotation_id"))
public class SupplierQuotationItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="quotation_id", nullable=false) private SupplierQuotation quotation;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="request_item_id", nullable=false) private QuotationRequestItem requestItem;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="product_listing_id", nullable=false) private ProductListing productListing;
    @Column(nullable=false) private int offeredQuantity;
    @Column(nullable=false) private int minimumOrderQuantity;
    @Column(nullable=false, precision=19, scale=4) private BigDecimal unitPrice;
}
