export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const response = await fetch(`/api/backend/${path.replace(/^\/+/, "")}`, {
    ...init,
    headers: {
      Accept: "application/json",
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...init.headers,
    },
    cache: "no-store",
  });

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as
      | { message?: string; error?: string }
      | null;
    throw new Error(error?.message ?? error?.error ?? `Request failed (${response.status}).`);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
