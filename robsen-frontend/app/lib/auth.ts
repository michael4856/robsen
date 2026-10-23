export const ACCESS_TOKEN_COOKIE = "robsen_access_token";

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
