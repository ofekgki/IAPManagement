import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { usePurchases } from "../hooks/usePortal";
import { FilterBar, type FilterState } from "../components/Filters";
import { Badge, Card, ErrorMessage, Input, PageHeader, Select, Spinner } from "../components/ui";
import { dateTime, money } from "../lib/format";
import { paymentMethodLabel, type PurchaseStatus } from "../types";

const STATUSES: PurchaseStatus[] = ["CREATED", "PENDING", "SUCCESS", "FAILED", "CANCELLED", "REQUIRES_VERIFICATION", "RESTORED"];

const PAGE_SIZE = 50;

export default function PurchasesPage() {
  const { appId = "" } = useParams();
  const [filters, setFilters] = useState<FilterState & { status?: string; userId?: string }>({});
  const [page, setPage] = useState(0);
  const purchases = usePurchases(appId, filters, page, PAGE_SIZE);
  // Changing any filter restarts from the first page.
  const applyFilters = (f: typeof filters) => {
    setFilters(f);
    setPage(0);
  };

  const rows = purchases.data?.items ?? [];
  const totalPages = purchases.data?.totalPages ?? 1;
  const totalItems = purchases.data?.totalItems ?? 0;

  return (
    <div>
      <PageHeader title="Purchases" subtitle="All purchase attempts recorded by the backend." />

      <FilterBar appId={appId} value={filters} onChange={(f) => applyFilters({ ...filters, ...f })} />
      <div className="mb-4 flex flex-wrap gap-2">
        <Select
          className="w-auto"
          value={filters.status ?? ""}
          onChange={(e) => applyFilters({ ...filters, status: e.target.value || undefined })}
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </Select>
        <Input
          className="w-48"
          placeholder="User ID"
          value={filters.userId ?? ""}
          onChange={(e) => applyFilters({ ...filters, userId: e.target.value || undefined })}
        />
      </div>

      {purchases.isLoading && <Spinner />}
      {purchases.error && <ErrorMessage error={purchases.error} />}

      <Card className="overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">Purchase ID</th>
              <th className="px-4 py-3">User</th>
              <th className="px-4 py-3">Item</th>
              <th className="px-4 py-3">Payment</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Revenue</th>
              <th className="px-4 py-3">Created</th>
              <th className="px-4 py-3">Completed</th>
              <th className="px-4 py-3">Provider</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((p) => (
              <tr key={p.purchaseId} className="border-b border-slate-100 last:border-0">
                <td className="px-4 py-3 font-mono text-xs">{p.purchaseId}</td>
                <td className="px-4 py-3 font-mono text-xs">{p.userId}</td>
                <td className="px-4 py-3">{p.itemId}</td>
                <td className="px-4 py-3">{paymentMethodLabel(p.paymentMethod)}</td>
                <td className="px-4 py-3">
                  <Badge
                    tone={
                      p.status === "SUCCESS"
                        ? "green"
                        : p.status === "FAILED"
                          ? "red"
                          : p.status === "RESTORED"
                            ? "blue"
                            : "slate"
                    }
                  >
                    {p.status}
                  </Badge>
                </td>
                <td className="px-4 py-3">{money(p.revenueMinor, p.currency ?? undefined)}</td>
                <td className="px-4 py-3 text-slate-500">{dateTime(p.createdAt)}</td>
                <td className="px-4 py-3 text-slate-500">{dateTime(p.completedAt)}</td>
                <td className="px-4 py-3">{p.provider}</td>
                <td className="px-4 py-3 text-right">
                  <Link to={`/apps/${appId}/purchases/${p.purchaseId}`} className="text-xs text-ink hover:underline">
                    View
                  </Link>
                </td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={10} className="px-4 py-6 text-center text-slate-500">
                  No purchases match these filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>

      <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
        <span>
          {totalItems.toLocaleString()} purchase{totalItems === 1 ? "" : "s"} · page {page + 1} of {totalPages}
        </span>
        <div className="flex gap-2">
          <button
            className="rounded-lg border border-border bg-card px-3 py-1.5 font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            disabled={page <= 0 || purchases.isLoading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Previous
          </button>
          <button
            className="rounded-lg border border-border bg-card px-3 py-1.5 font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
            disabled={page >= totalPages - 1 || purchases.isLoading}
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}
