# Robsen implementation update: phases 1–4

This note records the implementation in this change and the work still required before calling all four roadmap phases production complete.

## Implemented

### Phase 1: Business verification

- Added a one-to-one `BusinessVerification`, private `BusinessVerificationDocument` metadata, and immutable `BusinessVerificationReview` history.
- Added owner upload and submit endpoints, administrator review endpoints, and an authenticated document download endpoint. Files are stored outside public web paths by `LocalVerificationStorage`; set `app.documents.directory` to a persistent private volume in deployment.
- Required business license and tax identification documents. PDF, JPEG, and PNG uploads are limited to 10 MB. Rejection requires a reason; resubmission requires fresh required documents.
- Only the business owner can submit or view business verification files. Only `ROLE_SUPER_ADMIN` can review. Decisions update the existing `Business.verificationStatus`, and direct business profile updates can no longer set that status.
- `MarketplaceVerificationPolicy` controls listing publication and order acceptance through `app.marketplace.require-verified-to-publish` and `app.marketplace.require-verified-to-accept-orders` (both default to `true`).

### Phase 2: Inventory

- Kept `ProductListing.availableQuantity` as the single stock source; did not add a competing editable stock quantity or warehouse.
- Added `reorderLevel`, a stock overview read model, locked stock-in and target-quantity adjustment operations, and immutable stock movements. Listing edits no longer overwrite stock; initial listing stock is recorded as an initial movement.
- Order item creation and quantity changes reserve/release available stock transactionally. Cancellation/rejection releases it; delivery records fulfillment. Listing row locks prevent concurrent reservations from exceeding available stock.
- Available, reserved and on-hand values are derived for the inventory view. `LOW_STOCK` is calculated from the reorder level.

### Phase 3: Orders

- Extended the existing `CustomerOrder`, retaining its API and legacy string status field. Added rejection/processing remarks, quotation linkage and `OrderStatusHistory`.
- Added backend transition validation and status history on creation and each transition. Rejections require a reason. Order totals now use `BigDecimal`; order item unit prices remain snapshots.
- Added buyer/supplier order actions and a history/details view in the dashboard.

### Phase 4: RFQs and quotations

- Added `QuotationRequest`, request items, `SupplierQuotation` and quote items. RFQ requests support drafts and publication; verified suppliers may submit quotations for matching active listings; revisions retain a previous-version reference.
- Buyers can compare supplier responses and accept a quotation. Acceptance locks the RFQ and transactionally creates the existing order and its items using quoted prices, reserves stock, links the quotation, and marks competing submissions rejected. Repeated acceptance is rejected.
- Added default role catalog/navigation permissions for quotation requests and supplier quotations.

## API summary

| Method | Endpoint | Purpose | Authorization |
|---|---|---|---|
| GET | `/api/business-verifications/business/{businessId}` | Owner verification status and documents | Business owner |
| POST multipart | `/api/business-verifications/business/{businessId}/documents?type=...` | Upload private verification document (`file` part) | Business owner |
| POST | `/api/business-verifications/business/{businessId}/submit` | Submit or resubmit | Business owner |
| GET | `/api/business-verifications/admin` | Paginated review queue (`status`, `q`, `page`, `size`) | Platform super-admin |
| GET | `/api/business-verifications/admin/{id}` | Review detail and history | Platform super-admin |
| POST | `/api/business-verifications/admin/{id}/review` | Approve, reject or request resubmission (`ReviewDecision`) | Platform super-admin |
| GET | `/api/business-verifications/documents/{documentId}` | Secure inline document access | Owning business owner or platform super-admin |
| GET | `/api/inventory/{businessId}` | Paged overview (`q`, `status`, `page`, `size`) | `product-listings:READ` |
| POST | `/api/inventory/listings/{listingId}/stock-in` | Add stock (`StockRequest`) | `product-listings:UPDATE` |
| POST | `/api/inventory/listings/{listingId}/adjust` | Set available quantity (`AdjustmentRequest`) | `product-listings:UPDATE` |
| PUT | `/api/inventory/listings/{listingId}/reorder-level` | Set reorder level | `product-listings:UPDATE` |
| GET | `/api/stock-movements/business/{businessId}` | Paged stock history | `product-listings:READ` |
| GET | `/api/orders/{id}/history` | Immutable order status history | Buyer or supplier `orders:READ` |
| GET/POST | `/api/quotation-requests` | List accessible/open RFQs or create a draft | `quotation-requests:READ/CREATE` |
| POST | `/api/quotation-requests/{id}/publish` | Publish buyer RFQ | Buyer `quotation-requests:UPDATE` |
| GET | `/api/quotation-requests/{id}/quotations` | Compare quotation versions | Buyer or supplier quotation read permission |
| POST | `/api/quotation-requests/{id}/quotations` | Submit a supplier quotation | Supplier `supplier-quotations:CREATE` |
| POST | `/api/quotation-requests/{id}/accept` | Convert accepted quote into an order (`AcceptQuotation`) | RFQ owning business `quotation-requests:UPDATE` |
| GET | `/api/supplier-quotations` | Supplier’s quotations | `supplier-quotations:READ` |

All calls use the existing catch-all Next.js `/api/backend/*` proxy, preserving the HttpOnly token boundary.

## Schema and deployment notes

- New tables: `business_verifications`, `business_verification_documents`, `business_verification_reviews`, `stock_movements`, `order_status_history`, `quotation_requests`, `quotation_request_items`, `supplier_quotations`, and `supplier_quotation_items`.
- Changed tables: `product_listing` gains nullable `reorder_level`; `customer_order` gains rejection/remarks/quotation columns and changes `subtotal` from floating-point to `NUMERIC(19,4)`.
- The project still uses Hibernate `ddl-auto=update`; no production migration tool was introduced. Review the generated PostgreSQL DDL, back up the database, and validate `subtotal` conversion before deployment. Existing listing stock remains in place. Configure and back up the private document storage volume.
- Order workflow supports pending, confirmed, rejected, processing, ready for dispatch, shipped, delivered, cancelled and completed. Shipment records, tracking, partial shipment quantities, proof of delivery and delivery disputes are not implemented; the UI does not offer partial delivery.

## Remaining work and verification gaps

- The roadmap is not fully complete: there are no shipment or delivery-confirmation entities/APIs; RFQ invitations/closing, quotation drafts/editing, comparison sorting, and multiple-item RFQ composition in the UI remain outstanding. RFQ backend supports multiple items.
- Inventory uses `ProductListing.availableQuantity` as the available amount and derives reservations from open orders. A persisted reservation ledger, low-stock notifications, warehouse support and dedicated inventory dashboard summary cards are not included.
- Service-level workflow tests now cover owner document upload/submission and administrator approval, inventory stock-in/reservation/release, valid and invalid order transitions, and RFQ publication through quotation acceptance and order creation. Repository, access, and storage dependencies are mocked in these tests; persistence mappings, HTTP authorization, and concurrent transaction behavior still need dedicated integration coverage.
- API controllers still follow some existing direct-entity patterns for legacy order/listing endpoints. New verification and quotation APIs return DTO records.
- `cd backend && mvn test` passes with the configured local PostgreSQL service available (47 tests, including the new workflow coverage). Frontend TypeScript check and ESLint pass; ESLint reports six existing warnings (image optimization and an unused import). `npm run build` and a Webpack fallback were attempted, but Next.js could not spawn/bind its CSS processing process in this execution environment; the Webpack fallback also failed while parsing TypeScript `--showConfig` output.
