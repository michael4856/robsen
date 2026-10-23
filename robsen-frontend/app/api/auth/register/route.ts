import { forwardBackendResponse, isSameOriginRequest } from "@/app/lib/backend";

export async function POST(request: Request) {
  if (!isSameOriginRequest(request)) {
    return Response.json({ message: "Cross-origin requests are not allowed." }, { status: 403 });
  }

  let payload: string;
  try {
    payload = JSON.stringify(await request.json());
  } catch {
    return Response.json({ message: "A valid JSON request is required." }, { status: 400 });
  }

  return forwardBackendResponse("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: payload,
  });
}
