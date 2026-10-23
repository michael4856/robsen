"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";

const sections = [
  {
    key: "users",
    title: "Platform users",
    description: "Manage global accounts, platform access, and account status.",
    href: "/dashboard/users",
    listPath: "users/all",
  },
  {
    key: "roles",
    title: "Business roles",
    description: "Create custom roles and assign API-enforced resource permissions.",
    href: "/dashboard/roles",
    listPath: "roles/all",
  },
  {
    key: "memberships",
    title: "Business memberships",
    description: "Assign users to businesses and manage their business roles.",
    href: "/dashboard/memberships",
    listPath: "business-memberships",
  },
  {
    key: "businesses",
    title: "Businesses",
    description: "Review and manage all businesses on the platform.",
    href: "/dashboard/businesses",
    listPath: "business/all",
  },
  {
    key: "tenants",
    title: "Tenants",
    description: "Manage tenant workspaces and activation state.",
    href: "/dashboard/tenants",
    listPath: "tenants",
  },
];

export default function AdminPage() {
  return (
    <ProtectedArea>
      <AdminContent />
    </ProtectedArea>
  );
}

function AdminContent() {
  const profile = useAuthProfile();
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [error, setError] = useState("");

  useEffect(() => {
    if (!profile?.superAdmin) return;
    let active = true;
    Promise.all(
      sections.map(async ({ key, listPath }) => {
        const result = await apiFetch<unknown>(listPath);
        if (!Array.isArray(result)) throw new Error(`${key} endpoint did not return a list.`);
        return [key, result.length] as const;
      }),
    ).then((entries) => {
      if (active) setCounts(Object.fromEntries(entries));
    }).catch((loadError: unknown) => {
      if (active) {
        setError(loadError instanceof Error ? loadError.message : "Could not load administration data.");
      }
    });
    return () => {
      active = false;
    };
  }, [profile?.superAdmin]);

  if (!profile?.superAdmin) {
    return (
      <main className="dashboard-page">
        <div className="page-state error-state">
          <h1>Platform administrator access required</h1>
          <Link href="/dashboard">Back to workspace</Link>
        </div>
      </main>
    );
  }

  return (
    <main className="dashboard-page">
      <div className="dashboard-content">
        <div className="dashboard-heading">
          <div>
            <p className="dashboard-eyebrow">PLATFORM CONTROL</p>
            <h1>Administration</h1>
            <p className="dashboard-subtitle">
              Manage platform accounts, business access, roles, permissions, and workspaces.
            </p>
          </div>
          <span className="admin-badge admin-badge-large">SUPER ADMIN</span>
        </div>
        {error ? <div className="dashboard-alert" role="alert">{error}</div> : null}
        <div className="admin-grid">
          {sections.map((section) => (
            <Link className="admin-card" href={section.href} key={section.key}>
              <span className="admin-card-count">{counts[section.key] ?? "—"}</span>
              <h2>{section.title}</h2>
              <p>{section.description}</p>
              <span className="resource-card-link">Manage <span aria-hidden="true">↗</span></span>
            </Link>
          ))}
        </div>
        <section className="admin-note">
          <h2>Access model</h2>
          <p>
            Platform administrator access is separate from business membership roles.
            Business permissions are checked by the API against active memberships.
            Custom roles can only grant permissions from the supported resource catalog.
          </p>
        </section>
      </div>
    </main>
  );
}
