import { cookies } from "next/headers";
import { ACCESS_TOKEN_COOKIE } from "@/app/lib/auth";
import { forwardBackendResponse } from "@/app/lib/backend";

export async function GET() {
  const cookieStore = await cookies();
  const token = cookieStore.get(ACCESS_TOKEN_COOKIE)?.value;
  if (!token) {
    return Response.json({ message: "You are not signed in." }, { status: 401 });
  }

  const response = await forwardBackendResponse("/api/auth/me", {
    headers: { Authorization: `Bearer ${token}`, Accept: "application/json" },
  });
  if (response.status === 401) {
    cookieStore.delete(ACCESS_TOKEN_COOKIE);
    return Response.json({ message: "Your session has expired. Sign in again." }, { status: 401 });
  }
  return response;
}
