"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";
import {
  findResource,
  resources,
  type FieldDefinition,
  type ResourceDefinition,
} from "@/app/lib/resources";

type RecordValue = Record<string, unknown>;

export default function ResourceManager({ resourceKey }: { resourceKey: string }) {
  const resource = findResource(resourceKey);
  if (!resource) {
    return (
      <main className="dashboard-page">
        <div className="page-state">
          <h1>Page not found</h1>
          <Link href="/dashboard">Back to workspace</Link>
        </div>
      </main>
    );
  }

  return (
    <ProtectedArea>
      <ResourceContent resource={resource} />
    </ProtectedArea>
  );
}

function ResourceContent({ resource }: { resource: ResourceDefinition }) {
  const profile = useAuthProfile();
  const isAdmin =
    profile?.role?.toUpperCase() === "ADMIN" ||
    profile?.role?.toUpperCase() === "ROLE_ADMIN";
  const hasNavigationAccess = profile?.navigationItems?.includes(resource.key) ?? false;
  const canAccessResource =
    hasNavigationAccess && (!resource.adminOnly || isAdmin);
  const [records, setRecords] = useState<RecordValue[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<RecordValue | null>(null);
  const [formValues, setFormValues] = useState<Record<string, string | boolean | string[]>>({});
  const [isSaving, setIsSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);

  async function reload() {
    if (!canAccessResource) return;
    setError("");
    setIsLoading(true);
    try {
      const result = await apiFetch<unknown>(resource.listEndpoint);
      if (!Array.isArray(result)) {
        throw new Error(`${resource.label} endpoint did not return a list.`);
      }
      setRecords(result as RecordValue[]);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Could not load records.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    if (!canAccessResource) return;
    let active = true;
    loadRecords(resource.listEndpoint, resource.label)
      .then((result) => {
        if (active) setRecords(result);
      })
      .catch((loadError: unknown) => {
        if (active) {
          setError(
            loadError instanceof Error ? loadError.message : "Could not load records.",
          );
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [canAccessResource, resource.label, resource.listEndpoint]);

  const visibleRecords = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return records;
    return records.filter((record) =>
      resource.columns.some((column) =>
        formatValue(record[column]).toLowerCase().includes(query),
      ),
    );
  }, [records, resource.columns, search]);

  function startCreate() {
    setSelected(null);
    setFormValues(defaultValues(resource.fields));
    setError("");
    setIsFormOpen(true);
  }

  function startEdit(record: RecordValue) {
    setSelected(record);
    setFormValues(valuesFromRecord(resource.fields, record));
    setError("");
    setIsFormOpen(true);
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSaving(true);
    setError("");
    try {
      const payload = toRequestBody(resource.fields, formValues, Boolean(selected));
      const id = selected?.[resource.idField ?? "id"];
      await apiFetch(
        selected ? `${resource.endpoint}/${encodeURIComponent(String(id))}` : resource.endpoint,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify(payload),
        },
      );
      setIsFormOpen(false);
      setSelected(null);
      await reload();
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "Could not save this record.");
    } finally {
      setIsSaving(false);
    }
  }

  async function remove(record: RecordValue) {
    const id = record[resource.idField ?? "id"];
    const action = resource.deleteActionLabel ?? "Delete";
    if (id === undefined || !window.confirm(`${action} this ${resource.singular}?`)) return;

    setError("");
    try {
      await apiFetch(`${resource.endpoint}/${encodeURIComponent(String(id))}`, {
        method: "DELETE",
      });
      await reload();
    } catch (removeError) {
      setError(removeError instanceof Error ? removeError.message : "Could not delete this record.");
    }
  }

  return (
    <main className="dashboard-page">
      <div className="dashboard-content">
        {!canAccessResource ? (
          <div className="page-state error-state">
            <h1>Access not available</h1>
            <p>Your role does not have permission to access {resource.label.toLowerCase()}.</p>
            <Link href="/dashboard">Back to workspace</Link>
          </div>
        ) : (
          <>
        <div className="breadcrumb">
          <Link href="/dashboard">Workspace</Link>
          <span aria-hidden="true">/</span>
          <span>{resource.label}</span>
        </div>
        <div className="list-heading">
          <div>
            <p className="dashboard-eyebrow">NETWORK MANAGEMENT</p>
            <h1>{resource.label}</h1>
            <p className="dashboard-subtitle">{resource.description}</p>
          </div>
          <button className="primary-button" onClick={startCreate} type="button">
            <span aria-hidden="true">＋</span> Add {resource.singular}
          </button>
        </div>

        {error ? (
          <div className="dashboard-alert" role="alert">
            {error}
            <button onClick={() => void reload()} type="button">
              Retry
            </button>
          </div>
        ) : null}

        {isFormOpen ? (
          <section aria-labelledby="record-form-title" className="editor-panel">
            <div className="editor-heading">
              <div>
                <p className="dashboard-eyebrow">
                  {selected ? "UPDATE RECORD" : "NEW RECORD"}
                </p>
                <h2 id="record-form-title">
                  {selected ? `Edit ${resource.singular}` : `Add ${resource.singular}`}
                </h2>
              </div>
              <button
                aria-label="Close form"
                className="icon-button"
                onClick={() => setIsFormOpen(false)}
                type="button"
              >
                ×
              </button>
            </div>
            <form className="record-form" onSubmit={save}>
              {resource.fields
                .filter((field) => !field.createOnly || !selected)
                .filter((field) => !field.updateOnly || selected)
                .map((field) => (
                  <FieldInput
                    field={field}
                    key={`${field.name}-${field.label}`}
                    onChange={(value) =>
                      setFormValues((current) => ({ ...current, [field.name]: value }))
                    }
                    value={formValues[field.name] ?? ""}
                  />
                ))}
              <div className="form-actions">
                <button
                  className="secondary-button"
                  onClick={() => setIsFormOpen(false)}
                  type="button"
                >
                  Cancel
                </button>
                <button className="primary-button" disabled={isSaving} type="submit">
                  {isSaving ? "Saving…" : selected ? "Save changes" : "Create record"}
                </button>
              </div>
            </form>
          </section>
        ) : null}

        <section aria-label={`${resource.label} records`} className="records-panel">
          <div className="records-toolbar">
            <div>
              <h2>Records</h2>
              <span>{isLoading ? "Loading…" : `${records.length} total`}</span>
            </div>
            <input
              aria-label={`Search ${resource.label.toLowerCase()}`}
              className="search-input"
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search records"
              type="search"
              value={search}
            />
          </div>
          {isLoading ? (
            <div className="empty-state">Loading {resource.label.toLowerCase()}…</div>
          ) : visibleRecords.length === 0 ? (
            <div className="empty-state">
              {records.length === 0
                ? `No ${resource.label.toLowerCase()} yet. Add your first record to get started.`
                : "No records match your search."}
            </div>
          ) : (
            <div className="table-scroll">
              <table className="records-table">
                <thead>
                  <tr>
                    {resource.columns.map((column) => (
                      <th key={column}>{humanize(column)}</th>
                    ))}
                    <th aria-label="Actions" />
                  </tr>
                </thead>
                <tbody>
                  {visibleRecords.map((record, index) => (
                    <tr key={String(record[resource.idField ?? "id"] ?? index)}>
                      {resource.columns.map((column) => (
                        <td key={column}>{formatValue(record[column])}</td>
                      ))}
                      <td className="row-actions">
                        <button onClick={() => startEdit(record)} type="button">
                          Edit
                        </button>
                        {resource.deletable !== false ? (
                          <button
                            className="danger-text"
                            onClick={() => void remove(record)}
                            type="button"
                          >
                            {resource.deleteActionLabel ?? "Delete"}
                          </button>
                        ) : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
          </>
        )}
      </div>
    </main>
  );
}

function FieldInput({
  field,
  value,
  onChange,
}: {
  field: FieldDefinition;
  value: string | boolean | string[];
  onChange: (value: string | boolean | string[]) => void;
}) {
  const kind = field.kind ?? "text";
  if (kind === "navigation") {
    return (
      <fieldset className="navigation-permissions">
        <legend>{field.label}</legend>
        <div className="navigation-permission-options">
          {resources.map((resource) => (
            <label className="field-checkbox" key={resource.key}>
              <input
                checked={Array.isArray(value) && value.includes(resource.key)}
                onChange={(event) => {
                  const selected = Array.isArray(value) ? value : [];
                  onChange(
                    event.target.checked
                      ? [...selected, resource.key]
                      : selected.filter((item) => item !== resource.key),
                  );
                }}
                type="checkbox"
              />
              <span>{resource.label}</span>
            </label>
          ))}
        </div>
      </fieldset>
    );
  }
  if (kind === "role") {
    return <RoleInput field={field} onChange={onChange} value={value} />;
  }
  if (kind === "boolean") {
    return (
      <label className="field-checkbox">
        <input
          checked={Boolean(value)}
          name={field.name}
          onChange={(event) => onChange(event.target.checked)}
          type="checkbox"
        />
        <span>{field.label}</span>
      </label>
    );
  }

  return (
    <label className="field-control">
      <span>
        {field.label}
        {field.required ? <span className="required-mark"> *</span> : null}
      </span>
      {kind === "enum" ? (
        <select
          name={field.name}
          onChange={(event) => onChange(event.target.value)}
          required={field.required}
          value={String(value)}
        >
          <option value="">Choose an option</option>
          {field.options?.map((option) => (
            <option key={option} value={option}>
              {humanize(option)}
            </option>
          ))}
        </select>
      ) : (
        <input
          autoComplete={kind === "password" ? "new-password" : undefined}
          name={field.name}
          onChange={(event) => onChange(event.target.value)}
          placeholder={kind === "relation" ? "Enter the related record ID" : ""}
          required={field.required}
          step={kind === "number" ? "any" : undefined}
          type={kind === "relation" ? "number" : kind}
          value={String(value)}
        />
      )}
    </label>
  );
}

function RoleInput({
  field,
  value,
  onChange,
}: {
  field: FieldDefinition;
  value: string | boolean | string[];
  onChange: (value: string | boolean | string[]) => void;
}) {
  const [roles, setRoles] = useState<Array<{ name: string }>>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    apiFetch<unknown>("roles/active")
      .then((result) => {
        if (!isNamedRoleList(result)) {
          throw new Error("Active roles endpoint returned invalid role data.");
        }
        if (active) setRoles(result);
      })
      .catch((loadError: unknown) => {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "Could not load roles.");
        }
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <label className="field-control">
      <span>
        {field.label}
        {field.required ? <span className="required-mark"> *</span> : null}
      </span>
      <select
        name={field.name}
        onChange={(event) => onChange(event.target.value)}
        required={field.required}
        value={String(value)}
      >
        <option value="">Choose a role</option>
        {typeof value === "string" &&
        value &&
        !roles.some((role) => role.name === value) ? (
          <option value={value}>{humanize(value)} (inactive)</option>
        ) : null}
        {roles.map((role) => (
          <option key={role.name} value={role.name}>
            {humanize(role.name)}
          </option>
        ))}
      </select>
      {error ? <span role="alert">{error}</span> : null}
    </label>
  );
}

function isNamedRoleList(value: unknown): value is Array<{ name: string }> {
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

function defaultValues(fields: FieldDefinition[]) {
  return Object.fromEntries(
    fields.map((field) => [
      field.name,
      field.kind === "boolean" ? true : field.kind === "navigation" ? [] : "",
    ]),
  );
}

async function loadRecords(endpoint: string, label: string) {
  const result = await apiFetch<unknown>(endpoint);
  if (!Array.isArray(result)) {
    throw new Error(`${label} endpoint did not return a list.`);
  }
  return result as RecordValue[];
}

function valuesFromRecord(fields: FieldDefinition[], record: RecordValue) {
  const values: Record<string, string | boolean | string[]> = {};
  for (const field of fields) {
    const value = record[field.name];
    if (field.kind === "password") {
      values[field.name] = "";
    } else if (field.kind === "navigation") {
      values[field.name] = Array.isArray(value) ? value.map(String) : [];
    } else if (field.kind === "relation") {
      values[field.name] =
        value && typeof value === "object" && "id" in value
          ? String((value as { id: unknown }).id)
          : "";
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

function toRequestBody(
  fields: FieldDefinition[],
  values: Record<string, string | boolean | string[]>,
  isUpdate: boolean,
) {
  const body: Record<string, unknown> = {};
  for (const field of fields) {
    if (field.createOnly && isUpdate) continue;
    const value = values[field.name];
    if (field.kind === "boolean") {
      body[field.name] = Boolean(value);
    } else if (field.kind === "navigation") {
      body[field.name] = Array.isArray(value) ? value : [];
    } else if (field.kind === "relation") {
      if (String(value ?? "").trim()) {
        const id = Number(value);
        if (!Number.isSafeInteger(id) || id < 1) {
          throw new Error(`${field.label} must be a valid positive ID.`);
        }
        body[field.name] = { id };
      }
    } else if (String(value ?? "").trim()) {
      if (field.kind === "number") {
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

function formatValue(value: unknown): string {
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
      nested.tenantName;
    return label == null ? `#${String(nested.id ?? nested.tenantId ?? "?")}` : String(label);
  }
  return String(value);
}

function humanize(value: string) {
  return value
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/[-_]/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}
