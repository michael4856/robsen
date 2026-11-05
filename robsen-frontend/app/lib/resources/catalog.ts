import type { ResourceDefinition } from "./types";

export const productCategoriesResource: ResourceDefinition = {
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
  };
export const productsResource: ResourceDefinition = {
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
      { name: "imageUrl", label: "Product image", kind: "image" },
      { name: "price", label: "Price", kind: "number", required: true },
      { name: "active", label: "Active", kind: "boolean" },
    ],
  };
export const productListingsResource: ResourceDefinition = {
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
  };
