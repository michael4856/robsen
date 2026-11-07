import { BUSINESS_TYPE_OPTIONS } from "@/app/lib/resources";

export default function BusinessTypeCheckboxes({ selected = [] }: { selected?: string[] }) {
  return (
    <fieldset className="business-type-options">
      <legend>Business types</legend>
      <div className="navigation-permission-options">
        {BUSINESS_TYPE_OPTIONS.map((type) => (
          <label className="field-checkbox" key={type}>
            <input
              defaultChecked={selected.includes(type)}
              name="businessTypes"
              type="checkbox"
              value={type}
            />
            <span>{type.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase())}</span>
          </label>
        ))}
      </div>
      <small>Select every type that applies.</small>
    </fieldset>
  );
}
