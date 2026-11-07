"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import ProtectedArea, { useAuthProfile } from "@/app/components/protected-area";
import { apiFetch } from "@/app/lib/api-client";
import { StatusBadge } from "@/app/components/ui";
import { ACTIVE_BUSINESS_STORAGE_KEY, hasPermission } from "@/app/lib/auth";
import { findResource, type ResourceDefinition } from "@/app/lib/resources";
import { FieldInput } from "./field-input";
import { ProductCatalog, ProductCatalogSummary } from "./product-catalog";
import { defaultValues, formatValue, isDropdownColumn, loadRecords, statusTone, toRequestBody, valuesFromRecord } from "./record-utils";
import { humanize } from "./format";
import type { BusinessContextOption, FormValue, RecordValue } from "./types";
import LocationMapLink, { isLocationField } from "../location-map-link";

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
  const [pageSize, setPageSize] = useState(10);
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<RecordValue | null>(null);
  const [formValues, setFormValues] = useState<Record<string, FormValue>>({});
  const [isSaving, setIsSaving] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [columnFilters, setColumnFilters] = useState<Record<string, string>>({});
  const [businessOptions, setBusinessOptions] = useState<BusinessContextOption[]>([]);
  const [selectedBusinessId, setSelectedBusinessId] = useState("");
  const isProductsPage = resource.key === "products";
  const isBusinessScopedProducts = resource.key === "products" && !profile?.superAdmin;
  const isBusinessScopedListings = resource.key === "product-listings" && !profile?.superAdmin;
  const isBusinessScopedCategories = resource.key === "product-categories" && !profile?.superAdmin;
  const isBusinessScopedResource = isBusinessScopedProducts || isBusinessScopedListings || isBusinessScopedCategories;
  const listEndpoint = isBusinessScopedResource
    ? selectedBusinessId
      ? isBusinessScopedCategories
        ? `product-categories/business/${selectedBusinessId}`
        : isBusinessScopedProducts
          ? `products/catalog/business/${selectedBusinessId}`
          : `product-listings/business/${selectedBusinessId}`
      : ""
    : resource.listEndpoint;
  const selectedBusiness = businessOptions.find((business) => String(business.id) === selectedBusinessId);
  const canCreateInBusiness = profile?.superAdmin || (resource.key === "products"
    ? selectedBusiness?.canCreateProducts
    : resource.key === "product-categories"
      ? selectedBusiness?.canCreateProductCategories
      : canCreate);
  const canReadInBusiness = profile?.superAdmin || (resource.key === "products"
    ? canRead
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

  const totalPages = Math.max(1, Math.ceil(visibleRecords.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const pageRecords = visibleRecords.slice((currentPage - 1) * pageSize, currentPage * pageSize);
  const firstRecord = visibleRecords.length ? (currentPage - 1) * pageSize + 1 : 0;
  const lastRecord = Math.min(currentPage * pageSize, visibleRecords.length);

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

  async function approveBusiness(record: RecordValue) {
    const id = record[resource.idField ?? "id"];
    if (id === undefined) return;
    setError("");
    try {
      const values = valuesFromRecord(resource.fields, record);
      const payload = await toRequestBody(resource.fields, values, true);
      payload.verificationStatus = "APPROVED";
      await apiFetch(`${resource.endpoint}/${encodeURIComponent(String(id))}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      await reload();
    } catch (approvalError) {
      setError(approvalError instanceof Error ? approvalError.message : "Could not approve this business.");
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
              {isProductsPage ? <><Link href="/dashboard/products">Catalog</Link><span aria-hidden="true">/</span></> : null}
              <span>{resource.label}</span>
            </div>
            <div className="list-heading">
              <div>
                <p className="dashboard-eyebrow">NETWORK MANAGEMENT</p>
                <h1>{resource.label}</h1>
                <p className="dashboard-subtitle">{isProductsPage ? "Manage your product catalog across your business network." : resource.description}</p>
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
                      setPage(1);
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

            {isProductsPage && canReadInBusiness ? <ProductCatalogSummary records={records} isLoading={isLoading} /> : null}

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

            {isProductsPage && (isBusinessScopedResource ? canReadInBusiness : canRead) ? (
              <ProductCatalog
                canDelete={canDelete}
                canEditRecord={canEditRecord}
                isLoading={isLoading}
                onDelete={(record) => void remove(record)}
                onEdit={startEdit}
                records={records}
              />
            ) : null}

            {!isProductsPage && (isBusinessScopedResource ? canReadInBusiness : canRead) ? <section aria-label={`${resource.label} records`} className="records-panel">
              <div className="records-toolbar">
                <div>
                  <h2>Records</h2>
                  <span>{isLoading ? "Loading…" : `${records.length} total`}</span>
                </div>
                <input
                  aria-label={`Search ${resource.label.toLowerCase()}`}
                  className="search-input"
                  onChange={(event) => { setSearch(event.target.value); setPage(1); }}
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
                          { setPage(1); setColumnFilters((current) => ({
                            ...current,
                            [column]: event.target.value,
                          })); }
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
                      onClick={() => { setColumnFilters({}); setPage(1); }}
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
                          <th key={column} scope="col">{humanize(column)}</th>
                        ))}
                        {canUpdate || canDelete ? <th aria-label="Actions" scope="col" /> : null}
                      </tr>
                    </thead>
                    <tbody>
                      {pageRecords.map((record, index) => (
                        <tr key={String(record[resource.idField ?? "id"] ?? index)}>
                          {resource.columns.map((column) => (
                            <td key={column}>
                              {column.toLowerCase().includes("status") && formatValue(record[column]) !== "—" ? (
                                <StatusBadge tone={statusTone(record[column])}>
                                  {formatValue(record[column])}
                                </StatusBadge>
                              ) : isLocationField(column) && formatValue(record[column]) !== "—" ? (
                                <span className="location-cell">
                                  {formatValue(record[column])}
                                  <LocationMapLink location={formatValue(record[column])} />
                                </span>
                              ) : formatValue(record[column])}
                            </td>
                          ))}
                          {canEditRecord(record) || canDelete ? (
                            <td className="row-actions">
                              {canEditRecord(record) ? (
                                <button onClick={() => startEdit(record)} type="button">
                                  Edit
                                </button>
                              ) : null}
                              {resource.key === "businesses" && canEditRecord(record) &&
                                !["APPROVED", "CONFIRMED"].includes(String(record.verificationStatus ?? "").toUpperCase()) ? (
                                <button onClick={() => void approveBusiness(record)} type="button">
                                  Approve
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
              {!isLoading && visibleRecords.length > 0 ? (
                <div className="table-pagination" aria-label="Table pagination">
                  <span className="pagination-range">Showing {firstRecord}–{lastRecord} of {visibleRecords.length} records</span>
                  <label className="pagination-size">Rows per page
                    <select value={pageSize} onChange={(event) => { setPageSize(Number(event.target.value)); setPage(1); }}>
                      {[10, 25, 50, 100].map((size) => <option key={size} value={size}>{size}</option>)}
                    </select>
                  </label>
                  <div className="pagination-controls">
                    <button aria-label="First page" disabled={currentPage === 1} onClick={() => setPage(1)} type="button">«</button>
                    <button aria-label="Previous page" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)} type="button">‹</button>
                    {Array.from({ length: totalPages }, (_, index) => index + 1).filter((number) => number === 1 || number === totalPages || Math.abs(number - currentPage) <= 1).map((number, index, pages) => (
                      <span className="pagination-page-item" key={number}>{index > 0 && number - pages[index - 1] > 1 ? <span aria-hidden="true">…</span> : null}<button aria-current={currentPage === number ? "page" : undefined} onClick={() => setPage(number)} type="button">{number}</button></span>
                    ))}
                    <button aria-label="Next page" disabled={currentPage === totalPages} onClick={() => setPage(currentPage + 1)} type="button">›</button>
                    <button aria-label="Last page" disabled={currentPage === totalPages} onClick={() => setPage(totalPages)} type="button">»</button>
                  </div>
                </div>
              ) : null}
            </section> : isProductsPage ? null : <section className="dashboard-empty-state"><h2>Record browsing is not enabled</h2><p>You can use the actions granted to you on this page. Ask the business owner for read access if you need to browse existing {resource.label.toLowerCase()}.</p></section>}
          </>
        )}
      </div>
    </main>
  );
}
