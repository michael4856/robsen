export type FieldKind =
  | "text"
  | "image"
  | "email"
  | "password"
  | "number"
  | "date"
  | "datetime-local"
  | "boolean"
  | "relation"
  | "enum"
  | "role"
  | "status"
  | "navigation"
  | "permissions"
  | "checkboxes";

export type FieldDefinition = {
  name: string;
  label: string;
  kind?: FieldKind;
  required?: boolean;
  options?: string[];
  /** Required when kind === "relation": the endpoint used to load dropdown options. */
  endpoint?: string;
  createOnly?: boolean;
  updateOnly?: boolean;
  adminOnly?: boolean;
};

export type ResourceDefinition = {
  key: string;
  label: string;
  singular: string;
  endpoint: string;
  listEndpoint: string;
  idField?: string;
  description: string;
  columns: string[];
  fields: FieldDefinition[];
  deletable?: boolean;
  adminOnly?: boolean;
  deleteActionLabel?: string;
};
