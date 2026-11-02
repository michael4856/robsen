import type { ResourceDefinition } from "./types";

export const territoriesResource: ResourceDefinition = {
    key: "territories",
    label: "Territories",
    singular: "territory",
    endpoint: "territories",
    listEndpoint: "territories",
    description: "Maintain the regions and cities where partners operate.",
    columns: ["business", "name", "region", "city", "description"],
    fields: [
      { name: "business", label: "Business", kind: "relation", endpoint: "business/all", required: true },
      { name: "name", label: "Name", required: true },
      { name: "region", label: "Region" },
      { name: "city", label: "City" },
      { name: "description", label: "Description" },
    ],
  };
export const opportunitiesResource: ResourceDefinition = {
    key: "opportunities",
    label: "Distribution opportunities",
    singular: "opportunity",
    endpoint: "distribution-opportunities",
    listEndpoint: "distribution-opportunities",
    description: "Publish opportunities for partners to discover and apply to.",
    deleteActionLabel: "Close",
    columns: ["title", "business", "territory", "status", "expiresAt"],
    fields: [
      { name: "business", label: "Business", kind: "relation", endpoint: "business/all" },
      { name: "title", label: "Title", required: true },
      { name: "description", label: "Description" },
      { name: "imageUrl", label: "Cover image URL", kind: "text" },
      { name: "territory", label: "Territory", kind: "relation", endpoint: "territories" },
      { name: "requirements", label: "Requirements" },
      {
        name: "status",
        label: "Status",
        kind: "enum",
        options: ["DRAFT", "OPEN", "CLOSED", "EXPIRED", "CANCELLED"],
      },
      { name: "expiresAt", label: "Expires at", kind: "datetime-local" },
    ],
  };
export const partnerApplicationsResource: ResourceDefinition = {
    key: "partner-applications",
    label: "Partner applications",
    singular: "partner application",
    endpoint: "partner-applications",
    listEndpoint: "partner-applications",
    description: "Review applications submitted for distribution opportunities.",
    columns: ["opportunity", "applicantBusiness", "status", "reviewedAt"],
    deletable: false,
    fields: [
      { name: "opportunity", label: "Opportunity", kind: "relation", endpoint: "distribution-opportunities", required: true },
      { name: "applicantBusiness", label: "Applicant business", kind: "relation", endpoint: "business/all", required: true },
      { name: "message", label: "Message" },
      {
        name: "status",
        label: "Status",
        kind: "enum",
        options: ["PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED", "WITHDRAWN"],
        updateOnly: true,
      },
      { name: "reviewedAt", label: "Reviewed at", kind: "datetime-local", updateOnly: true },
    ],
  };
export const agreementsResource: ResourceDefinition = {
    key: "agreements",
    label: "Distribution agreements",
    singular: "agreement",
    endpoint: "distribution-agreements",
    listEndpoint: "distribution-agreements",
    description: "Track partner agreements, coverage, terms, and lifecycle.",
    deleteActionLabel: "Terminate",
    columns: ["partnerBusiness", "territory", "startDate", "endDate", "status"],
    fields: [
      { name: "partnerBusiness", label: "Partner business", kind: "relation", endpoint: "business/all", required: true },
      { name: "territory", label: "Territory", kind: "relation", endpoint: "territories" },
      { name: "startDate", label: "Start date", kind: "date", required: true },
      { name: "endDate", label: "End date", kind: "date" },
      {
        name: "status",
        label: "Status",
        kind: "enum",
        options: ["DRAFT", "PENDING_APPROVAL", "ACTIVE", "EXPIRED", "TERMINATED", "REJECTED"],
      },
      { name: "terms", label: "Terms" },
    ],
  };
