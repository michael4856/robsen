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
};

export type AuthResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: UserProfile;
};
