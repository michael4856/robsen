"use client";

import { useEffect, useState, type SyntheticEvent } from "react";
import Link from "next/link";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";

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
    if (!profile) return;
    let active = true;
    Promise.all([
      apiFetch<Listing[]>("product-listings/marketplace"),
      Promise.all(profile.businessIds.map((id) => apiFetch<Business>(`business/${id}`))),
      apiFetch<Opportunity[]>("distribution-opportunities/marketplace"),
    ])
      .then(([listingRows, businessRows, opportunityRows]) => {
        if (!active) return;
        setListings(listingRows);
        setBusinesses(businessRows);
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

  return (
    <main className="dashboard-page"><div className="dashboard-content">
      <div className="breadcrumb"><Link href="/dashboard">Workspace</Link><span aria-hidden="true">/</span><span>Discover</span></div>
      <div className="list-heading"><div><p className="dashboard-eyebrow">VERIFIED SUPPLIERS</p><h1>Discover wholesale products</h1><p className="dashboard-subtitle">Browse active listings from approved businesses and place an order using the supplier’s current price.</p></div></div>
      {businesses.length ? <label className="business-selector"><span>Buying business</span><select onChange={(event) => setBuyerId(event.target.value)} value={buyerId}>{businesses.map((business) => <option key={business.id} value={business.id}>{business.tradeName || business.legalName}</option>)}</select></label> : <div className="dashboard-empty-state"><h2>Create a business to start buying</h2><p>Your business must be approved before you can place wholesale orders.</p><Link className="text-button" href="/dashboard">Set up your business</Link></div>}
      <div className="discover-toolbar"><label className="field-control"><span>Search products, categories, and suppliers</span><input onChange={(event) => setQuery(event.target.value)} placeholder="Search marketplace" value={query} /></label></div>
      {error ? <div className="dashboard-alert" role="alert">{error}</div> : null}
      {notice ? <div className="dashboard-success" role="status">{notice}</div> : null}
      {loading ? <div className="empty-state">Loading marketplace…</div> : !visible.length ? <div className="empty-state">No approved supplier listings match your search.</div> : <div className="marketplace-grid">{visible.map((listing) => <article className="marketplace-card" key={listing.listingId}><div className={`marketplace-cover${listing.imageUrl ? "" : " marketplace-cover-placeholder"}`} data-initials={listing.productName.slice(0, 2).toUpperCase()}>{listing.imageUrl ? <img alt={listing.productName} onError={handleMarketplaceImageError} src={listing.imageUrl} /> : <span aria-hidden="true">{listing.productName.slice(0, 2).toUpperCase()}</span>}<span className="marketplace-category">{listing.category || "Wholesale"}</span></div><div className="marketplace-card-content"><p className="supplier-line">{listing.supplierName}{listing.supplierType ? ` · ${listing.supplierType}` : ""}</p><h2>{listing.productName}</h2><p className="marketplace-description">{listing.productDescription || "Quality wholesale product available from a verified supplier."}</p><div className="marketplace-meta"><strong className="listing-price">{listing.wholesalePrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}<span> per unit</span></strong><small>Min. {listing.minimumOrderQuantity} · {listing.availableQuantity} available</small></div><div className="marketplace-action"><label className="field-control"><span>Quantity</span><input min={listing.minimumOrderQuantity} max={listing.availableQuantity} onChange={(event) => setQuantities((current) => ({ ...current, [listing.listingId]: event.target.value }))} type="number" value={quantities[listing.listingId] ?? ""} /></label><button className="primary-button" disabled={pendingListing === listing.listingId || !businesses.length} onClick={() => void order(listing)} type="button">{pendingListing === listing.listingId ? "Submitting…" : "Place order"}</button></div></div></article>)}</div>}
      <section className="opportunity-discovery"><div className="list-heading"><div><p className="dashboard-eyebrow">DISTRIBUTION NETWORK</p><h2>Open partner opportunities</h2></div></div>{!opportunities.length ? <div className="empty-state">No open opportunities are available.</div> : <div className="marketplace-grid opportunity-grid">{opportunities.filter((opportunity) => !normalized || `${opportunity.title} ${opportunity.businessName} ${opportunity.territory ?? ""}`.toLowerCase().includes(normalized)).map((opportunity) => <article className="marketplace-card opportunity-card" key={opportunity.id}><div className={`marketplace-cover opportunity-cover${opportunity.imageUrl ? "" : " marketplace-cover-placeholder"}`} data-initials={opportunity.title.slice(0, 2).toUpperCase()}>{opportunity.imageUrl ? <img alt={opportunity.title} onError={handleMarketplaceImageError} src={opportunity.imageUrl} /> : <span aria-hidden="true">{opportunity.title.slice(0, 2).toUpperCase()}</span>}<span className="marketplace-category">{opportunity.territory || "Open opportunity"}</span></div><div className="marketplace-card-content"><p className="supplier-line">{opportunity.businessName}</p><h2>{opportunity.title}</h2><p className="marketplace-description">{opportunity.description || "A new distribution partnership opportunity is open."}</p>{opportunity.requirements ? <p className="opportunity-requirements"><strong>Requirements</strong>{opportunity.requirements}</p> : null}{opportunity.expiresAt ? <small className="opportunity-expiry">Applications close {new Date(opportunity.expiresAt).toLocaleDateString()}</small> : null}<div className="marketplace-action opportunity-action"><label className="field-control"><span>Application message</span><textarea onChange={(event) => setMessages((current) => ({ ...current, [opportunity.id]: event.target.value }))} value={messages[opportunity.id] ?? ""} /></label><button className="primary-button" disabled={applyingTo === opportunity.id || !businesses.length} onClick={() => void apply(opportunity)} type="button">{applyingTo === opportunity.id ? "Submitting…" : "Apply"}</button></div></div></article>)}</div>}</section>
    </div></main>
  );
}
