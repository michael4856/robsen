import { cookies } from "next/headers";
import { ACCESS_TOKEN_COOKIE } from "@/app/lib/auth";
import { forwardBackendResponse, isSameOriginRequest } from "@/app/lib/backend";

type RouteContext = {
  params: Promise<{ path: string[] }>;
};

async function proxy(request: Request, context: RouteContext) {
  if (!isSameOriginRequest(request)) {
    return Response.json({ message: "Cross-origin requests are not allowed." }, { status: 403 });
  }
  const requestUrl = new URL(request.url);

  const cookieStore = await cookies();
  const token = cookieStore.get(ACCESS_TOKEN_COOKIE)?.value;
  if (!token) {
    return Response.json(
      { code: "AUTH_REQUIRED", message: "Sign in to continue." },
      { status: 401 },
    );
  }

  const { path } = await context.params;
  const backendPath = path.map((segment) => encodeURIComponent(segment)).join("/");
  const body = ["GET", "HEAD"].includes(request.method) ? undefined : await request.arrayBuffer();
  const response = await forwardBackendResponse(
    `/api/${backendPath}${requestUrl.search}`,
    {
      method: request.method,
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: request.headers.get("accept") ?? "application/json",
        ...(request.headers.get("content-type")
          ? { "Content-Type": request.headers.get("content-type")! }
          : {}),
      },
      body,
    },
  );

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

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
