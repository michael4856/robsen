"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "@/app/lib/api-client";
import { resources, type FieldDefinition } from "@/app/lib/resources";
import { DROPDOWN_SOURCES, loadDropdownOptions } from "./dropdown-options";
import { isNamedRoleList } from "./record-utils";
import { humanize } from "./format";
import type { DropdownOption, FormValue } from "./types";
import LocationMapLink, { isLocationField } from "../location-map-link";

export function FieldInput({
  field,
  value,
  onChange,
}: {
  field: FieldDefinition;
  value: FormValue;
  onChange: (value: FormValue) => void;
}) {
  const kind = field.kind ?? "text";
  const [imageError, setImageError] = useState("");

  if (kind === "image") {
    const imageValue = String(value ?? "");
    async function chooseImage(file?: File) {
      setImageError("");
      if (!file) return;
      if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) {
        setImageError("Choose a PNG, JPEG, or WebP image.");
        return;
      }
      if (file.size > 8 * 1024 * 1024) {
        setImageError("Choose an image smaller than 8 MB.");
        return;
      }
      try {
        const dataUrl = await new Promise<string>((resolve, reject) => {
          const reader = new FileReader();
          reader.onerror = () => reject(new Error("Could not read this image."));
          reader.onload = () => resolve(String(reader.result));
          reader.readAsDataURL(file);
        });
        const image = await new Promise<HTMLImageElement>((resolve, reject) => {
          const loaded = new Image();
          loaded.onload = () => resolve(loaded);
          loaded.onerror = () => reject(new Error("This image could not be opened."));
          loaded.src = dataUrl;
        });
        const scale = Math.min(1, 720 / Math.max(image.width, image.height));
        const canvas = document.createElement("canvas");
        canvas.width = Math.max(1, Math.round(image.width * scale));
        canvas.height = Math.max(1, Math.round(image.height * scale));
        const context = canvas.getContext("2d");
        if (!context) throw new Error("Could not process this image.");
        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        let resized = canvas.toDataURL("image/jpeg", 0.82);
        if (resized.length > 1_500_000) resized = canvas.toDataURL("image/jpeg", 0.62);
        if (resized.length > 1_500_000) throw new Error("This image is too large after resizing.");
        onChange(resized);
      } catch (error) {
        setImageError(error instanceof Error ? error.message : "Could not process this image.");
      }
    }
    return (
      <label className="field-control image-upload-control">
        <span>{field.label}{field.required ? <span className="required-mark"> *</span> : null}</span>
        <input accept="image/png,image/jpeg,image/webp" name={`${field.name}-upload`} onChange={(event) => void chooseImage(event.target.files?.[0])} type="file" />
        <small>PNG, JPEG, or WebP. Images are resized before saving.</small>
        {imageValue ? <div className="image-upload-preview"><img alt="Product image preview" src={imageValue} /><button className="text-button" onClick={() => onChange("")} type="button">Remove image</button></div> : null}
        {imageError ? <span className="form-error" role="alert">{imageError}</span> : null}
      </label>
    );
  }

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

  if (kind === "checkboxes") {
    const selected = Array.isArray(value) ? value : [];
    return (
      <fieldset className="navigation-permissions">
        <legend>{field.label}{field.required ? <span className="required-mark"> *</span> : null}</legend>
        <div className="navigation-permission-options">
          {(field.options ?? []).map((option) => (
            <label className="field-checkbox" key={option}>
              <input
                checked={selected.includes(option)}
                onChange={(event) => onChange(
                  event.target.checked
                    ? [...selected, option]
                    : selected.filter((item) => item !== option),
                )}
                type="checkbox"
              />
              <span>{humanize(option)}</span>
            </label>
          ))}
        </div>
      </fieldset>
    );
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
    <div className="field-control">
      <label htmlFor={`${field.name}-input`}>
        {field.label}
        {field.required ? <span className="required-mark"> *</span> : null}
      </label>
      {kind === "enum" ? (
        <select
          id={`${field.name}-input`}
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
          id={`${field.name}-input`}
          name={field.name}
          onChange={(event) => onChange(event.target.value)}
          required={field.required}
          step={kind === "number" ? "any" : undefined}
          type={kind}
          value={String(value)}
        />
      )}
      {isLocationField(field.name) && String(value ?? "").trim() ? (
        <LocationMapLink location={String(value)} />
      ) : null}
    </div>
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
