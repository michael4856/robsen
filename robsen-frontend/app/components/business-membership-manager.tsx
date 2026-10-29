"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";

type BusinessOption = { id: number; legalName: string; tradeName?: string | null };
type UserOption = { id: number; fullName?: string | null; email?: string | null; username?: string | null };
type RoleOption = { id: number; name: string };
type Membership = {
  id: number;
  user: UserOption;
  business: BusinessOption;
  role: RoleOption;
  status: "INVITED" | "ACTIVE" | "SUSPENDED" | "REMOVED";
  permissionGrants?: string[];
};
type ManagerPermissionConfig = {
  businessId: number;
  roleName: string;
  recommendedDefaults: string[];
  currentPermissions: string[];
  effectivePermissions: string[];
  permissions: Array<{ key: string; label: string; description: string; defaultEnabled: boolean; enabled: boolean }>;
  defaultsVersion: number;
};
const NO_BUSINESS_IDS: number[] = [];

export default function BusinessMembershipManager() {
  return <ProtectedArea><MembershipContent /></ProtectedArea>;
}

function MembershipContent() {
  const profile = useAuthProfile();
  const businessIds = profile?.businessIds ?? NO_BUSINESS_IDS;
  const [businesses, setBusinesses] = useState<BusinessOption[]>([]);
  const [businessId, setBusinessId] = useState("");
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [roles, setRoles] = useState<RoleOption[]>([]);
  const [permissionCatalog, setPermissionCatalog] = useState<{ businessId: string; permissions: string[] } | null>(null);
  const [savingPermissions, setSavingPermissions] = useState(false);
  const [query, setQuery] = useState("");
  const [users, setUsers] = useState<UserOption[]>([]);
  const [selectedUser, setSelectedUser] = useState("");
  const [selectedRole, setSelectedRole] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [managerPermissionDirty, setManagerPermissionDirty] = useState(false);

  useEffect(() => {
    if (!profile?.superAdmin && !businessIds.length) return;
    let current = true;
    const businessRequest = profile?.superAdmin
      ? apiFetch<BusinessOption[]>("business/all")
      : Promise.all(businessIds.map((id) => apiFetch<BusinessOption>(`business/${id}`)));
    businessRequest
      .then((result) => {
        if (!current) return;
        setBusinesses(result);
        setBusinessId((existing) => existing || String(result[0]?.id ?? ""));
      })
      .catch((requestError: unknown) => setError(requestError instanceof Error ? requestError.message : "Could not load businesses."))
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [businessIds, profile?.superAdmin]);

  useEffect(() => {
    if (!businessId) return;
    let current = true;
    Promise.all([
      apiFetch<Membership[]>(`business-memberships/business/${businessId}`),
      apiFetch<RoleOption[]>(`business-memberships/assignable-roles?businessId=${businessId}`),
    ])
      .then(([memberRows, roleOptions]) => {
        if (!current) return;
        setMemberships(memberRows);
        setRoles(roleOptions);
        setSelectedRole((value) => value || String(roleOptions[0]?.id ?? ""));
      })
      .catch((requestError: unknown) => setError(requestError instanceof Error ? requestError.message : "Could not load memberships."))
      .finally(() => { if (current) setLoading(false); });
    apiFetch<string[]>(`business-memberships/permission-catalog?businessId=${businessId}`)
      .then((permissions) => { if (current) setPermissionCatalog({ businessId, permissions }); })
      .catch(() => { if (current) setPermissionCatalog({ businessId, permissions: [] }); });
    return () => { current = false; };
  }, [businessId]);

  async function searchUsers(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    try {
      const result = await apiFetch<UserOption[]>(`business-memberships/available-users?businessId=${businessId}&query=${encodeURIComponent(query)}`);
      setUsers(result);
      setSelectedUser(String(result[0]?.id ?? ""));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not search users.");
    }
  }

  async function invite(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");
    try {
      await apiFetch("business-memberships", {
        method: "POST",
        body: JSON.stringify({
          business: { id: Number(businessId) },
          user: { id: Number(selectedUser) },
          role: { id: Number(selectedRole) },
        }),
      });
      setNotice("Invitation created. The user can accept it from their workspace.");
      setUsers([]);
      setQuery("");
      await reloadMemberships();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not create invitation.");
    } finally {
      setSaving(false);
    }
  }

  async function reloadMemberships() {
    const rows = await apiFetch<Membership[]>(`business-memberships/business/${businessId}`);
    setMemberships(rows);
  }

  async function updateStatus(membership: Membership, status: Membership["status"]) {
    setError("");
    setNotice("");
    try {
      await apiFetch(`business-memberships/${membership.id}`, {
        method: "PUT",
        body: JSON.stringify({ role: { id: membership.role.id }, status }),
      });
      setNotice(`Membership ${status.toLowerCase()}.`);
      await reloadMemberships();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not update membership.");
    }
  }

  async function savePermissions(membership: Membership) {
    setSavingPermissions(true);
    setError("");
    setNotice("");
    try {
      await apiFetch(`business-memberships/${membership.id}/permissions`, {
        method: "PUT",
        body: JSON.stringify({ permissions: membership.permissionGrants ?? [] }),
      });
      setNotice(`Extra permissions updated for ${membership.user.fullName || membership.user.username}.`);
      await reloadMemberships();
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not update permissions.");
    } finally {
      setSavingPermissions(false);
    }
  }

  const selectedBusiness = businesses.find((business) => String(business.id) === businessId);

  return (
    <main className="dashboard-page">
      <div className="dashboard-content membership-content">
        <div className="breadcrumb"><Link href="/dashboard">Workspace</Link><span aria-hidden="true">/</span><span>Business memberships</span></div>
        <div className="list-heading"><div><p className="dashboard-eyebrow">BUSINESS TEAM</p><h1>Members and invitations</h1><p className="dashboard-subtitle">Invite registered Robsen users and manage access to your business.</p></div></div>
        {error ? <div className="dashboard-alert" role="alert">{error}</div> : null}
        {notice ? <div className="dashboard-success" role="status">{notice}</div> : null}
        {!profile?.superAdmin && !businessIds.length ? <section className="dashboard-empty-state"><h2>No business membership</h2><p>Create a business or accept an invitation to manage its team.</p><Link className="text-button" href="/dashboard">Go to workspace</Link></section> : (
          <>
            {businesses.length > 1 ? <label className="business-selector"><span>Business</span><select value={businessId} onChange={(event) => { if (managerPermissionDirty && !window.confirm("Discard unsaved manager permission changes?")) return; setManagerPermissionDirty(false); setLoading(true); setBusinessId(event.target.value); setUsers([]); setError(""); }}><option value="">Choose a business</option>{businesses.map((business) => <option key={business.id} value={business.id}>{business.tradeName || business.legalName}</option>)}</select></label> : null}
            <section className="editor-panel"><div className="editor-heading"><div><p className="dashboard-eyebrow">ADD A TEAM MEMBER</p><h2>{selectedBusiness?.tradeName || selectedBusiness?.legalName || "Your business"}</h2></div></div>
              <form className="record-form membership-search-form" onSubmit={searchUsers}>
                <label className="field-control"><span>Search by name, email, or username</span><input minLength={2} onChange={(event) => setQuery(event.target.value)} required value={query} /></label>
                <div className="form-actions"><button className="secondary-button" type="submit">Search users</button></div>
              </form>
              {users.length ? <form className="record-form membership-invite-form" onSubmit={invite}>
                <label className="field-control"><span>Existing user</span><select onChange={(event) => setSelectedUser(event.target.value)} required value={selectedUser}>{users.map((user) => <option key={user.id} value={user.id}>{user.fullName || user.username} · {user.email || user.username}</option>)}</select></label>
                <label className="field-control"><span>Business role</span><select onChange={(event) => setSelectedRole(event.target.value)} required value={selectedRole}>{roles.map((role) => <option key={role.id} value={role.id}>{role.name.replaceAll("_", " ")}</option>)}</select></label>
                <div className="form-actions"><button className="primary-button" disabled={saving} type="submit">{saving ? "Sending…" : "Invite member"}</button></div>
              </form> : null}
            </section>
            {profile?.superAdmin || memberships.some((membership) => membership.user.id === profile?.id
              && membership.status === "ACTIVE" && membership.role.name.toUpperCase() === "OWNER") ? (
              <ManagerPermissionEditor businessId={Number(businessId)} businessName={selectedBusiness?.tradeName || selectedBusiness?.legalName || "this business"} onDirtyChange={setManagerPermissionDirty} />
            ) : null}
            <section aria-label="Business members" className="records-panel"><div className="records-toolbar"><div><h2>{selectedBusiness?.tradeName || selectedBusiness?.legalName || "Members"}</h2><span>{loading ? "Loading…" : `${memberships.length} members and invitations`}</span></div></div>
              {loading ? <div className="empty-state">Loading memberships…</div> : memberships.length === 0 ? <div className="empty-state">No members yet. Search for a Robsen user to invite them.</div> : <div className="table-scroll"><table className="records-table"><thead><tr><th>USER</th><th>ROLE</th><th>STATUS</th><th>ACTIONS</th></tr></thead><tbody>
                {memberships.map((membership) => <MembershipRow
                  key={membership.id}
                  membership={membership}
                  permissionCatalog={permissionCatalog?.businessId === businessId ? permissionCatalog.permissions : null}
                  onSavePermissions={savePermissions}
                  onUpdateStatus={updateStatus}
                  savingPermissions={savingPermissions}
                />)}
              </tbody></table></div>}
            </section>
          </>
        )}
      </div>
    </main>
  );
}

function ManagerPermissionEditor({ businessId, businessName, onDirtyChange }: { businessId: number; businessName: string; onDirtyChange: (dirty: boolean) => void }) {
  const [configuration, setConfiguration] = useState<ManagerPermissionConfig | null>(null);
  const [selected, setSelected] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [dirty, setDirty] = useState(false);
  const endpoint = `business/${businessId}/roles/MANAGER/permissions`;

  useEffect(() => {
    let current = true;
    apiFetch<ManagerPermissionConfig>(endpoint)
      .then((result) => {
        if (!current) return;
        setConfiguration(result);
        setSelected(result.currentPermissions);
        setDirty(false);
        onDirtyChange(false);
      })
      .catch((requestError: unknown) => setError(requestError instanceof Error ? requestError.message : "Could not load manager permissions."))
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [endpoint, onDirtyChange]);

  useEffect(() => {
    if (!dirty) return;
    const preventUnload = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ""; };
    window.addEventListener("beforeunload", preventUnload);
    return () => window.removeEventListener("beforeunload", preventUnload);
  }, [dirty]);

  async function savePermissions(next = selected) {
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const updated = await apiFetch<ManagerPermissionConfig>(endpoint, {
        method: "PUT",
        body: JSON.stringify({ permissions: next }),
      });
      setConfiguration(updated);
      setSelected(updated.currentPermissions);
      setDirty(false);
      onDirtyChange(false);
      setNotice("Manager permissions saved. Changes now apply to active managers in this business.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not save manager permissions.");
    } finally {
      setSaving(false);
    }
  }

  async function resetDefaults() {
    if (!window.confirm("Restore the recommended manager permissions for this business?")) return;
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const updated = await apiFetch<ManagerPermissionConfig>(`${endpoint}/reset`, { method: "POST" });
      setConfiguration(updated);
      setSelected(updated.currentPermissions);
      setDirty(false);
      onDirtyChange(false);
      setNotice("Recommended manager permissions restored.");
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not reset manager permissions.");
    } finally {
      setSaving(false);
    }
  }

  const groups = (configuration?.permissions ?? []).reduce<Record<string, ManagerPermissionConfig["permissions"]>>((result, permission) => {
    const group = permission.key.split(":")[0];
    (result[group] ??= []).push(permission);
    return result;
  }, {});
  const groupLabels: Record<string, string> = {
    businesses: "Dashboard and reports", products: "Products", "product-categories": "Product categories",
    "product-listings": "Product listings", orders: "Orders", "order-items": "Order operations",
    opportunities: "Distribution opportunities", "partner-applications": "Partner applications",
    agreements: "Agreements", territories: "Territories",
  };

  return <section className="editor-panel manager-permission-settings">
    <div className="editor-heading"><div><p className="dashboard-eyebrow">ROLES & PERMISSIONS</p><h2>Manager permissions</h2><p className="dashboard-subtitle">Changes apply to every active MANAGER in {businessName}. Owners retain protected ownership controls.</p></div></div>
    {error ? <div className="dashboard-alert" role="alert">{error}</div> : null}
    {notice ? <div className="dashboard-success" role="status">{notice}</div> : null}
    {loading ? <div className="empty-state">Loading manager permissions…</div> : configuration ? <>
      <p className="permission-help">{configuration.effectivePermissions.length} permissions are currently effective for MANAGER memberships.</p>
      <div className="membership-permission-groups">
        {Object.entries(groups).map(([resource, permissions]) => <fieldset key={resource}>
          <legend>{groupLabels[resource] ?? resource.replaceAll("-", " ")}</legend>
          {permissions.map((permission) => <label key={permission.key} title={permission.description}>
            <input checked={selected.includes(permission.key)} onChange={(event) => {
              setSelected((current) => event.target.checked
                ? [...current, permission.key]
                : current.filter((item) => item !== permission.key));
              setDirty(true);
              onDirtyChange(true);
            }} type="checkbox" />
            <span>{permission.label}{permission.defaultEnabled ? " · Recommended" : ""}</span>
          </label>)}
        </fieldset>)}
      </div>
      <div className="form-actions">
        <button className="secondary-button" disabled={saving} onClick={() => void resetDefaults()} type="button">Reset to defaults</button>
        <button className="primary-button" disabled={saving || !dirty} onClick={() => void savePermissions()} type="button">{saving ? "Saving…" : "Save changes"}</button>
      </div>
    </> : <div className="empty-state">Manager permissions are unavailable for this business.</div>}
  </section>;
}

