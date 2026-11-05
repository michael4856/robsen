import { cookies } from "next/headers";
import { ACCESS_TOKEN_COOKIE } from "@/app/lib/auth";
import { forwardBackendResponse } from "@/app/lib/backend";

export async function GET() {
  const cookieStore = await cookies();
  const token = cookieStore.get(ACCESS_TOKEN_COOKIE)?.value;
  if (!token) {
    return Response.json(
      { code: "AUTH_REQUIRED", message: "Sign in to continue." },
      { status: 401 },
    );
  }

  const response = await forwardBackendResponse("/api/auth/me", {
    headers: { Authorization: `Bearer ${token}`, Accept: "application/json" },
  });
  if (response.status === 401) {
    cookieStore.delete(ACCESS_TOKEN_COOKIE);
    const result = (await response.json().catch(() => null)) as
      | { code?: string; message?: string }
      | null;
    return Response.json(
      {
        code: result?.code ?? "SESSION_EXPIRED",
        message:
          result?.message ??
          "Your sign-in has expired. Please sign in again to continue.",
      },
      { status: 401 },
    );
  }
  return response;
}
