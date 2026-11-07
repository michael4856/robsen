"use client";

import { useEffect, useState, type FormEvent } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { Alert, Button, Panel, StatusBadge } from "@/app/components/ui";
import { apiFetch, apiUpload } from "@/app/lib/api-client";
import { ACTIVE_BUSINESS_STORAGE_KEY } from "@/app/lib/auth";

type Business = { id: number; legalName: string; membershipRole: string };
type DocumentRow = { id: number; documentType: string; originalFileName: string; status: string; reviewComment?: string | null };
type Review = { previousStatus?: string | null; newStatus: string; reviewerUserId: number; reason?: string | null; notes?: string | null; reviewedAt: string };
type Verification = { id: number | null; businessId: number; businessName: string; status: string; submittedAt?: string | null; rejectionReason?: string | null; reviewNotes?: string | null; documents: DocumentRow[]; history: Review[] };

export default function VerificationPage() {
  return <ProtectedArea><VerificationContent /></ProtectedArea>;
}

function VerificationContent() {
  const profile = useAuthProfile();
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [businessId, setBusinessId] = useState("");
  const [verification, setVerification] = useState<Verification | null>(null);
  const [type, setType] = useState("BUSINESS_LICENSE");
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!profile || profile.superAdmin) return;
    apiFetch<Business[]>("business/my").then((rows) => {
      const owners = rows.filter((row) => row.membershipRole?.toUpperCase() === "OWNER");
      setBusinesses(owners);
      const stored = window.sessionStorage.getItem(ACTIVE_BUSINESS_STORAGE_KEY);
      const selected = owners.find((row) => String(row.id) === stored) ?? (owners.length === 1 ? owners[0] : null);
      if (selected) setBusinessId(String(selected.id));
    }).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Could not load businesses."));
  }, [profile]);

  useEffect(() => {
    if (!businessId) return;
    apiFetch<Verification>(`business-verifications/business/${businessId}`)
      .then(setVerification).catch((reason: unknown) => setError(reason instanceof Error ? reason.message : "Could not load verification."));
  }, [businessId]);

  async function upload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file || !businessId) return;
    setBusy(true); setError(""); setNotice("");
    const data = new FormData(); data.set("type", type); data.set("file", file);
    try {
      await apiUpload<DocumentRow>(`business-verifications/business/${businessId}/documents`, data);
      setFile(null); event.currentTarget.reset();
      setNotice("Document uploaded securely.");
      setVerification(await apiFetch<Verification>(`business-verifications/business/${businessId}`));
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Upload failed."); }
    finally { setBusy(false); }
  }

  async function submit() {
    if (!businessId) return;
    setBusy(true); setError(""); setNotice("");
    try { setVerification(await apiFetch<Verification>(`business-verifications/business/${businessId}/submit`, { method: "POST" })); setNotice("Verification submitted for platform review."); }
    catch (reason) { setError(reason instanceof Error ? reason.message : "Could not submit verification."); }
    finally { setBusy(false); }
  }

  return <main className="dashboard-page"><div className="dashboard-content">
    <header className="dashboard-heading"><div><p className="dashboard-eyebrow">BUSINESS TRUST</p><h1>Business verification</h1><p className="dashboard-subtitle">Submit business records for platform review and track the decision.</p></div></header>
    {profile?.superAdmin ? <Alert tone="warning">Review requests from Administration → Business verification.</Alert> : null}
    {error ? <Alert tone="danger">{error}</Alert> : null}{notice ? <Alert tone="success">{notice}</Alert> : null}
    {businesses.length > 1 ? <label className="form-field">Business<select value={businessId} onChange={(event) => setBusinessId(event.target.value)}><option value="">Choose a business</option>{businesses.map((row) => <option key={row.id} value={row.id}>{row.legalName}</option>)}</select></label> : null}
    {verification ? <>
      <Panel><div className="dashboard-heading"><div><h2>{verification.businessName}</h2><p>Current status</p></div><StatusBadge tone={verification.status === "APPROVED" ? "success" : verification.status === "REJECTED" ? "danger" : "warning"}>{verification.status.replaceAll("_", " ")}</StatusBadge></div>
        {verification.rejectionReason ? <Alert tone="danger">Reason: {verification.rejectionReason}</Alert> : null}{verification.reviewNotes ? <p>{verification.reviewNotes}</p> : null}
        {verification.history.length ? <ol>{verification.history.map((review, index) => <li key={`${review.reviewedAt}-${index}`}>{new Date(review.reviewedAt).toLocaleString()} · {review.previousStatus ?? "Submitted"} → {review.newStatus}{review.reason ? ` · ${review.reason}` : ""}{review.notes ? ` · ${review.notes}` : ""}</li>)}</ol> : null}
      </Panel>
      <Panel><h2>Required documents</h2><p>Upload a business license and tax identification record. PDF, JPEG, or PNG files up to 10 MB are accepted.</p>
        <form className="onboarding-form" onSubmit={upload}><label className="form-field">Document type<select value={type} onChange={(event) => setType(event.target.value)}><option value="BUSINESS_LICENSE">Business license</option><option value="TAX_IDENTIFICATION">Tax identification</option><option value="IMPORT_LICENSE">Import license</option><option value="OTHER">Other</option></select></label><label className="form-field">File<input type="file" accept="application/pdf,image/jpeg,image/png" onChange={(event) => setFile(event.target.files?.[0] ?? null)} required /></label><Button disabled={busy || !file}>Upload document</Button></form>
        <ul>{verification.documents.map((document) => <li key={document.id}><a href={`/api/backend/business-verifications/documents/${document.id}`} target="_blank" rel="noreferrer">{document.documentType.replaceAll("_", " ")} · {document.originalFileName}</a> — {document.status.replaceAll("_", " ")}{document.reviewComment ? ` · ${document.reviewComment}` : ""}</li>)}</ul>
        <Button disabled={busy || verification.status === "PENDING" || verification.status === "UNDER_REVIEW" || !verification.documents.some((doc) => doc.documentType === "BUSINESS_LICENSE") || !verification.documents.some((doc) => doc.documentType === "TAX_IDENTIFICATION")} onClick={submit}>Submit for review</Button>
      </Panel>
    </> : !profile?.superAdmin && businessId ? <Panel><p>Loading verification details…</p></Panel> : null}
    {!profile?.superAdmin && businesses.length === 0 ? <Alert tone="warning">Only a business owner can submit verification. Select an owned business to continue.</Alert> : null}
  </div></main>;
}
