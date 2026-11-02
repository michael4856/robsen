"use client";

import Link from "next/link";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { apiFetch } from "@/app/lib/api-client";
import type { UserProfile } from "@/app/lib/auth";
import { ListingGrid, StatusBadge } from "@/app/components/ui";

type Invitation = { id: number; business: { legalName: string; tradeName?: string | null }; role: { name: string } };
type PublicOpportunity = {
  id: number;
  title: string;
  description?: string | null;
  imageUrl?: string | null;
  territory?: string | null;
  businessId: number;
  businessName: string;
  businessType?: string | null;
  expiresAt?: string | null;
};
type PublicListing = {
  listingId: number;
  productId: number;
  supplierBusinessId: number;
  supplierName: string;
  supplierType?: string | null;
  productName: string;
  productDescription?: string | null;
  listingDescription?: string | null;
  imageUrl?: string | null;
  category?: string | null;
  wholesalePrice: number;
  minimumOrderQuantity: number;
  availableQuantity: number;
};
type PublicBusiness = { id: number; name: string; businessType?: string | null; description?: string | null; verificationStatus?: string | null };
type BusinessDirectory = { businesses: PublicBusiness[]; total: number };

const PAGE_SIZE = 4;

export default function NetworkHome({
  profile,
  invitations,
  onCreateBusiness,
  creating,
  createError,
  creationNotice,
  onAcceptInvitation,
  invitationError,
}: {
  profile: UserProfile;
  invitations: Invitation[];
  onCreateBusiness: (event: FormEvent<HTMLFormElement>) => void;
  creating: boolean;
  createError: string;
  creationNotice: string;
  onAcceptInvitation: (id: number) => void;
  invitationError: string;
}) {
  const [opportunities, setOpportunities] = useState<PublicOpportunity[]>([]);
  const [listings, setListings] = useState<PublicListing[]>([]);
  const [publicBusinesses, setPublicBusinesses] = useState<PublicBusiness[]>([]);
  const [businessCount, setBusinessCount] = useState(0);
  const [businessResults, setBusinessResults] = useState<PublicBusiness[]>([]);
  const [businessSearchLoading, setBusinessSearchLoading] = useState(false);
  const [businessSearchError, setBusinessSearchError] = useState("");
  const [loading, setLoading] = useState(true);
  const [feedError, setFeedError] = useState("");
  const [retryKey, setRetryKey] = useState(0);
  const [query, setQuery] = useState("");
  const [territory, setTerritory] = useState("");
  const [category, setCategory] = useState("");
  const [businessType, setBusinessType] = useState("");
  const [sort, setSort] = useState("newest");
  const [page, setPage] = useState(1);
  const [showBusinessForm, setShowBusinessForm] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showAllTerritories, setShowAllTerritories] = useState(false);
  const [favoriteListings, setFavoriteListings] = useState<number[]>([]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setFeedError("");
    Promise.allSettled([
      apiFetch<PublicOpportunity[]>("distribution-opportunities/marketplace"),
      apiFetch<PublicListing[]>("product-listings/marketplace"),
      apiFetch<BusinessDirectory>("business/marketplace"),
    ]).then(([opportunityResult, listingResult, businessResult]) => {
      if (!active) return;
      if (opportunityResult.status === "fulfilled") setOpportunities(opportunityResult.value);
      else setOpportunities([]);
      if (listingResult.status === "fulfilled") setListings(listingResult.value);
      else setListings([]);
      if (businessResult.status === "fulfilled") {
        setBusinessCount(businessResult.value.total);
        setPublicBusinesses(businessResult.value.businesses);
      } else {
        setBusinessCount(0);
        setPublicBusinesses([]);
      }
      const failures = [opportunityResult, listingResult, businessResult].filter((result) => result.status === "rejected");
      if (failures.length === 3) {
        const reason = opportunityResult.status === "rejected"
          ? opportunityResult.reason
          : listingResult.status === "rejected" ? listingResult.reason : null;
        setFeedError(reason instanceof Error ? reason.message : "Could not load the public network.");
      } else if (failures.length) {
        setFeedError("Some network information could not be loaded. You can retry to refresh it.");
      }
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [retryKey]);

  useEffect(() => {
    const search = query.trim();
    if (search.length < 2) {
      setBusinessResults([]);
      setBusinessSearchError("");
      setBusinessSearchLoading(false);
      return;
    }
    let active = true;
    const timeout = window.setTimeout(() => {
      setBusinessSearchLoading(true);
      setBusinessSearchError("");
      apiFetch<BusinessDirectory>(`business/marketplace?q=${encodeURIComponent(search)}`)
        .then((result) => { if (active) setBusinessResults(result.businesses); })
        .catch((error: unknown) => {
          if (active) {
            setBusinessResults([]);
            setBusinessSearchError(error instanceof Error ? error.message : "Could not search businesses.");
          }
        })
        .finally(() => { if (active) setBusinessSearchLoading(false); });
    }, 250);
    return () => { active = false; window.clearTimeout(timeout); };
  }, [query]);

  const territories = useMemo(() => [...new Set(opportunities.map((item) => item.territory?.trim()).filter((item): item is string => Boolean(item)))].sort((a, b) => a.localeCompare(b)), [opportunities]);
  const categories = useMemo(() => [...new Set(listings.map((item) => item.category?.trim()).filter((item): item is string => Boolean(item)))].sort((a, b) => a.localeCompare(b)), [listings]);
  const businessTypes = useMemo(() => [...new Set(opportunities.map((item) => item.businessType?.trim()).filter((item): item is string => Boolean(item)))].sort((a, b) => a.localeCompare(b)), [opportunities]);
  const territoryCounts = useMemo(() => opportunities.reduce<Record<string, number>>((counts, item) => {
    const name = item.territory?.trim();
    if (name) counts[name] = (counts[name] ?? 0) + 1;
    return counts;
  }, {}), [opportunities]);
  const listingCategoriesByBusiness = useMemo(() => listings.reduce<Record<number, string[]>>((map, item) => {
    const normalizedCategory = item.category?.trim();
    if (!normalizedCategory) return map;
    map[item.supplierBusinessId] ??= [];
    if (!map[item.supplierBusinessId].includes(normalizedCategory)) map[item.supplierBusinessId].push(normalizedCategory);
    return map;
  }, {}), [listings]);

  const filteredOpportunities = useMemo(() => opportunities.filter((item) => {
    const text = `${item.title} ${item.businessName} ${item.description ?? ""} ${item.territory ?? ""} ${item.businessType ?? ""}`.toLowerCase();
    if (query.trim() && !text.includes(query.trim().toLowerCase())) return false;
    if (territory && item.territory?.trim() !== territory) return false;
    if (businessType && item.businessType !== businessType) return false;
    if (category && !(listingCategoriesByBusiness[item.businessId] ?? []).includes(category)) return false;
    return true;
  }).sort((a, b) => sort === "closing"
    ? (a.expiresAt ? new Date(a.expiresAt).getTime() : Number.MAX_SAFE_INTEGER) - (b.expiresAt ? new Date(b.expiresAt).getTime() : Number.MAX_SAFE_INTEGER)
    : b.id - a.id), [opportunities, query, territory, businessType, category, listingCategoriesByBusiness, sort]);
  const pageCount = Math.max(1, Math.ceil(filteredOpportunities.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const pageOpportunities = filteredOpportunities.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const businessName = profile.fullName || profile.email;
  const pendingNotifications = invitations.length;
  const matchingListings = query.trim() ? listings.filter((item) => `${item.productName} ${item.supplierName} ${item.category ?? ""}`.toLowerCase().includes(query.trim().toLowerCase())).slice(0, 3) : [];

  function setFilter(setter: (value: string) => void, value: string) {
    setter(value);
    setPage(1);
  }

  function openBusinessSetup() {
    setShowBusinessForm(true);
    window.setTimeout(() => document.getElementById("create-business")?.scrollIntoView({ behavior: "smooth", block: "start" }), 0);
  }

  function focusInvitations() {
    if (!pendingNotifications) {
      setShowNotifications((current) => !current);
      return;
    }
    setShowNotifications(false);
    document.getElementById("network-invitations")?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  return <main className="dashboard-page network-home-page">
    <header className="network-home-topbar">
      <label className="network-global-search"><span aria-hidden="true">⌕</span><span className="visually-hidden">Search the network</span><input onChange={(event) => setFilter(setQuery, event.target.value)} placeholder="Search businesses, products, territories, opportunities…" type="search" value={query} /></label>
      <div className="network-notification-wrap"><button aria-expanded={showNotifications} aria-label={pendingNotifications ? `${pendingNotifications} pending invitations` : "Notifications"} className="network-notifications" onClick={focusInvitations} type="button"><svg aria-hidden="true" viewBox="0 0 24 24" fill="none"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" /></svg>{pendingNotifications ? <i>{pendingNotifications}</i> : null}</button>{showNotifications ? <div className="network-notification-popover" role="status">You’re all caught up.</div> : null}</div>
      <Link className="network-top-profile" href="/dashboard/profile"><span className="network-top-avatar" style={profile.profilePicture ? { backgroundImage: `url(${profile.profilePicture})` } : undefined}>{profile.profilePicture ? null : businessName.slice(0, 1).toUpperCase()}</span><span><strong>{businessName}</strong><small>{profile.superAdmin ? "Platform administrator" : profile.businessIds.length ? "Business member" : "Robsen member"}</small></span><b aria-hidden="true">⌄</b></Link>
    </header>
    <div className="dashboard-content network-home-content">
      <div className="breadcrumb"><span>Workspace</span><span aria-hidden="true">/</span><span>Overview</span></div>
      {creationNotice ? <p className="dashboard-success" role="status">{creationNotice}</p> : null}
      {invitationError ? <p className="dashboard-alert" role="alert">{invitationError}</p> : null}
      {query.trim().length >= 2 ? <section className="network-business-search-results" aria-label="Business search results" aria-live="polite"><div><strong>Businesses</strong>{businessSearchLoading ? <span>Searching…</span> : <span>{businessResults.length} matches</span>}</div>{businessSearchError ? <p role="alert">{businessSearchError}</p> : businessResults.length ? <ul>{businessResults.slice(0, 5).map((business) => <li key={business.id}><span className="network-business-mark" aria-hidden="true">{business.name.slice(0, 2).toUpperCase()}</span><span><strong>{business.name}</strong><small>{business.businessType?.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase()) || "Business"} · {businessStatusLabel(business.verificationStatus)}</small></span><Link href={`/dashboard/discover?search=${encodeURIComponent(business.name)}`}>View listings</Link></li>)}</ul> : !businessSearchLoading ? <p>No active business profiles matched that search.</p> : null}</section> : null}
      {matchingListings.length ? <section className="network-search-results" aria-label="Matching products"><strong>Matching products</strong>{matchingListings.map((item) => <div key={item.listingId}><span><b>{item.productName}</b><small>{item.supplierName}{item.category ? ` · ${item.category}` : ""}</small></span><Link href={`/dashboard/discover?search=${encodeURIComponent(query)}`}>View <span aria-hidden="true">→</span></Link></div>)}</section> : null}
      <div className="network-home-grid">
        <div className="network-home-main">
          <section className="network-welcome-banner">
            <div className="network-welcome-copy"><p className="dashboard-eyebrow">ROBSEN BUSINESS NETWORK</p><h1>Discover opportunities.<br />Grow your network.</h1><p>Explore distribution partnerships and products from businesses across Ethiopia.</p><div><a className="primary-button" href="#opportunity-feed">Explore opportunities <span aria-hidden="true">→</span></a><button className="secondary-button" onClick={openBusinessSetup} type="button">Create business profile</button></div></div>
            <div aria-hidden="true" className="network-welcome-art"><span className="welcome-sun" /><span className="welcome-mountain mountain-back" /><span className="welcome-mountain mountain-front" /><span className="welcome-skyline" /></div>
          </section>

          <section aria-label="Public network summary" className="network-stat-grid">
            <NetworkStat label="Open opportunities" value={opportunities.length} tone="green" icon="◎" loading={loading} />
            <NetworkStat label="Businesses" value={businessCount} tone="blue" icon="▥" loading={loading} />
            <NetworkStat label="Territories" value={territories.length} tone="violet" icon="⌖" loading={loading} />
            <NetworkStat label="Product listings" value={listings.length} tone="orange" icon="◇" loading={loading} />
          </section>

          {showBusinessForm ? <section className="editor-panel network-onboarding-form" id="create-business">
            <div className="editor-heading"><div><p className="dashboard-eyebrow">GET STARTED</p><h2>Create a business profile</h2></div><button aria-label="Close business form" className="icon-button" onClick={() => setShowBusinessForm(false)} type="button">×</button></div>
            <form className="record-form onboarding-form" onSubmit={onCreateBusiness}>
              <label className="field-control"><span>Legal business name *</span><input autoComplete="organization" name="legalName" required /></label>
              <label className="field-control"><span>Trading name</span><input name="tradeName" /></label>
              <label className="field-control"><span>Business type</span><select defaultValue="" name="businessType"><option value="">Choose a business type</option>{["MANUFACTURER", "WHOLESALER", "DISTRIBUTOR", "RETAILER", "IMPORTER", "EXPORTER", "SERVICE_PROVIDER", "COOPERATIVE", "OTHER"].map((type) => <option key={type} value={type}>{type.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase())}</option>)}</select></label>
              <label className="field-control"><span>Business description</span><textarea maxLength={2000} name="description" rows={3} /></label>
              <label className="field-control"><span>Business email</span><input autoComplete="email" name="email" type="email" /></label>
              <label className="field-control"><span>Phone</span><input autoComplete="tel" name="phoneNumber" /></label>
              <label className="field-control"><span>Address</span><input autoComplete="street-address" name="address" /></label>
              {createError ? <p className="form-error" role="alert">{createError}</p> : null}
              <div className="form-actions"><button className="primary-button" disabled={creating} type="submit">{creating ? "Creating…" : "Create business profile"}</button></div>
            </form>
          </section> : null}

          {invitations.length ? <section className="network-invitation-card" id="network-invitations"><div><p className="dashboard-eyebrow">YOUR INVITATIONS</p><h2>Join a business workspace</h2></div>{invitations.map((invitation) => <div className="network-invitation-row" key={invitation.id}><div><strong>{invitation.business.tradeName || invitation.business.legalName}</strong><span>Invited as {invitation.role.name.replaceAll("_", " ")}</span></div><button className="primary-button" onClick={() => onAcceptInvitation(invitation.id)} type="button">Accept invitation</button></div>)}</section> : null}

          <section className="network-opportunities-panel" id="opportunity-feed">
            <div className="network-feed-heading"><div className="network-opportunity-heading"><div className="network-opportunity-title-row"><h2>Distribution opportunities</h2><div className="network-feed-tabs" aria-label="Sort opportunities"><button className={sort === "newest" ? "selected" : ""} onClick={() => setFilter(setSort, "newest")} type="button">Recommended</button><button className={sort === "newest" ? "" : "selected"} onClick={() => setFilter(setSort, "closing")} type="button">Closing soon</button></div></div><p>Find businesses looking for partners across Ethiopia.</p></div><Link className="text-button" href="/dashboard/discover">Browse marketplace →</Link></div>
            <div className="network-feed-controls"><label><span>Territory</span><select onChange={(event) => setFilter(setTerritory, event.target.value)} value={territory}><option value="">All territories</option>{territories.map((item) => <option key={item} value={item}>{item}</option>)}</select></label><label><span>Category</span><select onChange={(event) => setFilter(setCategory, event.target.value)} value={category}><option value="">All categories</option>{categories.map((item) => <option key={item} value={item}>{item}</option>)}</select></label><label><span>Business type</span><select onChange={(event) => setFilter(setBusinessType, event.target.value)} value={businessType}><option value="">All business types</option>{businessTypes.map((item) => <option key={item} value={item}>{item.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase())}</option>)}</select></label></div>
            {feedError ? <div className="network-feed-error" role="alert"><span>{feedError}</span><button onClick={() => setRetryKey((value) => value + 1)} type="button">Retry</button></div> : null}
            {loading ? <div className="network-feed-loading" aria-live="polite">Loading opportunities…</div> : !pageOpportunities.length ? <div className="network-feed-empty"><h3>{opportunities.length ? "No opportunities match these filters" : "No open opportunities yet"}</h3><p>{opportunities.length ? "Try a different search or filter to see more of the network." : "Explore public businesses and product listings while new partnerships are being published."}</p><Link className="text-button" href="/dashboard/discover">Explore product listings →</Link></div> : <ListingGrid className="network-opportunity-grid">{pageOpportunities.map((item) => {
              const categoriesForBusiness = listingCategoriesByBusiness[item.businessId] ?? [];
              return <article className="network-opportunity-card" key={item.id}>
                <div className="network-opportunity-business"><span className="network-business-mark">{item.businessName.slice(0, 2).toUpperCase()}</span><div><strong>{item.businessName}</strong><small>{item.businessType?.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase()) || "Business"}</small></div><span className="network-verified-badge"><i aria-hidden="true">✓</i> Verified</span></div>
                <h3>{item.title}</h3><p className="network-opportunity-description">{item.description || "A verified business is looking for distribution partners."}</p>
                <div className="network-opportunity-tags">{item.territory ? <span>⌖ {item.territory}</span> : null}{categoriesForBusiness.slice(0, 1).map((name) => <span key={name}>◇ {name}</span>)}<span className="network-open-tag">Open</span></div>
                <div className="network-opportunity-footer"><small>{item.expiresAt ? `Applications close ${new Date(item.expiresAt).toLocaleDateString()}` : "Applications open"}</small><Link className="primary-button" href={`/dashboard/discover#opportunity-${item.id}`}>View opportunity <span aria-hidden="true">→</span></Link></div>
              </article>;
            })}</ListingGrid>}
            {!loading && filteredOpportunities.length > PAGE_SIZE ? <div className="network-feed-pagination"><span>Showing {(currentPage - 1) * PAGE_SIZE + 1}–{Math.min(currentPage * PAGE_SIZE, filteredOpportunities.length)} of {filteredOpportunities.length} opportunities</span><div><button aria-label="Previous page" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)} type="button">‹</button><span>{currentPage} / {pageCount}</span><button aria-label="Next page" disabled={currentPage === pageCount} onClick={() => setPage(currentPage + 1)} type="button">›</button></div></div> : null}
          </section>

          <section className="network-products-panel" aria-labelledby="network-products-heading">
            <div className="network-feed-heading"><div><h2 id="network-products-heading">Products from the network</h2><p>Browse active listings from verified businesses.</p></div><Link className="text-button" href="/dashboard/discover">Explore all products →</Link></div>
            {loading ? <p className="network-products-empty">Loading product listings…</p> : listings.length ? <ListingGrid className="network-product-grid">{listings.slice(0, 8).map((item) => <article className="network-product-card" key={item.listingId}><div className="network-product-image"><img alt={item.productName} loading="lazy" onError={(event) => { if (event.currentTarget.src.includes("picsum.photos/seed/robsen-")) event.currentTarget.style.visibility = "hidden"; else event.currentTarget.src = `https://picsum.photos/seed/robsen-${item.productId}/600/450`; }} src={item.imageUrl || `https://picsum.photos/seed/robsen-${item.productId}/600/450`} /><button aria-label={`${favoriteListings.includes(item.listingId) ? "Remove" : "Add"} ${item.productName} ${favoriteListings.includes(item.listingId) ? "from" : "to"} favorites`} aria-pressed={favoriteListings.includes(item.listingId)} className={`network-product-favorite${favoriteListings.includes(item.listingId) ? " is-favorite" : ""}`} onClick={() => setFavoriteListings((current) => current.includes(item.listingId) ? current.filter((id) => id !== item.listingId) : [...current, item.listingId])} type="button">♥</button><span className="network-product-supplier-logo" aria-label={`${item.supplierName} logo`}>{item.supplierName.slice(0, 2).toUpperCase()}</span></div><div className="network-product-details"><span className="network-product-category">{item.category || "Product"}</span><h3>{item.productName}</h3><p>{item.supplierName}{item.supplierType ? ` · ${item.supplierType.toLowerCase().replaceAll("_", " ")}` : ""}</p><strong>ETB {Number(item.wholesalePrice).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}<small> / unit</small></strong><Link href={`/dashboard/discover?search=${encodeURIComponent(item.productName)}`}>View listing →</Link></div></article>)}</ListingGrid> : <div className="network-product-empty-state"><span aria-hidden="true">◇</span><h3>No products to show yet</h3><p>Active product listings from verified suppliers will appear here.</p><Link className="primary-button" href="/dashboard/discover">Explore product listings →</Link></div>}
          </section>

          <section className="network-businesses-panel" aria-labelledby="network-businesses-heading">
            <div className="network-feed-heading"><div><h2 id="network-businesses-heading">Businesses and service providers</h2><p>Discover active business profiles across the Robsen network.</p></div></div>
            <article className="sample-business-card">
              <div className="sample-business-cover"><span className="sample-business-leaf" aria-hidden="true">✦</span><span className="sample-business-demo-tag">Sample profile</span></div>
              <div className="sample-business-content">
                <div className="sample-business-heading"><span className="sample-business-logo" aria-label="GreenHarvest Trading logo">GH<span>✦</span></span><div><h3>GreenHarvest Trading</h3><p>Agricultural products supplier · Addis Ababa, Ethiopia</p></div><span className="sample-business-status">Verified</span></div>
                <p className="sample-business-description">Locally sourced grains, coffee, and natural honey for retailers and wholesale partners across Ethiopia.</p>
                <div className="sample-business-details">
                  <div><strong>Products</strong><div className="sample-business-tags"><span>Teff flour</span><span>Arabica coffee</span><span>Wildflower honey</span></div></div>
                  <div><strong>Territories</strong><div className="sample-business-tags sample-territory-tags"><span>Addis Ababa</span><span>Oromia</span><span>Amhara</span></div></div>
                </div>
                <details className="sample-business-profile"><summary>View business profile</summary><div><p><b>About</b><br />GreenHarvest Trading connects Ethiopian growers with shops, restaurants, and distributors. The sample profile demonstrates how business details, products, and service regions appear.</p><p><b>Available products</b><br />Teff flour · Arabica coffee · Wildflower honey</p><p><b>Service territories</b><br />Addis Ababa · Oromia · Amhara</p><span>Example content for preview only. This is not a live marketplace listing.</span></div></details>
              </div>
            </article>
            {loading ? <p className="network-products-empty">Loading businesses…</p> : publicBusinesses.length ? <ListingGrid className="network-directory-grid">{publicBusinesses.slice(0, 8).map((business) => { const status = business.verificationStatus?.toUpperCase(); const statusClass = status === "APPROVED" || status === "COMPLETED" ? "is-complete" : status === "REJECTED" || status === "CANCELLED" ? "is-rejected" : "is-pending"; return <article className="network-directory-card" key={business.id}><div className="network-directory-identity"><span className="network-business-mark" aria-hidden="true">{business.name.slice(0, 2).toUpperCase()}</span><div><strong>{business.name}{status === "APPROVED" ? <span className="network-business-check" aria-label="Verified">✓</span> : null}</strong><small>{business.businessType?.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase()) || "Business"}</small></div></div><StatusBadge tone={statusClass === "is-complete" ? "success" : statusClass === "is-rejected" ? "danger" : "warning"} className={`network-business-status ${statusClass}`}>{businessStatusLabel(status)}</StatusBadge><p>{business.description || "Business profile in the Robsen network."}</p><Link aria-label={`View profile and listings from ${business.name}`} href={`/dashboard/discover?search=${encodeURIComponent(business.name)}`}>View profile <span aria-hidden="true">→</span></Link></article>; })}</ListingGrid> : !loading ? <p className="network-products-empty">No active business profiles are available yet. The sample profile above is a preview.</p> : null}
          </section>
        </div>

        <aside className="network-home-aside">
          <section className="territory-explorer-card"><div className="territory-explorer-heading"><div><h2>Explore territories</h2><p>Discover opportunities across Ethiopia’s regions.</p></div><span aria-hidden="true">⌖</span></div>
            {loading ? <p className="territory-empty">Loading territories…</p> : territories.length ? <ul>{territories.slice(0, showAllTerritories ? territories.length : 5).map((name, index) => <li key={name}><button onClick={() => { setFilter(setTerritory, name); document.getElementById("opportunity-feed")?.scrollIntoView({ behavior: "smooth" }); }} type="button"><span className={`territory-pin territory-pin-${index % 4}`} aria-hidden="true">⌖</span><span><strong>{name}</strong><small>{territoryCounts[name]} {territoryCounts[name] === 1 ? "opportunity" : "opportunities"}</small></span><b aria-hidden="true">›</b></button></li>)}</ul> : <p className="territory-empty">Territories with open opportunities will appear here.</p>}
            {territories.length > 5 ? <button className="text-button territory-show-all" onClick={() => setShowAllTerritories((value) => !value)} type="button">{showAllTerritories ? "Show fewer" : "View all territories"} →</button> : null}
          </section>
          <section className="network-new-member-card"><span className="network-new-member-illustration" aria-hidden="true"><i>R</i><b>＋</b></span><h2>New to Robsen?</h2><p>Create a business profile or join an existing business by accepting its owner’s invitation. Explore public opportunities in the meantime.</p><button className="primary-button" onClick={openBusinessSetup} type="button">Set up business</button></section>
        </aside>
      </div>
    </div>
  </main>;
}

function NetworkStat({ label, value, icon, tone, loading }: { label: string; value: number; icon: string; tone: string; loading: boolean }) {
  return <article className={`network-stat-card network-stat-${tone}`}><span aria-hidden="true">{icon}</span><div><small>{label}</small><strong>{loading ? "—" : value.toLocaleString()}</strong></div></article>;
}

function businessStatusLabel(status?: string | null) {
  if (!status) return "Status unavailable";
  if (status.toUpperCase() === "APPROVED") return "Verified";
  return status.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}
