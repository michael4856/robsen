"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { notifySessionExpired, type UserProfile } from "@/app/lib/auth";
import { resources } from "@/app/lib/resources";
import { Alert, Button } from "./ui";

export default function Navigation() {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [logoutError, setLogoutError] = useState("");
  const isDashboard = pathname.startsWith("/dashboard");
  const visibleResources = resources.filter(
    (resource) =>
      user?.navigationItems?.includes(resource.key) &&
      (!resource.adminOnly || user?.superAdmin),
  );

  useEffect(() => {
    let active = true;
    const loadUser = () => {
      fetch("/api/auth/me", { cache: "no-store" })
        .then(async (response) => {
        if (response.ok) {
          const result = (await response.json()) as UserProfile;
          if (active) setUser(result);
        } else {
          const result = (await response.json().catch(() => null)) as
            | { code?: string; message?: string }
            | null;
          if (active) setUser(null);
          if (result?.code === "SESSION_EXPIRED") {
            notifySessionExpired(result.message);
          }
        }
        })
        .catch(() => {
          if (active) setLogoutError("Could not confirm your session.");
        });
    };
    loadUser();
    window.addEventListener("robsen:profile-updated", loadUser);
    return () => {
      active = false;
      window.removeEventListener("robsen:profile-updated", loadUser);
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
            <Link href="/dashboard/discover">Discover</Link>
            {user?.superAdmin ? <Link href="/dashboard/admin">Administration</Link> : null}
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
            {user.superAdmin ? <span className="admin-badge">SUPER ADMIN</span> : null}
            <Link className="navigation-profile-link" href="/dashboard/profile" aria-label="Manage your profile">
              <span className="navigation-avatar" style={user.profilePicture ? { backgroundImage: `url(${user.profilePicture})` } : undefined} aria-hidden="true">
                {user.profilePicture ? null : (user.fullName || user.email).slice(0, 1).toUpperCase()}
              </span>
              <span className="navigation-user">{user.fullName || user.email}</span>
            </Link>
            <Button className="navigation-cta" onClick={signOut} variant="secondary">
              Sign out <span aria-hidden="true">↗</span>
            </Button>
          </div>
        ) : (
          <Link className="navigation-cta" href="/#login">
            Sign in <span aria-hidden="true">↗</span>
          </Link>
        )}
      </nav>
      {logoutError ? <Alert tone="danger" className="navigation-error">{logoutError}</Alert> : null}
    </header>
  );
}
