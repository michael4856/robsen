"use client";

import { useState } from "react";
import { StatusBadge } from "@/app/components/ui";
import { productCategoryName } from "./record-utils";
import type { RecordValue } from "./types";

export function ProductCatalogSummary({ records, isLoading }: { records: RecordValue[]; isLoading: boolean }) {
  const activeCount = records.filter((record) => Boolean(record.active)).length;
  const categories = new Set(records.map(productCategoryName).filter(Boolean));
  const cards = [
    { label: "Total products", value: records.length, symbol: "◇", tone: "blue" },
    { label: "Active products", value: activeCount, symbol: "✓", tone: "green" },
    { label: "Categories", value: categories.size, symbol: "◆", tone: "violet" },
  ];

  return <section className="product-summary-grid" aria-label="Product catalog summary">
    {cards.map((card) => <article className={`product-summary-card product-summary-${card.tone}`} key={card.label}>
      <span className="product-summary-icon" aria-hidden="true">{card.symbol}</span>
      <div><p>{card.label}</p><strong>{isLoading ? "—" : card.value.toLocaleString()}</strong></div>
    </article>)}
  </section>;
}

export function ProductCatalog({
  records,
  isLoading,
  canDelete,
  canEditRecord,
  onEdit,
  onDelete,
}: {
  records: RecordValue[];
  isLoading: boolean;
  canDelete: boolean;
  canEditRecord: (record: RecordValue) => boolean;
  onEdit: (record: RecordValue) => void;
  onDelete: (record: RecordValue) => void;
}) {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("");
  const [status, setStatus] = useState("");
  const [sort, setSort] = useState("newest");
  const [page, setPage] = useState(1);
  const pageSize = 4;
  const categories = [...new Set(records.map(productCategoryName).filter(Boolean))].sort((a, b) => a.localeCompare(b));
  const filtered = records.filter((record) => {
    const searchable = [record.name, record.description, productCategoryName(record)].filter(Boolean).join(" ").toLowerCase();
    if (query.trim() && !searchable.includes(query.trim().toLowerCase())) return false;
    if (category && productCategoryName(record) !== category) return false;
    if (status && String(Boolean(record.active)) !== status) return false;
    return true;
  }).sort((left, right) => {
    if (sort === "name") return String(left.name ?? "").localeCompare(String(right.name ?? ""));
    if (sort === "price-low") return Number(left.price ?? 0) - Number(right.price ?? 0);
    if (sort === "price-high") return Number(right.price ?? 0) - Number(left.price ?? 0);
    return Number(right.id ?? 0) - Number(left.id ?? 0);
  });
  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const currentPage = Math.min(page, pageCount);
  const pageRecords = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize);
  const start = filtered.length ? (currentPage - 1) * pageSize + 1 : 0;
  const end = Math.min(currentPage * pageSize, filtered.length);

  function changeFilter(update: () => void) {
    update();
    setPage(1);
  }

  return <section aria-label="Product catalog" className="records-panel product-catalog-panel">
    <div className="product-catalog-heading">
      <div><h2>All products</h2><span>{isLoading ? "Loading…" : `${filtered.length.toLocaleString()} products`}</span></div>
    </div>
    <div className="product-catalog-filters" role="search">
      <label className="product-search-control"><span className="visually-hidden">Search products</span><span aria-hidden="true">⌕</span><input onChange={(event) => changeFilter(() => setQuery(event.target.value))} placeholder="Search products…" type="search" value={query} /></label>
      <label><span className="visually-hidden">Filter by category</span><select onChange={(event) => changeFilter(() => setCategory(event.target.value))} value={category}><option value="">All categories</option>{categories.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>
      <label><span className="visually-hidden">Filter by status</span><select onChange={(event) => changeFilter(() => setStatus(event.target.value))} value={status}><option value="">All statuses</option><option value="true">Active</option><option value="false">Inactive</option></select></label>
      <label><span className="visually-hidden">Sort products</span><select onChange={(event) => changeFilter(() => setSort(event.target.value))} value={sort}><option value="newest">Newest first</option><option value="name">Name A–Z</option><option value="price-low">Price: low to high</option><option value="price-high">Price: high to low</option></select></label>
    </div>
    {isLoading ? <div className="empty-state">Loading products…</div> : filtered.length === 0 ? <div className="empty-state">{records.length ? "No products match these filters." : "No products yet. Add your first product to the catalog."}</div> : <>
      <div className="table-scroll product-table-scroll"><table className="records-table product-table">
        <thead><tr><th scope="col">Product</th><th scope="col">Category</th><th scope="col">Price</th><th scope="col">Status</th><th scope="col">Actions</th></tr></thead>
        <tbody>{pageRecords.map((record) => {
          const name = String(record.name ?? "Unnamed product");
          const categoryName = productCategoryName(record);
          const canEdit = canEditRecord(record);
          return <tr key={String(record.id)}>
            <td><div className="product-table-identity"><div className="product-table-image">{record.imageUrl ? <img alt="" loading="lazy" onError={(event) => { event.currentTarget.style.display = "none"; }} src={String(record.imageUrl)} /> : <span aria-hidden="true">{name.slice(0, 2).toUpperCase()}</span>}</div><div><strong>{name}</strong><p>{String(record.description ?? "")}</p></div></div></td>
            <td>{categoryName ? <span className="product-category-badge">{categoryName}</span> : <span className="product-table-muted">Uncategorized</span>}</td>
            <td><strong className="product-table-price">ETB {Number(record.price ?? 0).toLocaleString()}</strong></td>
            <td><StatusBadge className={`product-status-badge${record.active ? " is-active" : ""}`} tone={record.active ? "success" : "neutral"}><i aria-hidden="true" />{record.active ? "Active" : "Inactive"}</StatusBadge></td>
            <td className="product-table-actions">{canEdit ? <button aria-label={`Edit ${name}`} onClick={() => onEdit(record)} type="button">Edit</button> : null}{canDelete ? <button aria-label={`Delete ${name}`} className="danger-text" onClick={() => onDelete(record)} type="button">Delete</button> : null}</td>
          </tr>;
        })}</tbody>
      </table></div>
      <div className="product-pagination"><span>Showing {start} to {end} of {filtered.length.toLocaleString()} products</span><div aria-label="Product pages" className="pagination-buttons"><button aria-label="Previous page" disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)} type="button">‹</button>{Array.from({ length: pageCount }, (_, index) => index + 1).slice(Math.max(0, currentPage - 3), Math.max(5, currentPage + 2)).map((pageNumber) => <button aria-current={currentPage === pageNumber ? "page" : undefined} key={pageNumber} onClick={() => setPage(pageNumber)} type="button">{pageNumber}</button>)}<button aria-label="Next page" disabled={currentPage === pageCount} onClick={() => setPage(currentPage + 1)} type="button">›</button></div></div>
    </>}
  </section>;
}



