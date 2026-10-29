"use client";

import Link from "next/link";
import { type FormEvent, useEffect, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { Alert } from "@/app/components/ui";
import { apiFetch } from "@/app/lib/api-client";
import { ACTIVE_BUSINESS_STORAGE_KEY } from "@/app/lib/auth";
import { BUSINESS_TYPE_OPTIONS, resources } from "@/app/lib/resources";

type PendingInvitation = {
  id: number;
  business: { legalName: string; tradeName?: string | null };
  role: { name: string };
};
type BusinessSummary = {
  id: number;
  legalName: string;
  tradeName?: string | null;
  businessType?: string | null;
  verificationStatus?: string | null;
  membershipRole?: string | null;
};
type BusinessStats = {
  activeMembers: number;
  activeListings: number;
  availableProducts: number;
  productCategories: number;
  incomingOrders: number;
  ordersAwaitingAction: number;
  purchaseHistory: number;
  orderItems: number;
  openOpportunities: number;
  pendingApplications: number;
  activeAgreements: number;
  territories: number;
};
type FeedListing = {
  listingId: number;
  productName: string;
  productDescription?: string | null;
  listingDescription?: string | null;
  category?: string | null;
  supplierBusinessId: number;
  supplierName: string;
};
type FeedOpportunity = {
  id: number;
  businessId: number;
  title: string;
  description?: string | null;
  territory?: string | null;
  businessName: string;
};

export default function DashboardPage() {
  return (
    <ProtectedArea>
      <DashboardContent />
    </ProtectedArea>
  );
}

function DashboardContent() {
  const profile = useAuthProfile();
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [createError, setCreateError] = useState("");
  const [creationNotice, setCreationNotice] = useState("");
  const [creating, setCreating] = useState(false);
  const [invitations, setInvitations] = useState<PendingInvitation[]>([]);
  const [businesses, setBusinesses] = useState<BusinessSummary[]>([]);
  const [businessesLoaded, setBusinessesLoaded] = useState(false);
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const [businessStats, setBusinessStats] = useState<BusinessStats | null>(null);
  const [businessStatsBusinessId, setBusinessStatsBusinessId] = useState("");
  const navigationItems = profile?.navigationItems;
  const visibleResources = resources.filter(
    (resource) =>
      navigationItems?.includes(resource.key) &&
      (!resource.adminOnly || profile?.superAdmin),
  );
  const activeBusinessId = selectedBusinessId;
  const selectedBusiness = businesses.find((business) => String(business.id) === activeBusinessId);
  const needsBusinessSelection = Boolean(profile && !profile.superAdmin
    && profile.businessIds.length > 1 && businessesLoaded && !activeBusinessId);

  useEffect(() => {
    if (window.sessionStorage.getItem("robsen-business-created") === "1") {
      window.sessionStorage.removeItem("robsen-business-created");
      const timeout = window.setTimeout(() => {
        setCreationNotice("Your business is ready. You are its owner, and it is pending verification.");
      }, 0);
      return () => window.clearTimeout(timeout);
    }
  }, []);

  useEffect(() => {
    if (!profile?.businessIds.length || profile.superAdmin) return;
    let active = true;
    apiFetch<BusinessSummary[]>("business/my")
      .then((rows) => {
        if (!active) return;
        setError("");
        setBusinesses(rows);
        const storedId = window.sessionStorage.getItem(ACTIVE_BUSINESS_STORAGE_KEY);
        const storedBusiness = rows.find((business) => String(business.id) === storedId);
        const initialBusiness = storedBusiness ?? (rows.length === 1 ? rows[0] : null);
        if (initialBusiness) {
          const id = String(initialBusiness.id);
          setLoading(true);
          setSelectedBusinessId(id);
          window.sessionStorage.setItem(ACTIVE_BUSINESS_STORAGE_KEY, id);
        } else {
          setSelectedBusinessId("");
          window.sessionStorage.removeItem(ACTIVE_BUSINESS_STORAGE_KEY);
          setLoading(false);
        }
      })
      .catch((requestError: unknown) => {
        if (active) {
          setBusinesses([]);
          setError(requestError instanceof Error ? requestError.message : "Could not load your business workspaces.");
          setLoading(false);
        }
      })
      .finally(() => { if (active) setBusinessesLoaded(true); });
    return () => { active = false; };
  }, [profile]);

  useEffect(() => {
    if (!activeBusinessId || !profile || profile.superAdmin) {
      return;
    }
    let active = true;
    apiFetch<BusinessStats>(`business/${activeBusinessId}/dashboard`)
      .then((stats) => {
        if (active) {
          setBusinessStats(stats);
          setBusinessStatsBusinessId(activeBusinessId);
        }
      })
      .catch((requestError: unknown) => {
        if (active) setError(requestError instanceof Error ? requestError.message : "Could not load business statistics.");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [activeBusinessId, profile]);

  useEffect(() => {
    if (!profile || profile.superAdmin) return;
    apiFetch<PendingInvitation[]>("business-memberships/invitations")
      .then(setInvitations)
      .catch(() => setInvitations([]));
  }, [profile]);

  useEffect(() => {
    if (!profile || !profile.superAdmin) return;
    let active = true;
    Promise.all(
      resources
        .filter(
          (resource) =>
            navigationItems?.includes(resource.key) &&
            (!resource.adminOnly || profile?.superAdmin) &&
            (profile?.superAdmin || profile?.permissions.includes(`${resource.key}:READ`)) &&
            (profile?.superAdmin || resource.key !== "products"),
        )
        .map(async (resource) => {
          const data = await apiFetch<unknown>(resource.listEndpoint);
          if (!Array.isArray(data)) {
            throw new Error(`${resource.label} endpoint did not return a list.`);
          }
          return [resource.key, data.length] as const;
        }),
    )
      .then((entries) => {
        if (active) setCounts(Object.fromEntries(entries));
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : "Could not load dashboard data.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [profile, navigationItems]);

  useEffect(() => {
    if (!profile || profile.superAdmin || profile.businessIds.length) return;
    let active = true;
    Promise.resolve().then(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [profile]);

  function selectBusiness(businessId: string) {
    if (businessId) setLoading(true);
    setSelectedBusinessId(businessId);
    setError("");
    if (businessId) window.sessionStorage.setItem(ACTIVE_BUSINESS_STORAGE_KEY, businessId);
    else window.sessionStorage.removeItem(ACTIVE_BUSINESS_STORAGE_KEY);
  }

  async function createBusiness(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreating(true);
    setCreateError("");
    const formData = new FormData(event.currentTarget);
    try {
      const response = await apiFetch("business", {
        method: "POST",
        body: JSON.stringify({
          legalName: formData.get("legalName"),
          tradeName: formData.get("tradeName"),
          businessType: formData.get("businessType"),
          email: formData.get("email"),
          phoneNumber: formData.get("phoneNumber"),
          address: formData.get("address"),
        }),
      });
      if (!response) throw new Error("Could not create your business.");
      window.sessionStorage.setItem("robsen-business-created", "1");
      window.location.reload();
    } catch (requestError) {
      setCreateError(requestError instanceof Error ? requestError.message : "Could not create your business.");
    } finally {
      setCreating(false);
    }
  }

  return (
    <main className="dashboard-page">
      <div className="dashboard-content">
        <div className="dashboard-heading">
          <div>
            <p className="dashboard-eyebrow">ROBSEN WORKSPACE</p>
            <h1>Your network, at a glance.</h1>
            <p className="dashboard-subtitle">
              Manage the businesses, products, and partnerships that keep your
              distribution network moving.
            </p>
          </div>
          <span className="dashboard-date">
            <span className="status-dot" /> Live workspace
          </span>
        </div>

        {error ? (
          <Alert tone="danger" className="dashboard-alert">
            {error}
          </Alert>
        ) : null}
        {creationNotice ? <p className="dashboard-success" role="status">{creationNotice}</p> : null}

        {businesses.length > 1 && activeBusinessId ? (
          <label className="business-selector dashboard-business-selector"><span>Business workspace</span><select onChange={(event) => selectBusiness(event.target.value)} value={activeBusinessId}>{businesses.map((business) => <option key={business.id} value={business.id}>{business.tradeName || business.legalName} · {business.membershipRole || "Member"}</option>)}</select></label>
        ) : null}

        {needsBusinessSelection ? (
          <section className="dashboard-empty-state business-choice-panel" aria-live="polite">
            <h2>Choose a business workspace</h2>
            <p>Choose which business to open. The overview and business data will use this workspace.</p>
            <label className="business-selector dashboard-business-selector"><span>Business workspace</span><select onChange={(event) => selectBusiness(event.target.value)} value=""><option value="">Choose a business</option>{businesses.map((business) => <option key={business.id} value={business.id}>{business.tradeName || business.legalName} · {business.membershipRole || "Member"}</option>)}</select></label>
          </section>
        ) : null}

        {businessStats && businessStatsBusinessId === activeBusinessId && selectedBusiness ? (
          <section className="business-summary" aria-label="Business overview">
            <div className="business-summary-title"><div><p className="dashboard-eyebrow">{selectedBusiness.tradeName || selectedBusiness.legalName}</p><h2>Business overview</h2></div><span className={`verification-pill verification-${(selectedBusiness.verificationStatus || "PENDING").toLowerCase()}`}>{(selectedBusiness.verificationStatus || "PENDING").replaceAll("_", " ")}</span></div>
            <div className="business-metric-grid">
              <Metric label="Active members" value={businessStats.activeMembers} />
              <Metric label="Available products" value={businessStats.availableProducts} />
              <Metric label="Active listings" value={businessStats.activeListings} />
              <Metric label="Incoming orders" value={businessStats.incomingOrders} />
              <Metric label="Orders awaiting action" value={businessStats.ordersAwaitingAction} />
              <Metric label="Purchase history" value={businessStats.purchaseHistory} />
              <Metric label="Pending applications" value={businessStats.pendingApplications} />
              <Metric label="Open opportunities" value={businessStats.openOpportunities} />
              <Metric label="Active agreements" value={businessStats.activeAgreements} />
              <Metric label="Territories" value={businessStats.territories} />
            </div>
          </section>
        ) : null}

        {profile ? <BusinessNewsFeed /> : null}

        {invitations.length ? (
          <section className="invitation-panel" aria-labelledby="invitation-heading">
            <div><p className="dashboard-eyebrow">PENDING INVITATIONS</p><h2 id="invitation-heading">Join a business</h2></div>
            {invitations.map((invitation) => (
              <div className="invitation-row" key={invitation.id}>
                <p><strong>{invitation.business.tradeName || invitation.business.legalName}</strong><span>Invited as {invitation.role.name.replaceAll("_", " ")}</span></p>
                <button className="primary-button" onClick={async () => {
                  try {
                    await apiFetch(`business-memberships/invitations/${invitation.id}/accept`, { method: "POST" });
                    window.location.reload();
                  } catch (requestError) {
                    setError(requestError instanceof Error ? requestError.message : "Could not accept this invitation.");
                  }
                }} type="button">Accept invitation</button>
              </div>
            ))}
          </section>
        ) : null}

        {(!needsBusinessSelection && (profile?.superAdmin || !profile?.businessIds.length || Boolean(activeBusinessId))) ? <div className="resource-grid">
          {visibleResources.map((resource, index) => (
            <Link
              className="resource-card"
              href={`/dashboard/${resource.key}`}
              key={resource.key}
            >
              <span className="resource-card-number">
                {String(index + 1).padStart(2, "0")}
              </span>
              <span className="resource-card-count">
                {loading ? "…" : (profile?.superAdmin ? counts[resource.key] : businessScopedCount(resource.key, businessStats) ?? "—")}
              </span>
              <span className="resource-card-label">{resource.label}</span>
              <span className="resource-card-description">
                {resource.description}
              </span>
              <span className="resource-card-link">
                Open records <span aria-hidden="true">↗</span>
              </span>
            </Link>
          ))}
        </div> : null}
        {!loading && visibleResources.length === 0 && !error ? (
          profile?.businessIds.length === 0 && !profile.superAdmin ? (
            <section className="dashboard-empty-state" aria-live="polite">
              <h2>Set up your business</h2>
              <p>Create a business profile to open your Robsen workspace. Your account will become its owner, and the business will remain pending verification until reviewed.</p>
              <form className="record-form onboarding-form" onSubmit={createBusiness}>
                <label className="field-control"><span>Legal business name *</span><input autoComplete="organization" name="legalName" required /></label>
                <label className="field-control"><span>Trading name</span><input name="tradeName" /></label>
                <label className="field-control"><span>Business type</span><select defaultValue="" name="businessType"><option value="">Choose a business type</option>{BUSINESS_TYPE_OPTIONS.map((type) => <option key={type} value={type}>{humanizeBusinessType(type)}</option>)}</select></label>
                <label className="field-control"><span>What does your business do?</span><textarea maxLength={2000} name="description" rows={3} /></label>
                <label className="field-control"><span>Business email</span><input autoComplete="email" name="email" type="email" /></label>
                <label className="field-control"><span>Phone</span><input autoComplete="tel" name="phoneNumber" /></label>
                <label className="field-control"><span>Address</span><input autoComplete="street-address" name="address" /></label>
                {createError ? <p className="form-error" role="alert">{createError}</p> : null}
                <div className="form-actions"><button className="primary-button" disabled={creating} type="submit">{creating ? "Creating…" : "Create business"}</button></div>
              </form>
              <div className="onboarding-alternatives">
                <div><h3>Join an existing business</h3><p>Business access is invitation based. Accept an invitation below or ask the business owner to invite the email on your Robsen account.</p></div>
                <div><h3>Explore Robsen</h3><p>Browse public supplier products and distribution opportunities while you get set up.</p><Link className="text-button" href="/dashboard/discover">Explore marketplace</Link></div>
              </div>
            </section>
          ) : (
            <section className="dashboard-empty-state" aria-live="polite">
              <h2>Your workspace is being set up</h2>
              <p>No business tools are available for this account yet. Ask your business owner or platform administrator to review your membership role.</p>
            </section>
          )
        ) : null}
      </div>
    </main>
  );
}

function BusinessNewsFeed() {
  const profile = useAuthProfile();
  const [listings, setListings] = useState<FeedListing[]>([]);
  const [opportunities, setOpportunities] = useState<FeedOpportunity[]>([]);
  const [loadingFeed, setLoadingFeed] = useState(true);
  const [hasFeedError, setHasFeedError] = useState(false);

  useEffect(() => {
    let active = true;
    Promise.allSettled([
      apiFetch<FeedListing[]>("product-listings/marketplace"),
      apiFetch<FeedOpportunity[]>("distribution-opportunities/marketplace"),
    ]).then(([listingResult, opportunityResult]) => {
      if (!active) return;
      if (listingResult.status === "fulfilled") setListings(listingResult.value.slice(0, 4));
      if (opportunityResult.status === "fulfilled") setOpportunities(opportunityResult.value.slice(0, 4));
      setHasFeedError(listingResult.status === "rejected" && opportunityResult.status === "rejected");
    }).finally(() => { if (active) setLoadingFeed(false); });
    return () => { active = false; };
  }, []);

  const entries = [
    ...opportunities.filter((opportunity) => !profile?.businessIds.includes(opportunity.businessId)).map((opportunity) => ({
      id: `opportunity-${opportunity.id}`,
      label: "Distribution opportunity",
      title: opportunity.title,
      business: opportunity.businessName,
      description: [opportunity.territory, opportunity.description].filter(Boolean).join(" · "),
    })),
    ...listings.filter((listing) => !profile?.businessIds.includes(listing.supplierBusinessId)).map((listing) => ({
      id: `listing-${listing.listingId}`,
      label: listing.category || "Supplier update",
      title: listing.productName,
      business: listing.supplierName,
      description: listing.listingDescription || listing.productDescription || "A business has shared an active product listing.",
    })),
  ].slice(0, 6);

  return <section aria-label="News from the business network" className="network-feed">
    <div className="list-heading"><div><p className="dashboard-eyebrow">FROM THE NETWORK</p><h2>Business news & opportunities</h2><p className="dashboard-subtitle">Open opportunities and product information shared by approved businesses.</p></div><Link className="text-button" href="/dashboard/discover">Explore the network ↗</Link></div>
    {loadingFeed ? <div className="empty-state">Loading business news…</div>
      : hasFeedError ? <div className="empty-state">Network updates are temporarily unavailable.</div>
        : entries.length ? <div className="network-feed-grid">{entries.map((entry) => <article className="network-feed-card" key={entry.id}>
          <span>{entry.label}</span><h3>{entry.title}</h3><strong>{entry.business}</strong><p>{entry.description}</p>
          <Link href="/dashboard/discover">View details <span aria-hidden="true">↗</span></Link>
        </article>)}</div>
          : <div className="empty-state">There are no network updates yet. Check back as businesses publish opportunities and listings.</div>}
  </section>;
}

function humanizeBusinessType(type: string) {
  return type.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function Metric({ label, value }: { label: string; value: number }) {
  return <div className="business-metric"><span>{value}</span><small>{label}</small></div>;
}

function businessScopedCount(key: string, stats: BusinessStats | null) {
  if (!stats) return undefined;
  const counts: Record<string, number> = {
    businesses: 1,
    memberships: stats.activeMembers,
    "product-categories": stats.productCategories,
    products: stats.availableProducts,
    "product-listings": stats.activeListings,
    territories: stats.territories,
    opportunities: stats.openOpportunities,
    "partner-applications": stats.pendingApplications,
    agreements: stats.activeAgreements,
    orders: stats.incomingOrders + stats.purchaseHistory,
    "order-items": stats.orderItems,
  };
  return counts[key];
}
