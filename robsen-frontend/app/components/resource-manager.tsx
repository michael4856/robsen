"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";
import { ACTIVE_BUSINESS_STORAGE_KEY, hasPermission } from "@/app/lib/auth";
import {
  findResource,
  resources,
  type FieldDefinition,
  type ResourceDefinition,
} from "@/app/lib/resources";

type RecordValue = Record<string, unknown>;
type FormValue = string | boolean | string[];
type BusinessContextOption = {
  id: number;
  legalName?: string;
  tradeName?: string | null;
  canCreateProducts?: boolean;
  canUpdateProducts?: boolean;
  canReadProductListings?: boolean;
  canReadProductCategories?: boolean;
  canCreateProductCategories?: boolean;
  canUpdateProductCategories?: boolean;
};

type DropdownOption = {
  value: string;
  label: string;
};

type DropdownSource = {
  endpoint: string;
  /** Whether the API expects a numeric relation `{ id }` (true) or a raw label/string (false). */
  asRelation?: boolean;
  /** Optional transform of the raw endpoint payload into options. */
  transform?: (payload: unknown) => DropdownOption[];
};

/**
 * Central dropdown registry.
 * Add a new kind here and it instantly works everywhere in the form.
 * `kind` in FieldDefinition must match a key below.
 */
const DROPDOWN_SOURCES: Record<string, DropdownSource> = {
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

function loadDropdownOptions(endpoint: string): Promise<DropdownOption[]> {
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

// ---------------------------------------------------------------------------
// Top-level component (unchanged contract)
// ---------------------------------------------------------------------------

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
  const hasResourcePermission = profile?.permissions?.some((permission) =>
    permission.startsWith(`${resource.key}:`),
  ) ?? false;
  const canAccessResource =
    (profile?.superAdmin || hasResourcePermission) &&
    (!resource.adminOnly || profile?.superAdmin);
  const canRead = hasPermission(profile, `${resource.key}:READ`);
  const canCreate = hasPermission(profile, `${resource.key}:CREATE`);
  const canUpdate = hasPermission(profile, `${resource.key}:UPDATE`);
  const canDelete = hasPermission(profile, `${resource.key}:DELETE`);
  const [records, setRecords] = useState<RecordValue[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<RecordValue | null>(null);
  const [formValues, setFormValues] = useState<Record<string, FormValue>>({});
  const [isSaving, setIsSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [columnFilters, setColumnFilters] = useState<Record<string, string>>({});
  const [businessOptions, setBusinessOptions] = useState<BusinessContextOption[]>([]);
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const isBusinessScopedProducts = resource.key === "products" && !profile?.superAdmin;
  const isBusinessScopedListings = resource.key === "product-listings" && !profile?.superAdmin;
  const isBusinessScopedCategories = resource.key === "product-categories" && !profile?.superAdmin;
  const isBusinessScopedResource = isBusinessScopedProducts || isBusinessScopedListings || isBusinessScopedCategories;
  const listEndpoint = isBusinessScopedResource
    ? selectedBusinessId
      ? isBusinessScopedCategories
        ? `product-categories/business/${selectedBusinessId}`
        : `${isBusinessScopedProducts ? "products" : "product-listings"}/business/${selectedBusinessId}`
      : ""
    : resource.listEndpoint;
  const selectedBusiness = businessOptions.find((business) => String(business.id) === selectedBusinessId);
  const canCreateInBusiness = profile?.superAdmin || (resource.key === "products"
    ? selectedBusiness?.canCreateProducts
    : resource.key === "product-categories"
      ? selectedBusiness?.canCreateProductCategories
      : canCreate);
  const canReadInBusiness = profile?.superAdmin || (resource.key === "products"
    ? selectedBusiness?.canReadProductListings
    : resource.key === "product-categories"
      ? selectedBusiness?.canReadProductCategories
      : canRead);

  useEffect(() => {
    if (!isBusinessScopedResource || !profile) return;
    let active = true;
    apiFetch<BusinessContextOption[]>("product-listings/businesses")
      .then((businesses) => {
        if (!active) return;
        setBusinessOptions(businesses);
        const savedBusinessId = window.sessionStorage.getItem(ACTIVE_BUSINESS_STORAGE_KEY);
        setSelectedBusinessId((current) =>
          businesses.some((business) => String(business.id) === current)
            ? current
            : businesses.some((business) => String(business.id) === savedBusinessId)
              ? savedBusinessId ?? ""
              : String(businesses[0]?.id ?? ""),
        );
      })
      .catch((loadError: unknown) => {
        if (!active) return;
        setBusinessOptions([]);
        setSelectedBusinessId("");
        setError(loadError instanceof Error ? loadError.message : "Could not load business workspaces.");
      });
    return () => { active = false; };
  }, [isBusinessScopedResource, profile]);

  async function reload() {
    if (!canAccessResource || !(isBusinessScopedResource ? canReadInBusiness : canRead) || !listEndpoint) return;
    setError("");
    setIsLoading(true);
    try {
      const result = await apiFetch<unknown>(listEndpoint);
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
    if (!canAccessResource || !(isBusinessScopedResource ? canReadInBusiness : canRead) || (isBusinessScopedResource && !listEndpoint)) {
      let active = true;
      if (!isBusinessScopedResource || businessOptions.length === 0) {
        Promise.resolve().then(() => { if (active) setIsLoading(false); });
      }
      return () => { active = false; };
    }
    let active = true;
    loadRecords(listEndpoint, resource.label)
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
  }, [canAccessResource, canRead, canReadInBusiness, isBusinessScopedResource, listEndpoint, businessOptions.length, resource.label]);

  const filterableColumns = useMemo(
    () => resource.columns.filter((column) => isDropdownColumn(column)),
    [resource.columns],
  );

  const columnFilterOptions = useMemo(() => {
    const map: Record<string, string[]> = {};
    for (const column of filterableColumns) {
      const values = new Set<string>();
      for (const record of records) {
        const formatted = formatValue(record[column]);
        if (formatted && formatted !== "—") values.add(formatted);
      }
      map[column] = [...values].sort((a, b) => a.localeCompare(b));
    }
    return map;
  }, [records, filterableColumns]);

  const visibleRecords = useMemo(() => {
    const query = search.trim().toLowerCase();
    return records.filter((record) => {
      if (
        query &&
        !resource.columns.some((column) =>
          formatValue(record[column]).toLowerCase().includes(query),
        )
      ) {
        return false;
      }
      return Object.entries(columnFilters).every(([column, value]) => {
        if (!value) return true;
        return formatValue(record[column]) === value;
      });
    });
  }, [records, resource.columns, search, columnFilters]);

  function canEditRecord(record: RecordValue) {
    if (!canUpdate) return false;
    if (profile?.superAdmin || !["products", "product-categories"].includes(resource.key)) return true;
    const business = record.business as Record<string, unknown> | null | undefined;
    return Boolean(business?.id && String(business.id) === selectedBusinessId
      && (resource.key === "products" ? selectedBusiness?.canUpdateProducts : selectedBusiness?.canUpdateProductCategories));
  }

  function startCreate() {
    setSelected(null);
    const values = defaultValues(resource.fields);
    if ((isBusinessScopedListings || isBusinessScopedProducts || isBusinessScopedCategories) && selectedBusinessId) values.business = selectedBusinessId;
    setFormValues(values);
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
      const payload = await toRequestBody(resource.fields, formValues, Boolean(selected));
      if (!profile?.superAdmin && selectedBusinessId && (isBusinessScopedProducts || isBusinessScopedCategories)) {
        payload.business = { id: Number(selectedBusinessId) };
      }
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
              {canCreate && (isBusinessScopedProducts || isBusinessScopedCategories ? canCreateInBusiness : canCreate) ? (
                <button className="primary-button" onClick={startCreate} type="button">
                  <span aria-hidden="true">＋</span> Add {resource.singular}
                </button>
              ) : null}
            </div>

            {isBusinessScopedResource ? (
              <label className="business-selector dashboard-business-selector">
                <span>Business workspace</span>
                {businessOptions.length > 1 ? (
                  <select
                    onChange={(event) => {
                      setIsLoading(true);
                      setSelectedBusinessId(event.target.value);
                      window.sessionStorage.setItem(ACTIVE_BUSINESS_STORAGE_KEY, event.target.value);
                      setRecords([]);
                      setIsFormOpen(false);
                      setError("");
                    }}
                    value={selectedBusinessId}
                  >
                    {businessOptions.map((business) => (
                      <option key={business.id} value={business.id}>
                        {business.tradeName || business.legalName || `Business ${business.id}`}
                      </option>
                    ))}
                  </select>
                ) : (
                  <strong>{businessOptions[0]?.tradeName || businessOptions[0]?.legalName || `Business ${selectedBusinessId || "—"}`}</strong>
                )}
              </label>
            ) : null}

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
                  .filter((field) => !field.adminOnly || profile?.superAdmin)
                    .map((field) => (
                      (isBusinessScopedListings || isBusinessScopedProducts || isBusinessScopedCategories) && field.name === "business" && !profile?.superAdmin
                        ? <label className="field-control" key={`${field.name}-${field.label}`}>
                            <span>{field.label}</span>
                            <input readOnly value={businessOptions.find((business) => String(business.id) === selectedBusinessId)?.tradeName || businessOptions.find((business) => String(business.id) === selectedBusinessId)?.legalName || `Business ${selectedBusinessId}`} />
                          </label>
                        : <FieldInput
                            field={resource.key === "products" && field.name === "productCategory" && selectedBusinessId && !profile?.superAdmin
                              ? { ...field, endpoint: `product-categories/business/${selectedBusinessId}` }
                              : field}
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

            {(isBusinessScopedResource ? canReadInBusiness : canRead) ? <section aria-label={`${resource.label} records`} className="records-panel">
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

              {filterableColumns.length > 0 && records.length > 0 ? (
                <div className="records-filters" role="group" aria-label="Filter records">
                  {filterableColumns.map((column) => (
                    <label className="filter-control" key={column}>
                      <span>{humanize(column)}</span>
                      <select
                        onChange={(event) =>
                          setColumnFilters((current) => ({
                            ...current,
                            [column]: event.target.value,
                          }))
                        }
                        value={columnFilters[column] ?? ""}
                      >
                        <option value="">All</option>
                        {(columnFilterOptions[column] ?? []).map((option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        ))}
                      </select>
                    </label>
                  ))}
                  {Object.values(columnFilters).some(Boolean) ? (
                    <button
                      className="secondary-button"
                      onClick={() => setColumnFilters({})}
                      type="button"
                    >
                      Clear filters
                    </button>
                  ) : null}
                </div>
              ) : null}

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
                        {canUpdate || canDelete ? <th aria-label="Actions" /> : null}
                      </tr>
                    </thead>
                    <tbody>
                      {visibleRecords.map((record, index) => (
                        <tr key={String(record[resource.idField ?? "id"] ?? index)}>
                          {resource.columns.map((column) => (
                            <td key={column}>{formatValue(record[column])}</td>
                          ))}
                          {canEditRecord(record) || canDelete ? (
                            <td className="row-actions">
                              {canEditRecord(record) ? (
                                <button onClick={() => startEdit(record)} type="button">
                                  Edit
                                </button>
                              ) : null}
                              {canDelete && resource.deletable !== false ? (
                                <button
                                  className="danger-text"
                                  onClick={() => void remove(record)}
                                  type="button"
                                >
                                  {resource.deleteActionLabel ?? "Delete"}
                                </button>
                              ) : null}
                            </td>
                          ) : null}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section> : <section className="dashboard-empty-state"><h2>Record browsing is not enabled</h2><p>You can use the actions granted to you on this page. Ask the business owner for read access if you need to browse existing {resource.label.toLowerCase()}.</p></section>}
          </>
        )}
      </div>
    </main>
  );
}

// ---------------------------------------------------------------------------
// Field dispatch
// ---------------------------------------------------------------------------

function FieldInput({
  field,
  value,
  onChange,
}: {
  field: FieldDefinition;
  value: FormValue;
  onChange: (value: FormValue) => void;
}) {
  const kind = field.kind ?? "text";

  // Role is a special-case dropdown (was previously bespoke).
  if (kind === "role") {
    return <RoleInput field={field} onChange={onChange} value={value} />;
  }

  // Navigation / permissions remain multi-select checkboxes.
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
                  const selectedValues = Array.isArray(value) ? value : [];
                  onChange(
                    event.target.checked
                      ? [...selectedValues, resource.key]
                      : selectedValues.filter((item) => item !== resource.key),
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

  if (kind === "permissions") {
    return <PermissionInput field={field} onChange={onChange} value={value} />;
  }

  // Any kind registered in DROPDOWN_SOURCES renders as an async <select>.
  if (DROPDOWN_SOURCES[kind]) {
    return <AsyncSelectInput field={field} kind={kind} onChange={onChange} value={value} />;
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

  // Legacy "relation" also becomes a dropdown using its endpoint.
  if (kind === "relation") {
    return (
      <RelationSelectInput field={field} onChange={onChange} value={value} />
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
          {String(value) && !field.options?.includes(String(value)) ? (
            <option value={String(value)}>{humanize(String(value))} · Current</option>
          ) : null}
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
          required={field.required}
          step={kind === "number" ? "any" : undefined}
          type={kind}
          value={String(value)}
        />
      )}
    </label>
  );
}

// ---------------------------------------------------------------------------
// Async dropdown (used by business / territory / status / partner / category /
// product / order / distribution-agreement, etc.)
// ---------------------------------------------------------------------------

function AsyncSelectInput({
  field,
  kind,
  value,
  onChange,
}: {
  field: FieldDefinition;
  kind: string;
  value: FormValue;
  onChange: (value: FormValue) => void;
}) {
  const source = DROPDOWN_SOURCES[kind];
  const [options, setOptions] = useState<DropdownOption[]>([]);
  const [error, setError] = useState("");
  const [loaded, setLoaded] = useState(false);
  const isLoading = !loaded;

  useEffect(() => {
    let active = true;
    loadDropdownOptions(source.endpoint)
      .then((result) => {
        if (active) {
          setOptions(result);
          setLoaded(true);
        }
      })
      .catch((loadError: unknown) => {
        if (active) {
          setError(
            loadError instanceof Error ? loadError.message : "Could not load options.",
          );
          setLoaded(true);
        }
      })
      ;
    return () => {
      active = false;
    };
  }, [source.endpoint]);

  const currentValue = String(value ?? "");
  const hasCurrent = options.some((option) => option.value === currentValue);
  const currentLabel =
    options.find((option) => option.value === currentValue)?.label ??
    humanize(currentValue);

  return (
    <label className="field-control">
      <span>
        {field.label}
        {field.required ? <span className="required-mark"> *</span> : null}
      </span>
      <select
        disabled={isLoading}
        name={field.name}
        onChange={(event) => onChange(event.target.value)}
        required={field.required}
        value={currentValue}
      >
        <option value="">
          {isLoading ? "Loading…" : `Choose a ${field.label.toLowerCase()}`}
        </option>
        {currentValue && !hasCurrent ? (
          <option value={currentValue}>{currentLabel} (inactive)</option>
        ) : null}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? <span role="alert">{error}</span> : null}
    </label>
  );
}

// Legacy "relation" field — resolves to `endpoint` specified on the field itself.
function RelationSelectInput({
  field,
  value,
  onChange,
}: {
  field: FieldDefinition;
  value: FormValue;
  onChange: (value: FormValue) => void;
}) {
  const endpoint = (field as FieldDefinition & { endpoint?: string }).endpoint ?? "";
  const [options, setOptions] = useState<DropdownOption[]>([]);
  const [error, setError] = useState("");
  const [loadedEndpoint, setLoadedEndpoint] = useState("");
  const isLoading = Boolean(endpoint) && loadedEndpoint !== endpoint;

  useEffect(() => {
    if (!endpoint) return;
    let active = true;
    loadDropdownOptions(endpoint)
      .then((result) => {
        if (active) {
          setOptions(result);
          setLoadedEndpoint(endpoint);
        }
      })
      .catch((loadError: unknown) => {
        if (active) {
          setError(
            loadError instanceof Error ? loadError.message : "Could not load options.",
          );
          setLoadedEndpoint(endpoint);
        }
      })
      ;
    return () => {
      active = false;
    };
  }, [endpoint]);

  if (!endpoint) {
    // Fall back to text input if no endpoint was declared on the field.
    return (
      <label className="field-control">
        <span>
          {field.label}
          {field.required ? <span className="required-mark"> *</span> : null}
        </span>
        <input
          name={field.name}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Enter the related record ID"
          required={field.required}
          type="number"
          value={String(value)}
        />
      </label>
    );
  }

  const currentValue = String(value ?? "");
  const hasCurrent = options.some((option) => option.value === currentValue);
  const currentLabel =
    options.find((option) => option.value === currentValue)?.label ??
    humanize(currentValue);

  return (
    <label className="field-control">
      <span>
        {field.label}
        {field.required ? <span className="required-mark"> *</span> : null}
      </span>
      <select
        disabled={isLoading}
        name={field.name}
        onChange={(event) => onChange(event.target.value)}
        required={field.required}
        value={currentValue}
      >
        <option value="">
          {isLoading ? "Loading…" : `Choose a ${field.label.toLowerCase()}`}
        </option>
        {currentValue && !hasCurrent ? (
          <option value={currentValue}>{currentLabel} (inactive)</option>
        ) : null}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? <span role="alert">{error}</span> : null}
    </label>
  );
}

// ---------------------------------------------------------------------------
// Role (kept bespoke; uses its own active endpoint)
// ---------------------------------------------------------------------------

function RoleInput({
  field,
  value,
  onChange,
}: {
  field: FieldDefinition;
  value: FormValue;
  onChange: (value: FormValue) => void;
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

// ---------------------------------------------------------------------------
// Permission (unchanged behavior, unchanged markup)
// ---------------------------------------------------------------------------

function PermissionInput({
  field,
  value,
  onChange,
}: {
  field: FieldDefinition;
  value: FormValue;
  onChange: (value: FormValue) => void;
}) {
  const [permissions, setPermissions] = useState<string[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    apiFetch<unknown>("roles/permission-catalog")
      .then((result) => {
        if (!Array.isArray(result) || result.some((item) => typeof item !== "string")) {
          throw new Error("Permission catalog returned invalid data.");
        }
        if (active) setPermissions(result as string[]);
      })
      .catch((loadError: unknown) => {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : "Could not load permissions.");
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const selected = Array.isArray(value) ? value : [];
  const groups = [...new Set(permissions.map((permission) => permission.split(":")[0]))];

  return (
    <fieldset className="navigation-permissions">
      <legend>{field.label}</legend>
      <p className="permission-help">
        Permissions are enforced by the API for active business memberships.
      </p>
      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}
      {groups.map((resource) => (
        <div className="permission-group" key={resource}>
          <strong>{humanize(resource)}</strong>
          <div className="navigation-permission-options">
            {permissions
              .filter((permission) => permission.startsWith(`${resource}:`))
              .map((permission) => {
                const action = permission.split(":")[1];
                return (
                  <label className="field-checkbox" key={permission}>
                    <input
                      checked={selected.includes(permission)}
                      onChange={(event) =>
                        onChange(
                          event.target.checked
                            ? [...selected, permission]
                            : selected.filter((item) => item !== permission),
                        )
                      }
                      type="checkbox"
                    />
                    <span>{humanize(action)}</span>
                  </label>
                );
              })}
          </div>
        </div>
      ))}
    </fieldset>
  );
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

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

function isDropdownColumn(column: string) {
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

function defaultValues(fields: FieldDefinition[]): Record<string, FormValue> {
  return Object.fromEntries(
    fields.map((field) => {
      if (field.kind === "boolean") return [field.name, true];
      if (field.kind === "navigation" || field.kind === "permissions") {
        return [field.name, []];
      }
      return [field.name, ""];
    }),
  );
}

async function loadRecords(endpoint: string, label: string) {
  const result = await apiFetch<unknown>(endpoint);
  if (!Array.isArray(result)) {
    throw new Error(`${label} endpoint did not return a list.`);
  }
  return result as RecordValue[];
}

function valuesFromRecord(fields: FieldDefinition[], record: RecordValue): Record<string, FormValue> {
  const values: Record<string, FormValue> = {};
  for (const field of fields) {
    const value = record[field.name];
    if (field.kind === "password") {
      values[field.name] = "";
    } else if (field.kind === "navigation" || field.kind === "permissions") {
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

function isRelationKind(kind: string | undefined) {
  if (!kind) return false;
  if (kind === "relation") return true;
  const source = DROPDOWN_SOURCES[kind];
  return Boolean(source?.asRelation);
}

async function toRequestBody(
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
    } else if (kind === "navigation" || kind === "permissions") {
      body[field.name] = Array.isArray(value) ? value : [];
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
      nested.tenantName ??
      nested.statusName ??
      nested.territoryName ??
      nested.partnerName ??
      nested.categoryName;
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
