import { apiFetch } from "@/app/lib/api-client";
import { humanize } from "./format";
import type { DropdownOption, DropdownSource } from "./types";

export const DROPDOWN_SOURCES: Record<string, DropdownSource> = {
  business: { endpoint: "businesses/active", asRelation: true },
  territory: { endpoint: "territories", asRelation: true },
  status: { endpoint: "statuses", asRelation: false },
  "opportunity-status": { endpoint: "opportunity-statuses", asRelation: false },
  partner: { endpoint: "partners", asRelation: true },
  category: { endpoint: "categories", asRelation: true },
  product: { endpoint: "products", asRelation: true },
  "distribution-agreement": { endpoint: "distribution-agreements", asRelation: true },
  order: { endpoint: "orders", asRelation: true },
  "order-item": { endpoint: "order-items", asRelation: true },
};

// Module-level cache + in-flight dedupe so 6 dropdowns don't fire 6 identical requests.
const dropdownCache = new Map<string, Promise<DropdownOption[]>>();

export function loadDropdownOptions(endpoint: string): Promise<DropdownOption[]> {
  const cached = dropdownCache.get(endpoint);
  if (cached) return cached;
  const promise = apiFetch<unknown>(endpoint)
    .then((payload) => normalizeOptions(payload))
    .catch((error) => {
      dropdownCache.delete(endpoint); // allow retry on failure
      throw error;
    });
  dropdownCache.set(endpoint, promise);
  return promise;
}

function normalizeOptions(payload: unknown): DropdownOption[] {
  let list: unknown = payload;
  if (payload && typeof payload === "object" && !Array.isArray(payload)) {
    const envelope = payload as Record<string, unknown>;
    list =
      (Array.isArray(envelope.data) && envelope.data) ||
      (Array.isArray(envelope.content) && envelope.content) ||
      (Array.isArray(envelope.items) && envelope.items) ||
      payload;
  }
  if (!Array.isArray(list)) {
    throw new Error("Dropdown endpoint did not return a list.");
  }
  const seen = new Set<string>();
  const options: DropdownOption[] = [];
  for (const item of list as unknown[]) {
    const option = toOption(item);
    if (!option || seen.has(option.value)) continue;
    seen.add(option.value);
    options.push(option);
  }
  return options;
}

function toOption(item: unknown): DropdownOption | null {
  if (item == null) return null;
  if (typeof item === "string" || typeof item === "number") {
    const value = String(item);
    return { value, label: humanize(value) };
  }
  if (typeof item !== "object") return null;
  const obj = item as Record<string, unknown>;
  const rawId = obj.id ?? obj.value ?? obj.code ?? obj.key;
  const rawLabel =
    obj.name ??
    obj.label ??
    obj.title ??
    obj.fullName ??
    obj.legalName ??
    obj.tradeName ??
    obj.orderNumber ??
    obj.productName ??
    obj.statusName ??
    obj.territoryName ??
    obj.partnerName ??
    obj.categoryName ??
    rawId;
  if (rawId == null && rawLabel == null) return null;
  const value = String(rawId ?? rawLabel);
  const label = String(rawLabel ?? value);
  return { value, label: humanize(label) };
}

