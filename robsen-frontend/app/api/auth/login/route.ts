import { cookies } from "next/headers";
import { ACCESS_TOKEN_COOKIE, type AuthResponse } from "@/app/lib/auth";
import { backendUrl, isSameOriginRequest } from "@/app/lib/backend";

export async function POST(request: Request) {
  if (!isSameOriginRequest(request)) {
    return Response.json({ message: "Cross-origin requests are not allowed." }, { status: 403 });
  }

  let payload: unknown;
  try {
    payload = await request.json();
  } catch {
    return Response.json({ message: "A valid JSON request is required." }, { status: 400 });
  }

  let response: Response;
  try {
    response = await fetch(backendUrl("/api/auth/login"), {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload),
      cache: "no-store",
    });
  } catch {
    return Response.json(
      { message: "Could not reach the Robsen API. Check that Spring is running." },
      { status: 502 },
    );
  }

  const result = (await response.json().catch(() => null)) as AuthResponse | null;
  if (!response.ok) {
    return Response.json(
      { message: result && "message" in result ? result.message : "Sign in failed." },
      { status: response.status },
    );
  }
  if (
    !result ||
    typeof result.accessToken !== "string" ||
    result.tokenType !== "Bearer" ||
    !Number.isFinite(result.expiresIn) ||
    result.expiresIn < 1 ||
    !result.user
  ) {
    return Response.json(
      { message: "The Robsen API returned an invalid sign-in response." },
      { status: 502 },
    );
  }

  const cookieStore = await cookies();
  cookieStore.set(ACCESS_TOKEN_COOKIE, result.accessToken, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: result.expiresIn,
  });

  return Response.json({ user: result.user });
}
