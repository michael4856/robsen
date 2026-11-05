export * from "./resources/types";
export { BUSINESS_TYPE_OPTIONS } from "./resources/core";
import { businessesResource, membershipsResource, rolesResource, usersResource } from "./resources/core";
import { productCategoriesResource, productListingsResource, productsResource } from "./resources/catalog";
import { agreementsResource, opportunitiesResource, partnerApplicationsResource, territoriesResource } from "./resources/distribution";
import { orderItemsResource, ordersResource } from "./resources/orders";
import { tenantsResource } from "./resources/tenants";
import type { ResourceDefinition } from "./resources/types";

export const resources: ResourceDefinition[] = [
  businessesResource,
  usersResource,
  membershipsResource,
  rolesResource,
  productCategoriesResource,
  productsResource,
  productListingsResource,
  territoriesResource,
  opportunitiesResource,
  partnerApplicationsResource,
  agreementsResource,
  ordersResource,
  orderItemsResource,
  tenantsResource,
];

export function findResource(key: string) {
  return resources.find((resource) => resource.key === key);
}
