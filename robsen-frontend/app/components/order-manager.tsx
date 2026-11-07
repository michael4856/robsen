"use client";

import { useCallback, useEffect, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { Alert, Button, Panel, StatusBadge } from "@/app/components/ui";
import { apiFetch } from "@/app/lib/api-client";
import { ACTIVE_BUSINESS_STORAGE_KEY } from "@/app/lib/auth";

type Business = { id: number; legalName: string };
type BusinessLink = { id: number; legalName: string };
type Order = { id: number; orderNumber: string; buyerBusiness: BusinessLink; supplierBusiness: BusinessLink; status: string; subtotal: number; rejectionReason?: string | null; processingRemarks?: string | null };
type Item = { id: number; productListing: { product: { name: string } }; quantity: number; unitPrice: number; lineTotal: number };
type History = { previousStatus?: string | null; newStatus: string; changedByUserId: number; remarks?: string | null; createdAt: string };

export default function OrderManager() { return <ProtectedArea><OrderContent /></ProtectedArea>; }
function OrderContent() {
  const profile = useAuthProfile();
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [businessId, setBusinessId] = useState("");
  const [orders, setOrders] = useState<Order[]>([]);
  const [selected, setSelected] = useState<Order | null>(null);
  const [items, setItems] = useState<Item[]>([]);
  const [history, setHistory] = useState<History[]>([]);
  const [nextStatus, setNextStatus] = useState("");
  const [remarks, setRemarks] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (!profile || profile.superAdmin) return;
    apiFetch<Business[]>("business/my").then((rows) => {
      setBusinesses(rows); const saved = window.sessionStorage.getItem(ACTIVE_BUSINESS_STORAGE_KEY);
      setBusinessId(saved && rows.some((row) => String(row.id) === saved) ? saved : rows.length ? String(rows[0].id) : "");
    }).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Could not load business list."));
  }, [profile]);
  const load = useCallback(async () => {
    try { setOrders(await apiFetch<Order[]>("orders")); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not load orders."); }
  }, []);
  useEffect(() => { void Promise.resolve().then(load); }, [load]);
  const isSupplier = (order: Order) => profile?.superAdmin || String(order.supplierBusiness.id) === businessId;
  function actions(order: Order) {
    if (profile?.superAdmin) {
      if (order.status === "PENDING") return ["CONFIRMED", "REJECTED", "CANCELLED"];
      if (order.status === "CONFIRMED") return ["PROCESSING", "CANCELLED"];
      if (order.status === "PROCESSING") return ["READY_FOR_DISPATCH"];
      if (order.status === "READY_FOR_DISPATCH") return ["SHIPPED"];
      if (order.status === "SHIPPED") return ["DELIVERED"];
      if (order.status === "DELIVERED") return ["COMPLETED"];
      return [];
    }
    const supplier = isSupplier(order);
    if (order.status === "PENDING") return supplier ? ["CONFIRMED", "REJECTED"] : ["CANCELLED"];
    if (order.status === "CONFIRMED") return supplier ? ["PROCESSING"] : ["CANCELLED"];
    if (order.status === "PROCESSING") return supplier ? ["READY_FOR_DISPATCH"] : [];
    if (order.status === "READY_FOR_DISPATCH") return supplier ? ["SHIPPED"] : [];
    if (order.status === "SHIPPED") return supplier ? [] : ["DELIVERED"];
    if (order.status === "DELIVERED") return supplier ? [] : ["COMPLETED"];
    return [];
  }
  async function open(order: Order) {
    setSelected(order); setNextStatus(""); setError("");
    try { const [orderItems, orderHistory] = await Promise.all([apiFetch<Item[]>(`order-items/order/${order.id}`), apiFetch<History[]>(`orders/${order.id}/history`)]); setItems(orderItems); setHistory(orderHistory); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not load order details."); }
  }
  async function update() {
    if (!selected || !nextStatus) return;
    if (nextStatus === "REJECTED" && !rejectionReason.trim()) { setError("A rejection reason is required."); return; }
    setBusy(true); setError(""); setNotice("");
    try { const updated = await apiFetch<Order>(`orders/${selected.id}`, { method: "PUT", body: JSON.stringify({ status: nextStatus, rejectionReason, processingRemarks: remarks }) }); setSelected(updated); setNotice(`Order moved to ${nextStatus.replaceAll("_", " ")}.`); setRejectionReason(""); setRemarks(""); await load(); await open(updated); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not update order."); }
    finally { setBusy(false); }
  }
  const visibleOrders = orders.filter((order) => profile?.superAdmin || String(order.buyerBusiness.id) === businessId || String(order.supplierBusiness.id) === businessId);
  return <main className="dashboard-page"><div className="dashboard-content">
    <header className="dashboard-heading"><div><p className="dashboard-eyebrow">WHOLESALE OPERATIONS</p><h1>Orders</h1><p className="dashboard-subtitle">Process incoming orders, follow dispatch and record buyer delivery decisions.</p></div></header>
    {error ? <Alert tone="danger">{error}</Alert> : null}{notice ? <Alert tone="success">{notice}</Alert> : null}
    {businesses.length > 1 ? <label className="form-field">Business<select value={businessId} onChange={(event) => { setBusinessId(event.target.value); window.sessionStorage.setItem(ACTIVE_BUSINESS_STORAGE_KEY, event.target.value); }}>{businesses.map((row) => <option key={row.id} value={row.id}>{row.legalName}</option>)}</select></label> : null}
    <Panel><div className="table-scroll"><table><thead><tr><th>Order</th><th>Buyer</th><th>Supplier</th><th>Status</th><th>Subtotal</th><th /></tr></thead><tbody>{visibleOrders.map((order) => <tr key={order.id}><td>{order.orderNumber}</td><td>{order.buyerBusiness.legalName}</td><td>{order.supplierBusiness.legalName}</td><td><StatusBadge>{order.status.replaceAll("_", " ")}</StatusBadge></td><td>{Number(order.subtotal).toFixed(2)}</td><td><Button variant="outline" onClick={() => void open(order)}>Details</Button></td></tr>)}{visibleOrders.length === 0 ? <tr><td colSpan={6}>No orders for this business.</td></tr> : null}</tbody></table></div></Panel>
    {selected ? <Panel><div className="dashboard-heading"><div><h2>{selected.orderNumber}</h2><p>{selected.buyerBusiness.legalName} → {selected.supplierBusiness.legalName}</p></div><Button variant="quiet" onClick={() => setSelected(null)}>Close</Button></div>
      {selected.rejectionReason ? <Alert tone="danger">Rejection reason: {selected.rejectionReason}</Alert> : null}
      <div className="table-scroll"><table><thead><tr><th>Product</th><th>Quantity</th><th>Unit price</th><th>Line total</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td>{item.productListing.product.name}</td><td>{item.quantity}</td><td>{Number(item.unitPrice).toFixed(2)}</td><td>{Number(item.lineTotal).toFixed(2)}</td></tr>)}</tbody></table></div>
      {actions(selected).length ? <section><h3>Available action</h3><div className="form-field"><label htmlFor="order-next">Move order to</label><select id="order-next" value={nextStatus} onChange={(event) => setNextStatus(event.target.value)}><option value="">Choose an action</option>{actions(selected).map((action) => <option key={action} value={action}>{action.replaceAll("_", " ")}</option>)}</select></div>{nextStatus === "REJECTED" ? <label className="form-field">Rejection reason<textarea value={rejectionReason} onChange={(event) => setRejectionReason(event.target.value)} required /></label> : null}<label className="form-field">Processing or delivery remarks<textarea value={remarks} onChange={(event) => setRemarks(event.target.value)} /></label><Button variant="outline" disabled={busy || !nextStatus} onClick={() => void update()}>Update order</Button></section> : null}
      <h3>Status history</h3><ol>{history.map((row, index) => <li key={`${row.createdAt}-${index}`}>{new Date(row.createdAt).toLocaleString()} · {row.previousStatus ?? "Created"} → {row.newStatus}{row.remarks ? ` · ${row.remarks}` : ""}</li>)}</ol>
    </Panel> : null}
  </div></main>;
}
