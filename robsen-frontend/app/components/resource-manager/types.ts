export type RecordValue = Record<string, unknown>;
export type FormValue = string | boolean | string[];
export type BusinessContextOption = {
  id: number; legalName?: string; tradeName?: string | null;
  canCreateProducts?: boolean; canUpdateProducts?: boolean;
  canReadProductListings?: boolean; canReadProductCategories?: boolean;
  canCreateProductCategories?: boolean; canUpdateProductCategories?: boolean;
};
export type DropdownOption = { value: string; label: string };
export type DropdownSource = {
  endpoint: string;
  asRelation?: boolean;
  transform?: (payload: unknown) => DropdownOption[];
};
