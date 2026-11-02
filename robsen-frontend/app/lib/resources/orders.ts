import type { ResourceDefinition } from "./types";

export const ordersResource: ResourceDefinition = {
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
  };
export const orderItemsResource: ResourceDefinition = {
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
  };
