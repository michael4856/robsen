"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";
import { resources } from "@/app/lib/resources";

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
  const navigationItems = profile?.navigationItems;
  const visibleResources = resources.filter(
    (resource) =>
      navigationItems?.includes(resource.key) &&
      (!resource.adminOnly || profile?.superAdmin),
  );

  useEffect(() => {
    let active = true;
    Promise.all(
      resources
        .filter(
          (resource) =>
            navigationItems?.includes(resource.key) &&
            (!resource.adminOnly || profile?.superAdmin),
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
  }, [profile?.superAdmin, navigationItems]);

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
          <div className="dashboard-alert" role="alert">
            {error}
          </div>
        ) : null}

        <div className="resource-grid">
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
                {loading ? "…" : (counts[resource.key] ?? "—")}
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
        </div>
      </div>
    </main>
  );
}
