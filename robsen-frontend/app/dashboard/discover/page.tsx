"use client";

import { useEffect, useState, type SyntheticEvent } from "react";
import Link from "next/link";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";
import { ListingGrid, LoadingSkeleton } from "@/app/components/ui";

type Listing = {
  listingId: number;
  productName: string;
  productDescription?: string | null;
  imageUrl?: string | null;
  category?: string | null;
  supplierBusinessId: number;
  supplierName: string;
  supplierType?: string | null;
  wholesalePrice: number;
  minimumOrderQuantity: number;
  availableQuantity: number;
};
type Business = { id: number; legalName: string; tradeName?: string | null };
type MarketplaceSupplier = {
  id: number;
  name: string;
  businessType?: string | null;
  businessTypes?: string[];
  description?: string | null;
  verificationStatus?: string | null;
};
type SupplierDirectory = { businesses: MarketplaceSupplier[]; total: number };
type Opportunity = { id: number; title: string; description?: string | null; requirements?: string | null; imageUrl?: string | null; territory?: string | null; businessId: number; businessName: string; expiresAt?: string | null };

function handleMarketplaceImageError(event: SyntheticEvent<HTMLImageElement>) {
  const image = event.currentTarget;
  const cover = image.parentElement;
  if (!cover) return;
  image.remove();
  cover.classList.add("marketplace-cover-placeholder");
  const placeholder = document.createElement("span");
  placeholder.setAttribute("aria-hidden", "true");
  placeholder.textContent = cover.dataset.initials ?? "";
  cover.insertBefore(placeholder, cover.firstChild);
}

export default function DiscoverPage() {
  return <ProtectedArea><DiscoverContent /></ProtectedArea>;
}

