import { cookies } from "next/headers";
import { ACCESS_TOKEN_COOKIE, type AuthResponse } from "@/app/lib/auth";
import { backendUrl, isSameOriginRequest } from "@/app/lib/backend";

export async function PATCH(request: Request) {
  if (!isSameOriginRequest(request)) {
    return Response.json({ message: "Cross-origin requests are not allowed." }, { status: 403 });
  }
  const cookieStore = await cookies();
  const token = cookieStore.get(ACCESS_TOKEN_COOKIE)?.value;
  if (!token) {
    return Response.json({ code: "AUTH_REQUIRED", message: "Sign in to continue." }, { status: 401 });
  }
  let response: Response;
  try {
    response = await fetch(backendUrl("/api/auth/profile"), {
      method: "PATCH",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json", Accept: "application/json" },
      body: await request.text(),
      cache: "no-store",
    });
  } catch {
    return Response.json({ message: "Could not reach the Robsen API. Check that Spring is running." }, { status: 502 });
  }
  const result = (await response.json().catch(() => null)) as AuthResponse | { message?: string } | null;
  if (!response.ok) {
    if (response.status === 401) {
      cookieStore.delete(ACCESS_TOKEN_COOKIE);
      return Response.json({
        code: "SESSION_EXPIRED",
        message: result && "message" in result ? result.message : "Your sign-in has expired. Please sign in again to continue.",
      }, { status: 401 });
    }
    return Response.json({ message: result && "message" in result ? result.message : "Could not update your profile." }, { status: response.status });
  }
  if (!result || !("accessToken" in result) || typeof result.accessToken !== "string"
      || !Number.isFinite(result.expiresIn) || result.expiresIn < 1 || !result.user) {
    return Response.json({ message: "The Robsen API returned an invalid profile response." }, { status: 502 });
  }
  cookieStore.set(ACCESS_TOKEN_COOKIE, result.accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: result.expiresIn,
  });
  return Response.json(result.user);
}
