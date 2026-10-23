"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import type { UserProfile } from "@/app/lib/auth";
import { resources } from "@/app/lib/resources";

export default function Navigation() {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [logoutError, setLogoutError] = useState("");
  const isDashboard = pathname.startsWith("/dashboard");
  const isAdmin =
    user?.role.toUpperCase() === "ADMIN" ||
    user?.role.toUpperCase() === "ROLE_ADMIN";
  const visibleResources = resources.filter(
    (resource) =>
      user?.navigationItems?.includes(resource.key) &&
      (!resource.adminOnly || isAdmin),
  );

  useEffect(() => {
    let active = true;
    fetch("/api/auth/me", { cache: "no-store" })
      .then(async (response) => {
        if (response.ok) {
          const result = (await response.json()) as UserProfile;
          if (active) setUser(result);
        } else if (active) {
          setUser(null);
        }
      })
      .catch(() => {
        if (active) setLogoutError("Could not confirm your session.");
      });
    return () => {
      active = false;
    };
  }, [pathname]);

  async function signOut() {
    setLogoutError("");
    try {
      const response = await fetch("/api/auth/logout", { method: "POST" });
      if (!response.ok) throw new Error("Could not sign out.");
      setUser(null);
      router.push("/");
      router.refresh();
    } catch (error) {
      setLogoutError(error instanceof Error ? error.message : "Could not sign out.");
    }
  }

  return (
    <header className={`site-header${isDashboard ? " dashboard-header" : ""}`}>
      <nav className="navigation" aria-label="Main navigation">
        <Link className="brand" href="/" aria-label="Robsen home">
          <span className="brand-mark" aria-hidden="true">
            r
          </span>
          <span>robsen</span>
        </Link>

        {isDashboard ? (
          <div className="navigation-links dashboard-navigation-links">
            <Link href="/dashboard">Overview</Link>
            {visibleResources.map((resource) => (
              <Link href={`/dashboard/${resource.key}`} key={resource.key}>
                {resource.label}
              </Link>
            ))}
          </div>
        ) : (
          <div className="navigation-links">
            <a href="#platform">Platform</a>
            <a href="#businesses">For businesses</a>
            <a href="#products">Products</a>
          </div>
        )}

        {user ? (
          <div className="navigation-account">
            <span className="navigation-user">{user.fullName || user.email}</span>
            <button className="navigation-cta" onClick={signOut} type="button">
              Sign out <span aria-hidden="true">↗</span>
            </button>
          </div>
        ) : (
          <Link className="navigation-cta" href="/#login">
            Sign in <span aria-hidden="true">↗</span>
          </Link>
        )}
      </nav>
      {logoutError ? <p className="navigation-error">{logoutError}</p> : null}
    </header>
  );
}
