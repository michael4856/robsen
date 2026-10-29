export type FieldKind =
  | "text"
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
  | "permissions";

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

export const BUSINESS_TYPE_OPTIONS = [
  "MANUFACTURER",
  "WHOLESALER",
  "DISTRIBUTOR",
  "RETAILER",
  "IMPORTER",
  "EXPORTER",
  "SERVICE_PROVIDER",
  "COOPERATIVE",
  "OTHER",
];

export const resources: ResourceDefinition[] = [
  {
    key: "businesses",
    label: "Businesses",
    singular: "business",
    endpoint: "business",
    listEndpoint: "business/all",
    description: "Manage registered businesses and their verification status.",
    deleteActionLabel: "Deactivate",
    columns: ["legalName", "tradeName", "email", "businessType", "verificationStatus"],
    fields: [
      { name: "legalName", label: "Legal name", required: true },
      { name: "tradeName", label: "Trading name" },
      { name: "description", label: "Business description" },
      { name: "tinNumber", label: "Tax ID" },
      { name: "address", label: "Address" },
      { name: "email", label: "Email", kind: "email" },
      { name: "businessType", label: "Business type", kind: "enum", options: BUSINESS_TYPE_OPTIONS },
      { name: "phoneNumber", label: "Phone" },
      {
        name: "verificationStatus",
        label: "Verification status",
        kind: "enum",
        options: ["PENDING", "COMPLETED", "APPROVED", "REJECTED", "CONFIRMED", "CANCELLED"],
        adminOnly: true,
      },
      { name: "active", label: "Active", kind: "boolean", adminOnly: true },
    ],
  },
  {
    key: "users",
    label: "Users",
    singular: "user",
    endpoint: "users",
    listEndpoint: "users/all",
    description: "Manage Robsen accounts and their access roles.",
    adminOnly: true,
    deleteActionLabel: "Deactivate",
    columns: ["fullName", "email", "username", "role", "active"],
    fields: [
      { name: "fullName", label: "Full name", required: true },
      { name: "email", label: "Email", kind: "email", required: true },
      { name: "username", label: "Username", required: true },
      { name: "password", label: "Password", kind: "password", required: true, createOnly: true },
      { name: "password", label: "New password (leave blank to keep current)", kind: "password", updateOnly: true },
      { name: "phoneNumber", label: "Phone" },
      { name: "role", label: "Platform access", kind: "enum", options: ["USER", "SUPER_ADMIN"], required: true },
      { name: "active", label: "Active", kind: "boolean" },
    ],
  },
  {
    key: "memberships",
    label: "Business memberships",
    singular: "membership",
    endpoint: "business-memberships",
    listEndpoint: "business-memberships",
    description: "Connect users to businesses with the appropriate role.",
    deleteActionLabel: "Remove",
    columns: ["id", "user", "business", "role", "status"],
    fields: [
      { name: "user", label: "User", kind: "relation", endpoint: "users/all", required: true },
      { name: "business", label: "Business", kind: "relation", endpoint: "business/all", required: true },
      { name: "role", label: "Role", kind: "role", required: true },
      {
        name: "status",
        label: "Status",
        kind: "enum",
        options: ["INVITED", "ACTIVE", "SUSPENDED", "REMOVED"],
      },
    ],
  },
  {
    key: "roles",
    label: "Roles",
    singular: "role",
    endpoint: "roles",
    listEndpoint: "roles/all",
    description: "Create business roles and configure their resource-level permissions.",
    adminOnly: true,
    columns: ["name", "description", "permissions", "active"],
    fields: [
      { name: "name", label: "Name", required: true },
      { name: "description", label: "Description" },
      { name: "permissions", label: "Permissions", kind: "permissions" },
      { name: "active", label: "Active", kind: "boolean" },
    ],
  },
  {
    key: "product-categories",
    label: "Product categories",
    singular: "product category",
    endpoint: "product-categories",
    listEndpoint: "product-categories",
    description: "Organize products into searchable categories.",
    columns: ["name", "description", "active"],
    fields: [
      { name: "name", label: "Name", required: true },
      { name: "description", label: "Description" },
      { name: "active", label: "Active", kind: "boolean" },
    ],
  },
  {
    key: "products",
    label: "Products",
    singular: "product",
    endpoint: "products",
    listEndpoint: "products",
    description: "Manage the products available across the network.",
    columns: ["name", "productCategory", "price", "active"],
    fields: [
      { name: "name", label: "Name", required: true },
      {
        name: "productCategory",
        label: "Category",
        kind: "relation",
        endpoint: "product-categories",
      },
      { name: "description", label: "Description" },
      { name: "imageUrl", label: "Image URL", kind: "text" },
      { name: "price", label: "Price", kind: "number", required: true },
      { name: "active", label: "Active", kind: "boolean" },
    ],
  },
  {
    key: "product-listings",
    label: "Product listings",
    singular: "product listing",
    endpoint: "product-listings",
    listEndpoint: "product-listings",
    description: "List products for businesses with wholesale and stock details.",
    deleteActionLabel: "Deactivate",
    columns: ["business", "product", "availableQuantity", "wholesalePrice", "active"],
    fields: [
      { name: "business", label: "Business", kind: "relation", endpoint: "business/all", required: true },
      { name: "product", label: "Product", kind: "relation", endpoint: "products", required: true },
      { name: "availableQuantity", label: "Available quantity", kind: "number" },
      { name: "minimumOrderQuantity", label: "Minimum order quantity", kind: "number", required: true },
      { name: "wholesalePrice", label: "Wholesale price", kind: "number" },
      { name: "description", label: "Description" },
      { name: "active", label: "Active", kind: "boolean" },
    ],
  },
  {
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
  },
  {
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
  },
  {
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
  },
  {
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
  },
  {
    key: "orders",
    label: "Orders",
    singular: "order",
    endpoint: "orders",
    listEndpoint: "orders",
    description: "Follow customer orders between buyer and supplier businesses.",
    deleteActionLabel: "Cancel",
    columns: ["orderNumber", "buyerBusiness", "supplierBusiness", "status", "subtotal"],
    fields: [
      { name: "buyerBusiness", label: "Buyer business", kind: "relation", endpoint: "business/all", required: true },
      { name: "supplierBusiness", label: "Supplier business", kind: "relation", endpoint: "business/all", required: true },
      { name: "orderNumber", label: "Order number", required: true },
      {
        name: "status",
        label: "Status",
        kind: "enum",
        options: ["PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"],
      },
      { name: "subtotal", label: "Subtotal", kind: "number" },
    ],
  },
  {
    key: "order-items",
    label: "Order items",
    singular: "order item",
    endpoint: "order-items",
    listEndpoint: "order-items",
    description: "Manage the product listing quantities and prices within orders.",
    columns: ["order", "productListing", "quantity", "unitPrice", "lineTotal"],
    fields: [
      { name: "order", label: "Order", kind: "relation", endpoint: "orders", required: true },
      { name: "productListing", label: "Product listing", kind: "relation", endpoint: "product-listings", required: true },
      { name: "quantity", label: "Quantity", kind: "number", required: true },
      { name: "unitPrice", label: "Unit price", kind: "number", required: true },
    ],
  },
  {
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
  },
];

export function findResource(key: string) {
  return resources.find((resource) => resource.key === key);
}
