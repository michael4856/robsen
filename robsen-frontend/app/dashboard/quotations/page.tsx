"use client";

import { useEffect, useMemo, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { Alert, Button, Panel, StatusBadge } from "@/app/components/ui";
import { apiFetch } from "@/app/lib/api-client";
import { ACTIVE_BUSINESS_STORAGE_KEY } from "@/app/lib/auth";
import LocationMapLink from "@/app/components/location-map-link";

type Business = { id: number; legalName: string };
type MarketListing = { listingId: number; productId: number; productName: string; supplierName: string };
type OwnListing = { id: number; active: boolean; product: { id: number; name: string } };
type RequestItem = { id: number; productId: number; productName: string; quantity: number; specifications?: string | null };
type RFQ = { id: number; rfqNumber: string; buyerBusinessId: number; buyerName: string; title: string; status: string; responseDeadline?: string | null; deliveryLocation?: string | null; items: RequestItem[] };
type QuoteLine = { requestItemId: number; productListingId: number; productName: string; offeredQuantity: number; minimumOrderQuantity: number; unitPrice: number };
type Quote = { id: number; quotationNumber: string; requestId: number; supplierBusinessId: number; supplierName: string; verificationStatus: string; status: string; validUntil?: string | null; deliveryLeadDays: number; paymentTerms?: string | null; deliveryTerms?: string | null; remarks?: string | null; items: QuoteLine[] };

export default function QuotationsPage() { return <ProtectedArea><QuotationsContent /></ProtectedArea>; }

function QuotationsContent() {
  const profile = useAuthProfile();
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [businessId, setBusinessId] = useState("");
  const [marketListings, setMarketListings] = useState<MarketListing[]>([]);
  const [ownListings, setOwnListings] = useState<OwnListing[]>([]);
  const [requests, setRequests] = useState<RFQ[]>([]);
  const [selected, setSelected] = useState<RFQ | null>(null);
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [productId, setProductId] = useState("");
  const [quantity, setQuantity] = useState("1");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [deliveryLocation, setDeliveryLocation] = useState("");
  const [prices, setPrices] = useState<Record<number, string>>({});
  const [leadDays, setLeadDays] = useState("7");
  const [paymentTerms, setPaymentTerms] = useState("");
  const [deliveryTerms, setDeliveryTerms] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const usableProducts = useMemo(() => marketListings.filter((row, index, all) => all.findIndex((other) => other.productId === row.productId) === index), [marketListings]);

  useEffect(() => {
    if (!profile) return;
    Promise.all([
      profile.superAdmin ? Promise.resolve([] as Business[]) : apiFetch<Business[]>("business/my"),
      apiFetch<MarketListing[]>("product-listings/marketplace"),
      apiFetch<RFQ[]>("quotation-requests"),
    ]).then(([rows, listings, rfqs]) => {
      setBusinesses(rows); setMarketListings(listings); setRequests(rfqs);
      const stored = window.sessionStorage.getItem(ACTIVE_BUSINESS_STORAGE_KEY);
      const id = stored && rows.some((row) => String(row.id) === stored) ? stored : rows.length === 1 ? String(rows[0].id) : "";
      setBusinessId(id);
    }).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Could not load quotation workspace."));
  }, [profile]);
  useEffect(() => {
    if (!businessId || profile?.superAdmin) return;
    apiFetch<OwnListing[]>(`product-listings/business/${businessId}`).then(setOwnListings).catch(() => setOwnListings([]));
  }, [businessId, profile?.superAdmin]);
  async function refreshRequests() {
    setRequests(await apiFetch<RFQ[]>("quotation-requests"));
    if (selected) setSelected((await apiFetch<RFQ[]>("quotation-requests")).find((row) => row.id === selected.id) ?? selected);
  }
  async function createRequest(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError(""); setNotice("");
    try {
      const result = await apiFetch<RFQ>("quotation-requests", { method: "POST", body: JSON.stringify({ buyerBusinessId: Number(businessId), title, description, deliveryLocation, items: [{ productId: Number(productId), quantity: Number(quantity) }] }) });
      setTitle(""); setNotice(`RFQ ${result.rfqNumber} saved as a draft.`); await refreshRequests(); setSelected(result);
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Could not create RFQ."); }
    finally { setBusy(false); }
  }
  async function publish(rfq: RFQ) {
    try { await apiFetch(`quotation-requests/${rfq.id}/publish`, { method: "POST" }); setNotice("RFQ published to suppliers."); await refreshRequests(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not publish RFQ."); }
  }
  async function openQuotes(rfq: RFQ) {
    setSelected(rfq); setError("");
    try { setQuotes(await apiFetch<Quote[]>(`quotation-requests/${rfq.id}/quotations`)); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not load quotations."); }
  }
  async function submitQuote(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected) return; setBusy(true); setError(""); setNotice("");
    const lines: { requestItemId: number; productListingId: number; offeredQuantity: number; minimumOrderQuantity: number; unitPrice: number }[] = [];
    for (const item of selected.items) {
      const listing = ownListings.find((row) => row.active && row.product.id === item.productId);
      if (!listing) { setError(`You need an active listing for ${item.productName} before quoting.`); setBusy(false); return; }
      const price = Number(prices[item.id]); if (!Number.isFinite(price) || price < 0) { setError(`Enter a valid price for ${item.productName}.`); setBusy(false); return; }
      lines.push({ requestItemId: item.id, productListingId: listing.id, offeredQuantity: item.quantity, minimumOrderQuantity: 1, unitPrice: price });
    }
    try {
      await apiFetch<Quote>(`quotation-requests/${selected.id}/quotations`, { method: "POST", body: JSON.stringify({ supplierBusinessId: Number(businessId), deliveryLeadDays: Number(leadDays), paymentTerms, deliveryTerms, items: lines }) });
      setNotice("Quotation submitted to the buyer."); await openQuotes(selected);
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Could not submit quotation."); }
    finally { setBusy(false); }
  }
  async function accept(quote: Quote) {
    if (!selected) return; setBusy(true); setError("");
    try { const result = await apiFetch<{ orderNumber: string }>(`quotation-requests/${selected.id}/accept`, { method: "POST", body: JSON.stringify({ quotationId: quote.id }) }); setNotice(`Quotation accepted. Order ${result.orderNumber} is now pending.`); await refreshRequests(); await openQuotes(selected); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not accept quotation."); }
    finally { setBusy(false); }
  }
  const ownRfq = (rfq: RFQ) => String(rfq.buyerBusinessId) === businessId;
  const listedItems = selected?.items.map((item) => ({ item, listing: ownListings.find((row) => row.active && row.product.id === item.productId) })) ?? [];

  return <main className="dashboard-page"><div className="dashboard-content">
    <header className="dashboard-heading"><div><p className="dashboard-eyebrow">WHOLESALE SOURCING</p><h1>RFQs &amp; quotations</h1><p className="dashboard-subtitle">Request offers from suppliers, compare terms and turn an accepted quotation into an order.</p></div></header>
    {error ? <Alert tone="danger">{error}</Alert> : null}{notice ? <Alert tone="success">{notice}</Alert> : null}
    {businesses.length > 1 ? <label className="form-field">Business<select value={businessId} onChange={(event) => { setBusinessId(event.target.value); window.sessionStorage.setItem(ACTIVE_BUSINESS_STORAGE_KEY, event.target.value); }}><option value="">Choose a business</option>{businesses.map((row) => <option key={row.id} value={row.id}>{row.legalName}</option>)}</select></label> : null}
    {businessId ? <Panel><h2>Create a request for quotation</h2><form className="onboarding-form" onSubmit={createRequest}>
      <label className="form-field">Title<input value={title} onChange={(event) => setTitle(event.target.value)} required /></label>
      <label className="form-field">Product<select value={productId} onChange={(event) => setProductId(event.target.value)} required><option value="">Choose a product</option>{usableProducts.map((row) => <option key={row.productId} value={row.productId}>{row.productName}</option>)}</select></label>
      <label className="form-field">Quantity<input type="number" min="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></label>
      <div className="form-field"><label htmlFor="rfq-delivery-location">Delivery location</label><input id="rfq-delivery-location" value={deliveryLocation} onChange={(event) => setDeliveryLocation(event.target.value)} />{deliveryLocation ? <LocationMapLink location={deliveryLocation} /> : null}</div>
      <label className="form-field">Description<textarea value={description} onChange={(event) => setDescription(event.target.value)} /></label>
      <Button disabled={busy || !businessId || !productId}>Save draft RFQ</Button>
    </form></Panel> : null}
    <Panel><h2>Requests and available RFQs</h2><div className="table-scroll"><table><thead><tr><th>RFQ</th><th>Buyer</th><th>Title</th><th>Products</th><th>Status</th><th /></tr></thead><tbody>{requests.map((rfq) => <tr key={rfq.id}><td>{rfq.rfqNumber}</td><td>{rfq.buyerName}</td><td>{rfq.title}</td><td>{rfq.items.length}</td><td><StatusBadge>{rfq.status}</StatusBadge></td><td><div className="form-actions">{ownRfq(rfq) && rfq.status === "DRAFT" ? <Button variant="secondary" onClick={() => void publish(rfq)}>Publish</Button> : null}<Button variant="outline" onClick={() => void openQuotes(rfq)}>Details / quotes</Button></div></td></tr>)}{requests.length === 0 ? <tr><td colSpan={6}>No RFQs are available.</td></tr> : null}</tbody></table></div></Panel>
    {selected ? <Panel><div className="dashboard-heading"><div><h2>{selected.rfqNumber} · {selected.title}</h2><p>{selected.buyerName} · {selected.deliveryLocation ?? "No delivery location"}</p>{selected.deliveryLocation ? <LocationMapLink location={selected.deliveryLocation} /> : null}</div><Button variant="quiet" onClick={() => setSelected(null)}>Close</Button></div>
      <ul>{selected.items.map((item) => <li key={item.id}>{item.productName} · requested {item.quantity}{item.specifications ? ` · ${item.specifications}` : ""}</li>)}</ul>
      {selected.status === "OPEN" && !ownRfq(selected) && businessId ? <form className="onboarding-form" onSubmit={submitQuote}><h3>Submit your quotation</h3>{listedItems.map(({ item, listing }) => <div className="form-field" key={item.id}><label>{item.productName} · quantity {item.quantity} · {listing ? `listing ${listing.id}` : "No matching active listing"}</label><input aria-label={`${item.productName} unit price`} type="number" min="0" step="0.01" placeholder="Unit price" value={prices[item.id] ?? ""} onChange={(event) => setPrices({ ...prices, [item.id]: event.target.value })} disabled={!listing} /></div>)}<label className="form-field">Delivery lead days<input type="number" min="0" value={leadDays} onChange={(event) => setLeadDays(event.target.value)} /></label><label className="form-field">Payment terms<input value={paymentTerms} onChange={(event) => setPaymentTerms(event.target.value)} /></label><label className="form-field">Delivery terms<input value={deliveryTerms} onChange={(event) => setDeliveryTerms(event.target.value)} /></label><Button disabled={busy || listedItems.some((row) => !row.listing)}>Submit quotation</Button></form> : null}
      <h3>Supplier offers</h3><div className="table-scroll"><table><thead><tr><th>Supplier</th><th>Verification</th><th>Items</th><th>Lead time</th><th>Terms</th><th>Total</th><th /></tr></thead><tbody>{quotes.map((quote) => <tr key={quote.id}><td>{quote.supplierName}</td><td>{quote.verificationStatus}</td><td>{quote.items.map((line) => `${line.productName}: ${line.offeredQuantity} × ${line.unitPrice}`).join(" · ")}</td><td>{quote.deliveryLeadDays} days</td><td>{quote.paymentTerms ?? "—"} / {quote.deliveryTerms ?? "—"}</td><td>{quote.items.reduce((sum, line) => sum + line.offeredQuantity * line.unitPrice, 0).toFixed(2)}</td><td>{ownRfq(selected) && quote.status === "SUBMITTED" ? <Button disabled={busy} onClick={() => void accept(quote)}>Accept</Button> : <StatusBadge>{quote.status}</StatusBadge>}</td></tr>)}{quotes.length === 0 ? <tr><td colSpan={7}>No quotations submitted yet.</td></tr> : null}</tbody></table></div>
    </Panel> : null}
  </div></main>;
}