function MembershipRow({
  membership,
  permissionCatalog,
  onSavePermissions,
  onUpdateStatus,
  savingPermissions,
}: {
  membership: Membership;
  permissionCatalog: string[] | null;
  onSavePermissions: (membership: Membership) => Promise<void>;
  onUpdateStatus: (membership: Membership, status: Membership["status"]) => Promise<void>;
  savingPermissions: boolean;
}) {
  const [editing, setEditing] = useState(false);
  const [selected, setSelected] = useState<string[]>(membership.permissionGrants ?? []);
  const isOwner = membership.role.name.toUpperCase() === "OWNER";
  const canEditPermissions = Boolean(permissionCatalog?.length)
    && !isOwner && membership.status !== "SUSPENDED" && membership.status !== "REMOVED";
  const grouped = (permissionCatalog ?? []).reduce<Record<string, string[]>>((groups, permission) => {
    const resource = permission.split(":")[0];
    (groups[resource] ??= []).push(permission);
    return groups;
  }, {});

  return <>
    <tr>
      <td>{membership.user.fullName || membership.user.username}<br /><span className="membership-email">{membership.user.email}</span></td>
      <td>{membership.role.name.replaceAll("_", " ")}</td>
      <td>{membership.status}<br />{membership.permissionGrants?.length ? <span className="membership-email">{membership.permissionGrants.length} extra permissions</span> : null}</td>
      <td className="membership-actions">
        {canEditPermissions ? <button className="text-button" onClick={() => setEditing((value) => !value)} type="button">{editing ? "Close permissions" : "Permissions"}</button> : null}
        {!isOwner && membership.status === "ACTIVE" ? <button className="text-button" onClick={() => void onUpdateStatus(membership, "SUSPENDED")} type="button">Suspend</button> : null}
        {!isOwner && membership.status === "SUSPENDED" ? <button className="text-button" onClick={() => void onUpdateStatus(membership, "ACTIVE")} type="button">Reactivate</button> : null}
        {!isOwner && membership.status !== "REMOVED" ? <button className="text-button danger-text" onClick={() => void onUpdateStatus(membership, "REMOVED")} type="button">Remove</button> : null}
      </td>
    </tr>
    {editing && canEditPermissions ? <tr><td colSpan={4}>
      <section className="membership-permission-editor" aria-label={`Extra permissions for ${membership.user.fullName || membership.user.username}`}>
        <div><h3>Extra permissions</h3><p>The member keeps the permissions included in their role. These grants add individual actions for this business.</p></div>
        <div className="membership-permission-groups">{Object.entries(grouped).map(([resource, permissions]) => <fieldset key={resource}>
          <legend>{resource.replaceAll("-", " ")}</legend>
          {permissions.map((permission) => <label key={permission}><input checked={selected.includes(permission)} onChange={(event) => setSelected((current) => event.target.checked ? [...current, permission] : current.filter((item) => item !== permission))} type="checkbox" /><span>{permission.split(":")[1].toLowerCase()}</span></label>)}
        </fieldset>)}</div>
        <div className="form-actions"><button className="secondary-button" onClick={() => { setSelected(membership.permissionGrants ?? []); setEditing(false); }} type="button">Cancel</button><button className="primary-button" disabled={savingPermissions} onClick={() => void onSavePermissions({ ...membership, permissionGrants: selected })} type="button">{savingPermissions ? "Saving…" : "Save permissions"}</button></div>
      </section>
    </td></tr> : null}
  </>;
}
