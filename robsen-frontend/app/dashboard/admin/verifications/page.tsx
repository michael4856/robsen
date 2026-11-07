"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { Alert, Button, Panel, StatusBadge } from "@/app/components/ui";
import { apiFetch } from "@/app/lib/api-client";

type DocumentRow = { id: number; documentType: string; originalFileName: string; status: string; reviewComment?: string | null };
type Review = { previousStatus?: string | null; newStatus: string; reviewerUserId: number; reviewedAt: string; reason?: string | null; notes?: string | null };
type Verification = { id: number; businessId: number; businessName: string; status: string; submittedAt?: string | null; reviewedAt?: string | null; reviewedByUserId?: number | null; rejectionReason?: string | null; reviewNotes?: string | null; documents: DocumentRow[]; history: Review[] };
type Page<T> = { content: T[]; totalElements: number };

export default function VerificationAdminPage() { return <ProtectedArea><AdminVerificationContent /></ProtectedArea>; }

function AdminVerificationContent() {
  const profile = useAuthProfile();
  const [rows, setRows] = useState<Verification[]>([]);
  const [selected, setSelected] = useState<Verification | null>(null);
  const [status, setStatus] = useState("");
  const [query, setQuery] = useState("");
  const [reason, setReason] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const load = useCallback(async () => {
    const params = new URLSearchParams({ page: "0", size: "100" });
    if (status) params.set("status", status); if (query.trim()) params.set("q", query.trim());
    try { const result = await apiFetch<Page<Verification>>(`business-verifications/admin?${params}`); setRows(result.content); }
    catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Could not load requests."); }
  }, [query, status]);
  useEffect(() => { if (profile?.superAdmin) void Promise.resolve().then(load); }, [profile?.superAdmin, load]);
  async function decide(decision: "APPROVED" | "REJECTED" | "RESUBMISSION_REQUIRED") {
    if (!selected) return;
    setError(""); setNotice("");
    try {
      const updated = await apiFetch<Verification>(`business-verifications/admin/${selected.id}/review`, { method: "POST", body: JSON.stringify({ status: decision, reason, notes }) });
      setSelected(updated); setNotice(`Business marked ${decision.toLowerCase().replaceAll("_", " ")}.`); setReason(""); setNotes(""); await load();
    } catch (requestError) { setError(requestError instanceof Error ? requestError.message : "Review failed."); }
  }
  if (!profile?.superAdmin) return <main className="dashboard-page"><div className="page-state error-state"><h1>Platform administrator access required</h1><Link href="/dashboard/admin">Back to administration</Link></div></main>;
  return <main className="dashboard-page"><div className="dashboard-content">
    <header className="dashboard-heading"><div><p className="dashboard-eyebrow">PLATFORM TRUST</p><h1>Business verification</h1><p className="dashboard-subtitle">Review supporting records and keep a clear record of platform decisions.</p></div></header>
    {error ? <Alert tone="danger">{error}</Alert> : null}{notice ? <Alert tone="success">{notice}</Alert> : null}
    <Panel><div className="table-toolbar"><input placeholder="Search business name" value={query} onChange={(event) => setQuery(event.target.value)} /><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All statuses</option>{["PENDING","UNDER_REVIEW","APPROVED","REJECTED","RESUBMISSION_REQUIRED"].map((value) => <option key={value}>{value}</option>)}</select><Button variant="secondary" onClick={() => void load()}>Search</Button></div>
      <div className="table-scroll"><table><thead><tr><th>Business</th><th>Status</th><th>Submitted</th><th>Files</th><th /></tr></thead><tbody>{rows.map((row) => <tr key={row.id}><td>{row.businessName}</td><td><StatusBadge>{row.status.replaceAll("_", " ")}</StatusBadge></td><td>{row.submittedAt ? new Date(row.submittedAt).toLocaleString() : "—"}</td><td>{row.documents.length}</td><td><Button variant="outline" onClick={() => setSelected(row)}>Review</Button></td></tr>)}{rows.length === 0 ? <tr><td colSpan={5}>No verification requests match these filters.</td></tr> : null}</tbody></table></div>
    </Panel>
    {selected ? <Panel><div className="dashboard-heading"><div><h2>{selected.businessName}</h2><p>Business ID {selected.businessId} · reviewer {selected.reviewedByUserId ?? "not reviewed"}</p></div><Button variant="quiet" onClick={() => setSelected(null)}>Close</Button></div>
      <ul>{selected.documents.map((document) => <li key={document.id}><a href={`/api/backend/business-verifications/documents/${document.id}`} target="_blank" rel="noreferrer">{document.documentType.replaceAll("_", " ")} · {document.originalFileName}</a> — {document.status.replaceAll("_", " ")}</li>)}</ul>
      {selected.rejectionReason ? <Alert tone="danger">Rejection: {selected.rejectionReason}</Alert> : null}
      {selected.history.length ? <ol>{selected.history.map((review, index) => <li key={`${review.reviewedAt}-${index}`}>{new Date(review.reviewedAt).toLocaleString()} · {review.previousStatus ?? "Submitted"} → {review.newStatus} · Reviewer {review.reviewerUserId}</li>)}</ol> : null}
      <label className="form-field">Rejection reason (required for rejection)<textarea value={reason} onChange={(event) => setReason(event.target.value)} /></label>
      <label className="form-field">Review notes<textarea value={notes} onChange={(event) => setNotes(event.target.value)} /></label>
      <div className="form-actions"><Button onClick={() => void decide("APPROVED")}>Approve</Button><Button variant="danger" disabled={!reason.trim()} onClick={() => void decide("REJECTED")}>Reject</Button><Button variant="secondary" onClick={() => void decide("RESUBMISSION_REQUIRED")}>Request resubmission</Button></div>
    </Panel> : null}
  </div></main>;
}
