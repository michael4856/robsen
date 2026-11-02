import type { ResourceDefinition } from "./types";

export const tenantsResource: ResourceDefinition = {
    key: "tenants",
    label: "Tenants",
    singular: "tenant",
    endpoint: "tenants",
    listEndpoint: "tenants",
    idField: "tenantId",
    description: "Manage tenant workspaces and their activation state.",
    adminOnly: true,
    deleteActionLabel: "Deactivate",
    columns: ["tenantName", "location", "description", "active"],
    fields: [
      { name: "tenantName", label: "Name", required: true },
      { name: "description", label: "Description" },
      { name: "location", label: "Location" },
      { name: "active", label: "Active", kind: "boolean" },
    ],
  };