function DiscoverContent() {
  const profile = useAuthProfile();
  const [listings, setListings] = useState<Listing[]>([]);
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [suppliers, setSuppliers] = useState<MarketplaceSupplier[]>([]);
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [buyerId, setBuyerId] = useState("");
  const [query, setQuery] = useState("");
  const [quantities, setQuantities] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [pendingListing, setPendingListing] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [applyingTo, setApplyingTo] = useState<number | null>(null);
  const [messages, setMessages] = useState<Record<number, string>>({});

  useEffect(() => {
    const initialSearch = new URLSearchParams(window.location.search).get("search");
    if (initialSearch) void Promise.resolve().then(() => setQuery(initialSearch));
  }, []);

  useEffect(() => {
    if (!profile) return;
    let active = true;
    Promise.all([
      apiFetch<Listing[]>("product-listings/marketplace"),
      Promise.all(profile.businessIds.map((id) => apiFetch<Business>(`business/${id}`))),
      apiFetch<SupplierDirectory>("business/marketplace"),
      apiFetch<Opportunity[]>("distribution-opportunities/marketplace"),
    ])
      .then(([listingRows, businessRows, supplierDirectory, opportunityRows]) => {
        if (!active) return;
        setListings(listingRows);
        setBusinesses(businessRows);
        setSuppliers(supplierDirectory.businesses);
        setOpportunities(opportunityRows);
        setBuyerId(String(businessRows[0]?.id ?? ""));
      })
      .catch((requestError: unknown) => setError(requestError instanceof Error ? requestError.message : "Could not load marketplace."))
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [profile]);

  async function order(listing: Listing) {
    const quantity = Number(quantities[listing.listingId]);
    if (!Number.isSafeInteger(quantity) || quantity < listing.minimumOrderQuantity) {
      setError(`Enter at least ${listing.minimumOrderQuantity} units.`);
      return;
    }
    if (quantity > listing.availableQuantity) {
      setError("The requested quantity exceeds available stock.");
      return;
    }
    if (!buyerId) {
      setError("Create or select a business before placing an order.");
      return;
    }
    setPendingListing(listing.listingId);
    setError("");
    setNotice("");
    let orderId: number | undefined;
    try {
      const created = await apiFetch<{ id: number }>("orders", {
        method: "POST",
        body: JSON.stringify({ buyerBusiness: { id: Number(buyerId) }, supplierBusiness: { id: listing.supplierBusinessId } }),
      });
      orderId = created.id;
      await apiFetch("order-items", {
        method: "POST",
        body: JSON.stringify({ order: { id: created.id }, productListing: { id: listing.listingId }, quantity }),
      });
      setNotice("Order submitted. The supplier will review it.");
      const freshListings = await apiFetch<Listing[]>("product-listings/marketplace");
      setListings(freshListings);
    } catch (requestError) {
      if (orderId) {
        await apiFetch(`orders/${orderId}`, { method: "DELETE" }).catch(() => undefined);
      }
      setError(requestError instanceof Error ? requestError.message : "Could not place this order.");
    } finally {
      setPendingListing(null);
    }
  }

  async function apply(opportunity: Opportunity) {
    if (!buyerId) { setError("Create a business before applying to opportunities."); return; }
    setApplyingTo(opportunity.id);
    setError("");
    setNotice("");
    try {
      await apiFetch("partner-applications", {
        method: "POST",
        body: JSON.stringify({
          opportunity: { id: opportunity.id },
          applicantBusiness: { id: Number(buyerId) },
          message: messages[opportunity.id] ?? "",
        }),
      });
      setNotice("Application submitted. You can track it in Partner applications.");
      setOpportunities((current) => current.filter((item) => item.id !== opportunity.id));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not submit application.");
    } finally {
      setApplyingTo(null);
    }
  }

  const normalized = query.trim().toLowerCase();
  const visible = listings.filter((listing) =>
    [listing.productName, listing.category, listing.supplierName, listing.supplierType]
      .some((value) => value?.toLowerCase().includes(normalized)));
  const visibleSuppliers = suppliers.filter((supplier) =>
    [supplier.name, supplier.businessType, supplier.description, ...(supplier.businessTypes ?? [])]
      .some((value) => value?.toLowerCase().includes(normalized)));
  const visibleOpportunities = opportunities.filter((opportunity) =>
    !normalized || `${opportunity.title} ${opportunity.businessName} ${opportunity.territory ?? ""} ${opportunity.description ?? ""}`
      .toLowerCase().includes(normalized));
  const businessTypeLabel = (supplier: MarketplaceSupplier) => {
    const types = supplier.businessTypes?.length
      ? supplier.businessTypes
      : (supplier.businessType ?? "").split(",");
    return [...new Set(types.map((type) => type.trim()).filter(Boolean))]
      .map((type) => type.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase()))
      .join(", ") || "Business";
  };

  return (
    <main className="dashboard-page discover-marketplace">
      <div className="dashboard-content">
        <div className="breadcrumb"><Link href="/dashboard">Workspace</Link><span aria-hidden="true">/</span><span>Discover</span></div>
        <section className="discover-hero" aria-labelledby="discover-title">
          <div className="discover-hero-copy">
            <p className="dashboard-eyebrow">ROBSEN MARKETPLACE</p>
            <h1 id="discover-title">Discover opportunities</h1>
            <p>Your B2B marketplace for wholesale products and business partnerships.</p>
            <label className="discover-search">
              <span aria-hidden="true">⌕</span>
              <span className="visually-hidden">Search products, suppliers, and opportunities</span>
              <input
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search products and suppliers..."
                value={query}
              />
              {query ? <button aria-label="Clear search" onClick={() => setQuery("")} type="button">×</button> : null}
            </label>
            <nav aria-label="Marketplace sections" className="discover-section-nav">
              <a href="#discover-products"><span>Products</span><strong>{loading ? "—" : visible.length}</strong></a>
              <a href="#discover-suppliers"><span>Suppliers</span><strong>{loading ? "—" : visibleSuppliers.length}</strong></a>
              <a href="#discover-partners"><span>Partners</span><strong>{loading ? "—" : visibleOpportunities.length}</strong></a>
            </nav>
          </div>
          {businesses.length ? (
            <label className="business-selector discover-buyer-selector">
              <span>Buying business</span>
              <select onChange={(event) => setBuyerId(event.target.value)} value={buyerId}>
                {businesses.map((business) => (
                  <option key={business.id} value={business.id}>{business.tradeName || business.legalName}</option>
                ))}
              </select>
            </label>
          ) : (
            <div className="discover-buyer-note">
              <strong>Ready to buy?</strong>
              <span>Create a business profile to place wholesale orders.</span>
              <Link href="/dashboard">Set up your business <span aria-hidden="true">→</span></Link>
            </div>
          )}
        </section>
      {error ? <div className="dashboard-alert" role="alert">{error}</div> : null}
      {notice ? <div className="dashboard-success" role="status">{notice}</div> : null}
      <section aria-labelledby="discover-products-title" className="discover-section" id="discover-products">
        <div className="discover-section-heading">
          <div><p className="dashboard-eyebrow">WHOLESALE CATALOG</p><h2 id="discover-products-title">Featured wholesale products</h2></div>
          <span>{loading ? "Loading…" : `${visible.length} ${visible.length === 1 ? "product" : "products"}`}</span>
        </div>
        {loading ? (
          <ListingGrid aria-busy="true" aria-label="Loading marketplace listings" className="marketplace-grid">
            <LoadingSkeleton className="marketplace-loading-card" /><LoadingSkeleton className="marketplace-loading-card" /><LoadingSkeleton className="marketplace-loading-card" />
          </ListingGrid>
        ) : !visible.length ? (
          <div className="discover-empty-card">{query ? "No products match your search." : "No approved supplier listings are available right now."}</div>
        ) : (
          <ListingGrid className="marketplace-grid">
            {visible.map((listing) => (
              <article className="marketplace-card" key={listing.listingId}>
                <div className={`marketplace-cover${listing.imageUrl ? "" : " marketplace-cover-placeholder"}`} data-initials={listing.productName.slice(0, 2).toUpperCase()}>
                  {listing.imageUrl ? <img alt={listing.productName} onError={handleMarketplaceImageError} src={listing.imageUrl} /> : <span aria-hidden="true">{listing.productName.slice(0, 2).toUpperCase()}</span>}
                  <span className="marketplace-category">{listing.category || "Wholesale"}</span>
                </div>
                <div className="marketplace-card-content">
                  <p className="supplier-line">{listing.supplierName}{listing.supplierType ? ` · ${listing.supplierType}` : ""}</p>
                  <h2>{listing.productName}</h2>
                  <p className="marketplace-description">{listing.productDescription || "Quality wholesale product available from a verified supplier."}</p>
                  <div className="marketplace-meta">
                    <strong className="listing-price">{listing.wholesalePrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}<span> ETB / unit</span></strong>
                    <small>Minimum {listing.minimumOrderQuantity} · {listing.availableQuantity.toLocaleString()} available</small>
                  </div>
                  <div className="marketplace-action">
                    <label className="field-control"><span>Quantity</span><input min={listing.minimumOrderQuantity} max={listing.availableQuantity} onChange={(event) => setQuantities((current) => ({ ...current, [listing.listingId]: event.target.value }))} type="number" value={quantities[listing.listingId] ?? ""} /></label>
                    <button className="primary-button" disabled={pendingListing === listing.listingId || !businesses.length} onClick={() => void order(listing)} type="button">{pendingListing === listing.listingId ? "Submitting…" : "Place order"}</button>
                  </div>
                </div>
              </article>
            ))}
          </ListingGrid>
        )}
      </section>

      <section aria-labelledby="discover-suppliers-title" className="discover-section" id="discover-suppliers">
        <div className="discover-section-heading">
          <div><p className="dashboard-eyebrow">BUSINESS NETWORK</p><h2 id="discover-suppliers-title">Suppliers</h2></div>
          <span>{loading ? "Loading…" : `${visibleSuppliers.length} ${visibleSuppliers.length === 1 ? "supplier" : "suppliers"}`}</span>
        </div>
        {loading ? (
          <ListingGrid aria-busy="true" aria-label="Loading suppliers" className="supplier-grid">
            <LoadingSkeleton className="supplier-loading-card" /><LoadingSkeleton className="supplier-loading-card" /><LoadingSkeleton className="supplier-loading-card" />
          </ListingGrid>
        ) : !visibleSuppliers.length ? (
          <div className="discover-empty-card">{query ? "No suppliers match your search." : "No business profiles are available yet."}</div>
        ) : (
          <ListingGrid className="supplier-grid">
            {visibleSuppliers.map((supplier) => (
              <article className="supplier-card" key={supplier.id}>
                <span aria-hidden="true" className="supplier-card-mark">{supplier.name.slice(0, 2).toUpperCase()}</span>
                <div className="supplier-card-content">
                  <p className="supplier-line">{businessTypeLabel(supplier)}</p>
                  <h3>{supplier.name}</h3>
                  <p>{supplier.description || "A Robsen business network member."}</p>
                  <span className={`supplier-verification supplier-verification-${(supplier.verificationStatus ?? "PENDING").toLowerCase()}`}>
                    {(supplier.verificationStatus ?? "PENDING").replaceAll("_", " ")}
                  </span>
                  <Link href={`/dashboard/discover?search=${encodeURIComponent(supplier.name)}`}>Explore products <span aria-hidden="true">→</span></Link>
                </div>
              </article>
            ))}
          </ListingGrid>
        )}
      </section>

      <section aria-labelledby="discover-partners-title" className="discover-section opportunity-discovery" id="discover-partners">
        <div className="discover-section-heading">
          <div><p className="dashboard-eyebrow">DISTRIBUTION NETWORK</p><h2 id="discover-partners-title">Open partner opportunities</h2></div>
          <span>{loading ? "Loading…" : `${visibleOpportunities.length} ${visibleOpportunities.length === 1 ? "opportunity" : "opportunities"}`}</span>
        </div>
        {loading ? (
          <ListingGrid aria-busy="true" aria-label="Loading partner opportunities" className="marketplace-grid opportunity-grid">
            <LoadingSkeleton className="marketplace-loading-card" /><LoadingSkeleton className="marketplace-loading-card" /><LoadingSkeleton className="marketplace-loading-card" />
          </ListingGrid>
        ) : !visibleOpportunities.length ? (
          <div className="discover-empty-card">{query ? "No opportunities match your search." : "No open opportunities are available."}</div>
        ) : (
          <ListingGrid className="marketplace-grid opportunity-grid">
            {visibleOpportunities.map((opportunity) => (
              <article className="marketplace-card opportunity-card" id={`opportunity-${opportunity.id}`} key={opportunity.id}>
                <div className={`marketplace-cover opportunity-cover${opportunity.imageUrl ? "" : " marketplace-cover-placeholder"}`} data-initials={opportunity.title.slice(0, 2).toUpperCase()}>
                  {opportunity.imageUrl ? <img alt={opportunity.title} onError={handleMarketplaceImageError} src={opportunity.imageUrl} /> : <span aria-hidden="true">{opportunity.title.slice(0, 2).toUpperCase()}</span>}
                  <span className="marketplace-category">{opportunity.territory || "Open opportunity"}</span>
                </div>
                <div className="marketplace-card-content">
                  <p className="supplier-line">{opportunity.businessName}</p>
                  <h2>{opportunity.title}</h2>
                  <p className="marketplace-description">{opportunity.description || "A new distribution partnership opportunity is open."}</p>
                  {opportunity.requirements ? <p className="opportunity-requirements"><strong>Requirements</strong>{opportunity.requirements}</p> : null}
                  {opportunity.expiresAt ? <small className="opportunity-expiry">Applications close {new Date(opportunity.expiresAt).toLocaleDateString()}</small> : null}
                  <div className="marketplace-action opportunity-action">
                    <label className="field-control"><span>Application message</span><textarea onChange={(event) => setMessages((current) => ({ ...current, [opportunity.id]: event.target.value }))} value={messages[opportunity.id] ?? ""} /></label>
                    <button className="primary-button" disabled={applyingTo === opportunity.id || !businesses.length} onClick={() => void apply(opportunity)} type="button">{applyingTo === opportunity.id ? "Submitting…" : "Apply"}</button>
                  </div>
                </div>
              </article>
            ))}
          </ListingGrid>
        )}
      </section>
      </div>
    </main>
  );
}
