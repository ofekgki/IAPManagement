import { useItems } from "../hooks/usePortal";
import { PAYMENT_METHODS } from "../types";
import { Select } from "./ui";

export type FilterState = {
  from?: string;
  to?: string;
  itemId?: string;
  paymentMethod?: string;
  groupBy?: string;
};

export function FilterBar({
  appId,
  value,
  onChange,
  showItem = true,
  showMethod = true,
  showGroupBy = false,
}: {
  appId: string;
  value: FilterState;
  onChange: (next: FilterState) => void;
  showItem?: boolean;
  showMethod?: boolean;
  showGroupBy?: boolean;
}) {
  const items = useItems(appId);
  const set = (patch: Partial<FilterState>) => onChange({ ...value, ...patch });
  const input = "rounded-lg border border-border bg-card px-3 py-1.5 text-sm";

  return (
    <div className="mb-4 flex flex-wrap items-center gap-2">
      <input
        type="date"
        className={input}
        value={value.from ?? ""}
        onChange={(e) => set({ from: e.target.value || undefined })}
      />
      <span className="text-slate-400">→</span>
      <input
        type="date"
        className={input}
        value={value.to ?? ""}
        onChange={(e) => set({ to: e.target.value || undefined })}
      />
      {showItem && (
        <Select
          className="w-auto"
          value={value.itemId ?? ""}
          onChange={(e) => set({ itemId: e.target.value || undefined })}
        >
          <option value="">All items</option>
          {(items.data ?? []).map((it) => (
            <option key={it.itemId} value={it.itemId}>
              {it.name}
            </option>
          ))}
        </Select>
      )}
      {showMethod && (
        <Select
          className="w-auto"
          value={value.paymentMethod ?? ""}
          onChange={(e) => set({ paymentMethod: e.target.value || undefined })}
        >
          <option value="">All payment methods</option>
          {PAYMENT_METHODS.map((m) => (
            <option key={m.value} value={m.value}>
              {m.label}
            </option>
          ))}
        </Select>
      )}
      {showGroupBy && (
        <Select
          className="w-auto"
          value={value.groupBy ?? "day"}
          onChange={(e) => set({ groupBy: e.target.value })}
        >
          <option value="day">By day</option>
          <option value="week">By week</option>
          <option value="month">By month</option>
        </Select>
      )}
      <button
        className="text-xs text-slate-500 underline"
        onClick={() => onChange({ groupBy: value.groupBy })}
      >
        Reset range
      </button>
    </div>
  );
}
