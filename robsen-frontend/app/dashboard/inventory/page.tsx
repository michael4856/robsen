"use client";

import { useCallback, useEffect, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { Alert, Button } from "@/app/components/ui";
import { apiFetch } from "@/app/lib/api-client";
import { ACTIVE_BUSINESS_STORAGE_KEY } from "@/app/lib/auth";

type Business = { id: number; legalName: string };
type InventoryRow = { id: number; productListingId: number; productName: string; quantityOnHand: number; quantityReserved: number; availableQuantity: number; reorderLevel: number; stockStatus: string };
type Movement = { id: number; productName: string; type: string; quantity: number; previousQuantity: number; newQuantity: number; reason: string; createdAt: string };
type Page<T> = { content: T[]; totalElements: number; totalPages: number };

export default function InventoryPage() { return <ProtectedArea><InventoryContent /></ProtectedArea>; }

function InventoryContent() {
  const profile = useAuthProfile();
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [businessId, setBusinessId] = useState("");
  const [rows, setRows] = useState<InventoryRow[]>([]);
  const [movements, setMovements] = useState<Movement[]>([]);
  const [selected, setSelected] = useState<InventoryRow | null>(null);
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("");
  const [pageNumber, setPageNumber] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [mode, setMode] = useState<"in" | "adjust">("in");
  const [quantity, setQuantity] = useState("");
  const [reason, setReason] = useState("");
  const [reorderLevel, setReorderLevel] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!profile || profile.superAdmin) return;
    apiFetch<Business[]>("business/my").then((items) => {
      setBusinesses(items);
      const stored = window.sessionStorage.getItem(ACTIVE_BUSINESS_STORAGE_KEY);
      const id = stored && items.some((row) => String(row.id) === stored) ? stored : items.length === 1 ? String(items[0].id) : "";
      setBusinessId(id);
    }).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Could not load businesses."));
  }, [profile]);

  const load = useCallback(async () => {
    if (!businessId) return;
    try {
      const params = new URLSearchParams({ page: String(pageNumber), size: "20" });
      if (query.trim()) params.set("q", query.trim());
      if (filter) params.set("status", filter);
      const result = await apiFetch<Page<InventoryRow>>(`inventory/${businessId}?${params}`);
      setRows(result.content); setTotalPages(result.totalPages);
      const history = await apiFetch<Page<Movement>>(`stock-movements/business/${businessId}?page=0&size=30`);
      setMovements(history.content);
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Could not load inventory."); }
  }, [businessId, filter, query, pageNumber]);
  useEffect(() => { void Promise.resolve().then(load); }, [load]);
  const lowStockCount = rows.filter((row) => row.stockStatus !== "IN_STOCK").length;
  const availableUnits = rows.reduce((sum, row) => sum + row.availableQuantity, 0);
  const reservedUnits = rows.reduce((sum, row) => sum + row.quantityReserved, 0);
  const stockStatusLabel = (status: string) => status.replaceAll("_", " ").toLowerCase();
  const stockStatusClass = (status: string) => status.toLowerCase().replaceAll("_", "-");
  async function saveStock(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selected) return;
    setBusy(true); setError(""); setNotice("");
    try {
      const url = mode === "in" ? `inventory/listings/${selected.productListingId}/stock-in` : `inventory/listings/${selected.productListingId}/adjust`;
      const body = mode === "in" ? { quantity: Number(quantity), reason } : { targetAvailable: Number(quantity), reason };
      await apiFetch<InventoryRow>(url, { method: "POST", body: JSON.stringify(body) });
      setNotice("Inventory updated and movement recorded."); setQuantity(""); setReason(""); await load();
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Could not update stock."); }
    finally { setBusy(false); }
  }
  async function saveReorder() {
    if (!selected) return;
    setBusy(true); setError(""); setNotice("");
    try { await apiFetch<InventoryRow>(`inventory/listings/${selected.productListingId}/reorder-level`, { method: "PUT", body: JSON.stringify({ reorderLevel: Number(reorderLevel) }) }); setNotice("Reorder level saved."); await load(); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not save reorder level."); }
    finally { setBusy(false); }
  }

  return (
    <main className="dashboard-page inventory-dashboard">
      <div className="dashboard-content">
        <header className="inventory-heading">
          <div>
            <p className="dashboard-eyebrow">OPERATIONS CENTER</p>
            <h1>Inventory overview</h1>
            <p>Monitor stock levels, manage replenishment, and review recent warehouse activity.</p>
          </div>
          {businesses.length > 1 ? (
            <label className="inventory-business-select">
              <span>Business workspace</span>
              <select value={businessId} onChange={(event) => {
                setBusinessId(event.target.value);
                setPageNumber(0);
                window.sessionStorage.setItem(ACTIVE_BUSINESS_STORAGE_KEY, event.target.value);
              }}>
                <option value="">Choose a business</option>
                {businesses.map((row) => <option key={row.id} value={row.id}>{row.legalName}</option>)}
              </select>
            </label>
          ) : businesses.length === 1 ? (
            <div className="inventory-workspace-chip"><span>WORKSPACE</span><strong>{businesses[0].legalName}</strong></div>
          ) : null}
        </header>

        {error ? <Alert className="inventory-alert" tone="danger">{error}</Alert> : null}
        {notice ? <Alert className="inventory-alert" tone="success">{notice}</Alert> : null}

        <section aria-label="Inventory summary" className="inventory-summary-grid">
          <article className="inventory-summary-card">
            <span className="inventory-summary-icon inventory-icon-blue" aria-hidden="true">▦</span>
            <div><p>Listings on this page</p><strong>{rows.length}</strong><small>of {rows.length ? "current results" : "no results"}</small></div>
          </article>
          <article className="inventory-summary-card">
            <span className="inventory-summary-icon inventory-icon-green" aria-hidden="true">↗</span>
            <div><p>Available units</p><strong>{availableUnits.toLocaleString()}</strong><small>ready to fulfill</small></div>
          </article>
          <article className="inventory-summary-card">
            <span className="inventory-summary-icon inventory-icon-violet" aria-hidden="true">◷</span>
            <div><p>Reserved units</p><strong>{reservedUnits.toLocaleString()}</strong><small>committed to orders</small></div>
          </article>
          <article className={`inventory-summary-card${lowStockCount ? " inventory-summary-warning" : ""}`}>
            <span className="inventory-summary-icon inventory-icon-amber" aria-hidden="true">!</span>
            <div><p>Needs attention</p><strong>{lowStockCount}</strong><small>low or out on this page</small></div>
          </article>
        </section>

        <section aria-labelledby="inventory-list-title" className="inventory-panel">
          <div className="inventory-panel-heading">
            <div><p className="inventory-panel-kicker">STOCK CONTROL</p><h2 id="inventory-list-title">Inventory items</h2><span>Track available, reserved, and reorder quantities.</span></div>
            <span className="inventory-result-count">{rows.length} shown</span>
          </div>
          <div className="inventory-toolbar">
            <label className="inventory-search">
              <span aria-hidden="true">⌕</span>
              <span className="visually-hidden">Search inventory</span>
              <input aria-label="Search inventory" placeholder="Search products..." value={query} onChange={(event) => { setQuery(event.target.value); setPageNumber(0); }} onKeyDown={(event) => { if (event.key === "Enter") void load(); }} />
            </label>
            <label className="inventory-filter">
              <span className="visually-hidden">Filter stock status</span>
              <select aria-label="Filter stock status" value={filter} onChange={(event) => { setFilter(event.target.value); setPageNumber(0); }}>
                <option value="">All stock statuses</option><option value="IN_STOCK">In stock</option><option value="LOW_STOCK">Low stock</option><option value="OUT_OF_STOCK">Out of stock</option>
              </select>
            </label>
            <Button className="inventory-refresh-button" variant="secondary" onClick={() => void load()}>Refresh</Button>
          </div>
          <div className="table-scroll inventory-table-scroll">
            <table className="records-table inventory-table">
              <thead><tr><th>Product</th><th>On hand</th><th>Reserved</th><th>Available</th><th>Reorder at</th><th>Status</th><th><span className="visually-hidden">Actions</span></th></tr></thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.productListingId}>
                    <td><div className="inventory-product"><span aria-hidden="true">{row.productName.slice(0, 2).toUpperCase()}</span><strong>{row.productName}</strong></div></td>
                    <td>{row.quantityOnHand.toLocaleString()}</td>
                    <td>{row.quantityReserved.toLocaleString()}</td>
                    <td><strong className="inventory-available-value">{row.availableQuantity.toLocaleString()}</strong></td>
                    <td>{row.reorderLevel.toLocaleString()}</td>
                    <td><span className={`inventory-status inventory-status-${stockStatusClass(row.stockStatus)}`}><i aria-hidden="true" />{stockStatusLabel(row.stockStatus)}</span></td>
                    <td><Button className="inventory-manage-button" variant="outline" onClick={() => { setSelected(row); setReorderLevel(String(row.reorderLevel)); }}>Manage stock</Button></td>
                  </tr>
                ))}
                {rows.length === 0 ? <tr><td className="inventory-empty-row" colSpan={7}>{businessId ? "No inventory listings match these filters." : "Choose a business workspace to view its inventory."}</td></tr> : null}
              </tbody>
            </table>
          </div>
          <div className="inventory-pagination">
            <span>Page {totalPages ? pageNumber + 1 : 0} of {totalPages}</span>
            <div><Button variant="secondary" disabled={pageNumber === 0} onClick={() => setPageNumber((current) => Math.max(0, current - 1))}>Previous</Button><Button variant="secondary" disabled={pageNumber + 1 >= totalPages} onClick={() => setPageNumber((current) => current + 1)}>Next</Button></div>
          </div>
        </section>

        {selected ? (
          <section aria-labelledby="inventory-manage-title" className="inventory-manage-panel">
            <div className="inventory-panel-heading">
              <div><p className="inventory-panel-kicker">STOCK ADJUSTMENT</p><h2 id="inventory-manage-title">{selected.productName}</h2><span>Update inventory and configure its replenishment threshold.</span></div>
              <Button variant="quiet" onClick={() => setSelected(null)}>Close</Button>
            </div>
            <div className="inventory-manage-grid">
              <form className="inventory-adjust-card" onSubmit={saveStock}>
                <label className="inventory-field"><span>Operation</span><select value={mode} onChange={(event) => setMode(event.target.value as "in" | "adjust")}><option value="in">Add stock</option><option value="adjust">Set available quantity</option></select></label>
                <label className="inventory-field"><span>{mode === "in" ? "Quantity to add" : "Target available quantity"}</span><input type="number" min="0" value={quantity} onChange={(event) => setQuantity(event.target.value)} required /></label>
                <label className="inventory-field"><span>Reason</span><input value={reason} onChange={(event) => setReason(event.target.value)} required /></label>
                <Button disabled={busy}>{busy ? "Saving…" : "Save stock change"}</Button>
              </form>
              <div className="inventory-adjust-card inventory-reorder-card">
                <label className="inventory-field"><span>Reorder level</span><input type="number" min="0" value={reorderLevel} onChange={(event) => setReorderLevel(event.target.value)} /></label>
                <p>Get a clear signal when available stock reaches this quantity.</p>
                <Button variant="secondary" disabled={busy} onClick={saveReorder}>{busy ? "Saving…" : "Save reorder level"}</Button>
              </div>
            </div>
          </section>
        ) : null}

        <section aria-labelledby="inventory-activity-title" className="inventory-panel inventory-activity-panel">
          <div className="inventory-panel-heading">
            <div><p className="inventory-panel-kicker">AUDIT TRAIL</p><h2 id="inventory-activity-title">Recent stock movements</h2><span>Latest changes recorded for this business.</span></div>
            <span className="inventory-result-count">{movements.length} recent</span>
          </div>
          <div className="table-scroll inventory-table-scroll">
            <table className="records-table inventory-table movement-table">
              <thead><tr><th>Date</th><th>Product</th><th>Movement</th><th>Quantity</th><th>Before</th><th>After</th><th>Reason</th></tr></thead>
              <tbody>
                {movements.map((row) => (
                  <tr key={row.id}>
                    <td>{new Date(row.createdAt).toLocaleString()}</td><td><strong>{row.productName}</strong></td>
                    <td><span className={`inventory-movement inventory-movement-${row.type.toLowerCase().replaceAll("_", "-")}`}>{row.type.replaceAll("_", " ")}</span></td>
                    <td><strong>{row.quantity.toLocaleString()}</strong></td><td>{row.previousQuantity.toLocaleString()}</td><td>{row.newQuantity.toLocaleString()}</td><td>{row.reason}</td>
                  </tr>
                ))}
                {movements.length === 0 ? <tr><td className="inventory-empty-row" colSpan={7}>No stock movements recorded yet.</td></tr> : null}
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </main>
  );
}
