"use client";

import { useState } from "react";

export function isLocationField(name: string) {
  return /address|location|city|region/i.test(name);
}

export function LocationMapInput({
  name,
  label,
  defaultValue = "",
  required = false,
  maxLength = 500,
  autoComplete,
}: {
  name: string;
  label: string;
  defaultValue?: string;
  required?: boolean;
  maxLength?: number;
  autoComplete?: string;
}) {
  const [location, setLocation] = useState(defaultValue);
  const inputId = `${name}-location-input`;
  return (
    <div className="field-control">
      <label htmlFor={inputId}>
        {label}{required ? <span className="required-mark"> *</span> : null}
      </label>
      <input
        autoComplete={autoComplete}
        id={inputId}
        maxLength={maxLength}
        name={name}
        onChange={(event) => setLocation(event.target.value)}
        required={required}
        value={location}
      />
      {location.trim() ? <LocationMapLink location={location} /> : null}
    </div>
  );
}

export default function LocationMapLink({
  location,
  className,
}: {
  location: string;
  className?: string;
}) {
  const [previewOpen, setPreviewOpen] = useState(false);
  const query = location.trim();
  if (!query || query === "—") return null;
  const encodedQuery = encodeURIComponent(query);
  const url = `https://www.google.com/maps/search/?api=1&query=${encodedQuery}`;
  const previewUrl = `https://maps.google.com/maps?q=${encodedQuery}&output=embed`;
  return (
    <span className="location-map">
      <a className={className ?? "location-map-link"} href={url} rel="noreferrer" target="_blank">
        View on Google Maps
      </a>
      <button
        aria-expanded={previewOpen}
        className="location-map-preview-toggle"
        onClick={() => setPreviewOpen((open) => !open)}
        type="button"
      >
        {previewOpen ? "Hide map preview" : "Preview map"}
      </button>
      {previewOpen ? (
        <iframe
          className="location-map-preview"
          loading="lazy"
          referrerPolicy="no-referrer"
          src={previewUrl}
          title={`Google Maps preview for ${query}`}
        />
      ) : null}
    </span>
  );
}
