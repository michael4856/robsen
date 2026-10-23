"use client";

import { useRouter } from "next/navigation";
import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type { UserProfile } from "@/app/lib/auth";

const AuthProfileContext = createContext<UserProfile | null>(null);

export function useAuthProfile() {
  return useContext(AuthProfileContext);
}

export default function ProtectedArea({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [status, setStatus] = useState<"checking" | "ready" | "error">("checking");
  const [error, setError] = useState("");
  const [profile, setProfile] = useState<UserProfile | null>(null);

  useEffect(() => {
    let active = true;
    fetch("/api/auth/me", { cache: "no-store" })
      .then(async (response) => {
        if (response.status === 401) {
          router.replace("/");
          return;
        }
        if (!response.ok) {
          const result = (await response.json().catch(() => null)) as
            | { message?: string }
            | null;
          throw new Error(result?.message ?? "Could not verify your session.");
        }
        const result = (await response.json()) as UserProfile;
        if (
          !result ||
          typeof result.role !== "string" ||
          !Array.isArray(result.navigationItems) ||
          result.navigationItems.some((item) => typeof item !== "string")
        ) {
          throw new Error("The Robsen API returned an invalid user profile.");
        }
        if (active) setProfile(result);
        if (active) setStatus("ready");
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : "Could not verify your session.",
          );
          setStatus("error");
        }
      });
    return () => {
      active = false;
    };
  }, [router]);

  if (status === "checking") {
    return (
      <main className="dashboard-page">
        <div className="page-state">Checking your session…</div>
      </main>
    );
  }
  if (status === "error") {
    return (
      <main className="dashboard-page">
        <div className="page-state error-state">
          <h1>We couldn’t verify your session</h1>
          <p>{error}</p>
          <button onClick={() => window.location.reload()} type="button">
            Try again
          </button>
        </div>
      </main>
    );
  }
  return (
    <AuthProfileContext.Provider value={profile}>
      {children}
    </AuthProfileContext.Provider>
  );
}
