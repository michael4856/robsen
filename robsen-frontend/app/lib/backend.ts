export function backendUrl(path: string) {
  const baseUrl = process.env.BACKEND_API_URL ?? "http://localhost:8080";
  return `${baseUrl.replace(/\/+$/, "")}${path}`;
}

export function isSameOriginRequest(request: Request) {
  const origin = request.headers.get("origin");
  if (origin && origin !== new URL(request.url).origin) {
    return false;
  }
  return request.headers.get("sec-fetch-site") !== "cross-site";
}

export async function forwardBackendResponse(
  path: string,
  init: RequestInit,
) {
  try {
    const response = await fetch(backendUrl(path), {
      ...init,
      cache: "no-store",
    });
    return new Response(response.body, {
      status: response.status,
      headers: {
        "Content-Type":
          response.headers.get("content-type") ?? "application/json",
      },
    });
  } catch {
    return Response.json(
      {
        message:
          "Could not reach the Robsen API. Check that Spring is running and BACKEND_API_URL is correct.",
      },
      { status: 502 },
    );
  }
}
