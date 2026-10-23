import { cookies } from "next/headers";
import { ACCESS_TOKEN_COOKIE } from "@/app/lib/auth";
import { isSameOriginRequest } from "@/app/lib/backend";

export async function POST(request: Request) {
  if (!isSameOriginRequest(request)) {
    return Response.json({ message: "Cross-origin requests are not allowed." }, { status: 403 });
  }
  const cookieStore = await cookies();
  cookieStore.delete(ACCESS_TOKEN_COOKIE);
  return Response.json({ ok: true });
}
