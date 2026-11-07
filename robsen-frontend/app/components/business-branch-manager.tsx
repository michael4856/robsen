"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { apiFetch } from "@/app/lib/api-client";
import LocationMapLink from "@/app/components/location-map-link";

type Branch = {
  id: number;
  name: string;
  address: string;
  city?: string | null;
  region?: string | null;
  phoneNumber?: string | null;
  email?: string | null;
};

type BranchForm = Omit<Branch, "id">;

const emptyBranch: BranchForm = {
  name: "",
  address: "",
  city: "",
  region: "",
  phoneNumber: "",
  email: "",
};

export default function BusinessBranchManager({
  businessId,
  canManage,
}: {
  businessId: number;
  canManage: boolean;
}) {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [selected, setSelected] = useState<Branch | null>(null);
  const [values, setValues] = useState<BranchForm>(emptyBranch);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [error, setError] = useState("");

  const reload = useCallback(async () => {
    try {
      const result = await apiFetch<Branch[]>(`business/${businessId}/branches`);
      setBranches(result);
      setError("");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not load business branches.");
    } finally {
      setIsLoading(false);
    }
  }, [businessId]);

  useEffect(() => {
    let active = true;
    apiFetch<Branch[]>(`business/${businessId}/branches`)
      .then((result) => {
        if (!active) return;
        setBranches(result);
        setError("");
      })
      .catch((reason: unknown) => {
        if (!active) return;
        setError(reason instanceof Error ? reason.message : "Could not load business branches.");
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => { active = false; };
  }, [businessId]);

  function startCreate() {
    setSelected(null);
    setValues(emptyBranch);
    setError("");
    setIsFormOpen(true);
  }

  function startEdit(branch: Branch) {
    setSelected(branch);
    setValues({
      name: branch.name,
      address: branch.address,
      city: branch.city ?? "",
      region: branch.region ?? "",
      phoneNumber: branch.phoneNumber ?? "",
      email: branch.email ?? "",
    });
    setError("");
    setIsFormOpen(true);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    setError("");
    try {
      await apiFetch(
        selected
          ? `business/${businessId}/branches/${selected.id}`
          : `business/${businessId}/branches`,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify(values),
        },
      );
      setIsFormOpen(false);
      setSelected(null);
      await reload();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not save this branch.");
    } finally {
      setIsSaving(false);
    }
  }

  async function deactivate(branch: Branch) {
    if (!window.confirm(`Deactivate ${branch.name}?`)) return;
    setError("");
    try {
      await apiFetch(`business/${businessId}/branches/${branch.id}`, { method: "DELETE" });
      await reload();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not deactivate this branch.");
    }
  }

  return (
    <section aria-label="Business branches" className="business-branches editor-panel">
      <div className="editor-heading">
        <div>
          <p className="dashboard-eyebrow">BUSINESS LOCATIONS</p>
          <h2>Branches</h2>
        </div>
        {canManage ? <button className="primary-button" onClick={startCreate} type="button">Add branch</button> : null}
      </div>

      {error ? <p className="dashboard-alert" role="alert">{error}</p> : null}

      {isFormOpen ? (
        <form className="record-form" onSubmit={(event) => void save(event)}>
          <label className="field-control"><span>Branch name *</span><input maxLength={255} onChange={(event) => setValues((current) => ({ ...current, name: event.target.value }))} required value={values.name} /></label>
          <div className="field-control"><label htmlFor="branch-address">Address *</label><input id="branch-address" maxLength={500} onChange={(event) => setValues((current) => ({ ...current, address: event.target.value }))} required value={values.address} />{values.address ? <LocationMapLink location={[values.address, values.city, values.region].filter(Boolean).join(", ")} /> : null}</div>
          <label className="field-control"><span>City</span><input maxLength={120} onChange={(event) => setValues((current) => ({ ...current, city: event.target.value }))} value={values.city ?? ""} /></label>
          <label className="field-control"><span>Region</span><input maxLength={120} onChange={(event) => setValues((current) => ({ ...current, region: event.target.value }))} value={values.region ?? ""} /></label>
          <label className="field-control"><span>Phone</span><input maxLength={50} onChange={(event) => setValues((current) => ({ ...current, phoneNumber: event.target.value }))} value={values.phoneNumber ?? ""} /></label>
          <label className="field-control"><span>Email</span><input maxLength={255} onChange={(event) => setValues((current) => ({ ...current, email: event.target.value }))} type="email" value={values.email ?? ""} /></label>
          <div className="form-actions">
            <button className="secondary-button" onClick={() => setIsFormOpen(false)} type="button">Cancel</button>
            <button className="primary-button" disabled={isSaving} type="submit">{isSaving ? "Saving…" : selected ? "Save changes" : "Create branch"}</button>
          </div>
        </form>
      ) : null}

      {isLoading ? <p>Loading branches…</p> : branches.length ? (
        <div className="business-branch-list">
          {branches.map((branch) => (
            <article className="business-branch-card" key={branch.id}>
              <div>
                <h3>{branch.name}</h3>
                <p>{[branch.address, branch.city, branch.region].filter(Boolean).join(", ")}</p>
                <LocationMapLink location={[branch.address, branch.city, branch.region].filter(Boolean).join(", ")} />
                {branch.phoneNumber ? <p>{branch.phoneNumber}</p> : null}
                {branch.email ? <p>{branch.email}</p> : null}
              </div>
              {canManage ? <div className="row-actions"><button onClick={() => startEdit(branch)} type="button">Edit</button><button className="danger-text" onClick={() => void deactivate(branch)} type="button">Deactivate</button></div> : null}
            </article>
          ))}
        </div>
      ) : <p>No branches have been added yet.</p>}
    </section>
  );
}
