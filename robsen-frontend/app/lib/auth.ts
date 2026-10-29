export const ACCESS_TOKEN_COOKIE = "robsen_access_token";
export const SESSION_EXPIRED_EVENT = "robsen:session-expired";
export const SESSION_NOTICE_KEY = "robsen-auth-notice";
export const ACTIVE_BUSINESS_STORAGE_KEY = "robsen-active-business";
export const SESSION_EXPIRED_MESSAGE =
  "Your sign-in has expired. Please sign in again to continue.";

export type SessionExpiredDetail = {
  message?: string;
};

let lastSessionRefresh = 0;
let sessionRefreshPromise: Promise<void> | null = null;

export function renewSessionIfDue() {
  const now = Date.now();
  if (lastSessionRefresh && now - lastSessionRefresh < 5 * 60 * 1000) {
    return sessionRefreshPromise ?? Promise.resolve();
  }

  lastSessionRefresh = now;
  sessionRefreshPromise = fetch("/api/auth/refresh", {
    method: "POST",
    cache: "no-store",
  })
    .then(async (response) => {
      if (response.status === 401) {
        const result = (await response.json().catch(() => null)) as
          | { message?: string }
          | null;
        notifySessionExpired(result?.message);
      }
    })
    .catch(() => undefined)
    .finally(() => {
      sessionRefreshPromise = null;
    });

  return sessionRefreshPromise;
}

export function notifySessionExpired(message = SESSION_EXPIRED_MESSAGE) {
  window.dispatchEvent(
    new CustomEvent<SessionExpiredDetail>(SESSION_EXPIRED_EVENT, {
      detail: { message },
    }),
  );
}

export type UserProfile = {
  id: number;
  fullName: string;
  email: string;
  username: string;
  role: string;
  phoneNumber: string | null;
  active: boolean;
  navigationItems: string[];
  permissions: string[];
  superAdmin: boolean;
  businessIds: number[];
  profilePicture?: string | null;
};

export function hasPermission(profile: UserProfile | null, permission: string) {
  return Boolean(
    profile?.superAdmin || profile?.permissions?.includes(permission),
  );
}

export type AuthResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: UserProfile;
};
