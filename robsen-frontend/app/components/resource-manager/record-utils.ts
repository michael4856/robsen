import { apiFetch } from "@/app/lib/api-client";
import { resources, type FieldDefinition } from "@/app/lib/resources";
import { DROPDOWN_SOURCES } from "./dropdown-options";
import type { FormValue, RecordValue } from "./types";

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

export function isNamedRoleList(value: unknown): value is Array<{ name: string }> {
  return (
    Array.isArray(value) &&
    value.every(
      (role) =>
        typeof role === "object" &&
        role !== null &&
        "name" in role &&
        typeof role.name === "string",
    )
  );
}

export function isDropdownColumn(column: string) {
  return [
    "business",
    "businessId",
    "territory",
    "territoryId",
    "status",
    "partner",
    "partnerId",
    "category",
    "categoryId",
    "product",
    "productId",
    "role",
    "order",
    "orderId",
    "distributionAgreement",
    "distributionAgreementId",
  ].includes(column);
}

export function defaultValues(fields: FieldDefinition[]): Record<string, FormValue> {
  return Object.fromEntries(
    fields.map((field) => {
      if (field.kind === "boolean") return [field.name, true];
      if (field.kind === "navigation" || field.kind === "permissions" || field.kind === "checkboxes") {
        return [field.name, []];
      }
      return [field.name, ""];
    }),
  );
}

export async function loadRecords(endpoint: string, label: string) {
  const result = await apiFetch<unknown>(endpoint);
  if (!Array.isArray(result)) {
    throw new Error(`${label} endpoint did not return a list.`);
  }
  return result as RecordValue[];
}

export function valuesFromRecord(fields: FieldDefinition[], record: RecordValue): Record<string, FormValue> {
  const values: Record<string, FormValue> = {};
  for (const field of fields) {
    const value = record[field.name];
    if (field.kind === "password") {
      values[field.name] = "";
    } else if (field.kind === "navigation" || field.kind === "permissions" || field.kind === "checkboxes") {
      values[field.name] = Array.isArray(value) ? value.map(String) : [];
    } else if (isRelationKind(field.kind)) {
      // Accept { id } or { id, name } or plain scalar.
      if (value && typeof value === "object" && "id" in (value as object)) {
        values[field.name] = String((value as { id: unknown }).id);
      } else {
        values[field.name] = value == null ? "" : String(value);
      }
    } else if (field.kind === "boolean") {
      values[field.name] = Boolean(value);
    } else if (value != null && field.kind === "datetime-local") {
      values[field.name] = String(value).slice(0, 16);
    } else {
      values[field.name] = value == null ? "" : String(value);
    }
  }
  return values;
}

export function isRelationKind(kind: string | undefined) {
  if (!kind) return false;
  if (kind === "relation") return true;
  const source = DROPDOWN_SOURCES[kind];
  return Boolean(source?.asRelation);
}

export async function toRequestBody(
  fields: FieldDefinition[],
  values: Record<string, FormValue>,
  isUpdate: boolean,
): Promise<Record<string, unknown>> {
  const body: Record<string, unknown> = {};
  for (const field of fields) {
    if (field.createOnly && isUpdate) continue;
    const value = values[field.name];
    const kind = field.kind ?? "text";

    if (kind === "boolean") {
      body[field.name] = Boolean(value);
    } else if (kind === "navigation" || kind === "permissions" || kind === "checkboxes") {
      const selectedValues = Array.isArray(value) ? value : [];
      if (field.required && selectedValues.length === 0) {
        throw new Error(`Select at least one ${field.label.toLowerCase()}.`);
      }
      body[field.name] = selectedValues;
    } else if (isRelationKind(kind) || kind === "role" || DROPDOWN_SOURCES[kind]) {
      const source = DROPDOWN_SOURCES[kind];
      const asRelation = kind === "relation" || source?.asRelation === true;
      const trimmed = String(value ?? "").trim();
      if (trimmed) {
        if (asRelation) {
          const id = Number(trimmed);
          if (!Number.isSafeInteger(id) || id < 1) {
            throw new Error(`${field.label} must reference a valid record.`);
          }
          body[field.name] = { id };
        } else {
          // Enum-style (status, opportunity-status, role): send raw value.
          body[field.name] = trimmed;
        }
      } else if (field.required) {
        throw new Error(`${field.label} is required.`);
      }
    } else if (String(value ?? "").trim()) {
      if (kind === "number") {
        const number = Number(value);
        if (!Number.isFinite(number)) {
          throw new Error(`${field.label} must be a valid number.`);
        }
        body[field.name] = number;
      } else {
        body[field.name] = String(value).trim();
      }
    } else if (field.required) {
      throw new Error(`${field.label} is required.`);
    }
  }
  return body;
}

export function formatValue(value: unknown): string {
  if (value == null || value === "") return "—";
  if (typeof value === "boolean") return value ? "Active" : "Inactive";
  if (Array.isArray(value)) {
    return value.length
      ? value
          .map((item) => resources.find((resource) => resource.key === item)?.label ?? String(item))
          .join(", ")
      : "No sections";
  }
  if (typeof value === "object") {
    const nested = value as Record<string, unknown>;
    const label =
      nested.fullName ??
      nested.legalName ??
      nested.tradeName ??
      nested.name ??
      nested.title ??
      nested.orderNumber ??
      nested.productName ??
      nested.tenantName ??
      nested.statusName ??
      nested.territoryName ??
      nested.partnerName ??
      nested.categoryName;
    return label == null ? `#${String(nested.id ?? nested.tenantId ?? "?")}` : String(label);
  }
  return String(value);
}

export function statusTone(value: unknown): "neutral" | "success" | "warning" | "danger" {
  const status = formatValue(value).toLowerCase().replaceAll("_", " ").trim();
  if (/\b(active|approved|complete|completed|accepted|fulfilled|paid|verified)\b/.test(status)) return "success";
  if (/\b(pending|invited|draft|open|processing)\b/.test(status)) return "warning";
  if (/\b(inactive|rejected|reject|cancelled|canceled|removed|suspended|suspend|failed|overdue)\b/.test(status)) return "danger";
  return "neutral";
}

export function productCategoryName(record: RecordValue) {
  const category = record.productCategory;
  if (typeof category === "string") return category;
  if (category && typeof category === "object") {
    const value = category as Record<string, unknown>;
    return String(value.name ?? value.categoryName ?? "");
  }
  return "";
}
