import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useFunnel, useOverview, usePurchasesByStatus } from "../hooks/usePortal";
import { FilterBar, type FilterState } from "../components/Filters";
import { Badge, Button, Card, ErrorMessage, Kpi, PageHeader, Spinner } from "../components/ui";
import { money, num, percent } from "../lib/format";
import { STATUS, SURFACE } from "../lib/theme";

const STATUS_TONE: Record<string, "green" | "red" | "amber" | "slate" | "blue"> = {
  SUCCESS: "green",
  FAILED: "red",
  CANCELLED: "amber",
  PENDING: "slate",
  CREATED: "slate",
  REQUIRES_VERIFICATION: "amber",
  RESTORED: "blue",
};

// Bar fill per purchase status, using the shared status palette so the color matches the badge tone.
const STATUS_BAR: Record<string, string> = {
  SUCCESS: STATUS.success,
  FAILED: STATUS.error,
  CANCELLED: STATUS.warning,
  PENDING: SURFACE.textMuted,
  CREATED: SURFACE.textMuted,
  REQUIRES_VERIFICATION: STATUS.warning,
  RESTORED: STATUS.info,
};

export default function AnalyticsPage() {
  const { appId = "" } = useParams();
  const [filters, setFilters] = useState<FilterState>({});
  const overview = useOverview(appId, filters);
  const funnel = useFunnel(appId, filters);
  const byStatus = usePurchasesByStatus(appId, filters);

  const o = overview.data;
  const statusRows = byStatus.data ?? [];
  const statusTotal = statusRows.reduce((sum, r) => sum + r.count, 0);

  return (
    <div>
      <PageHeader
        title="Analytics"
        subtitle="Default range: last 12 months. Use the date filters to zoom in."
        actions={
          <Link to={`/apps/${appId}/analytics/revenue`}>
            <Button variant="secondary">Revenue →</Button>
          </Link>
        }
      />

      <FilterBar appId={appId} value={filters} onChange={setFilters} />

      {overview.isLoading && <Spinner />}
      {overview.error && <ErrorMessage error={overview.error} />}

      <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <Kpi label="Popup opened" value={num(o?.popupShown)} />
        <Kpi label="Cancelled" value={num(o?.cancelClicked)} />
        <Kpi label="Started" value={num(o?.purchaseStarted)} />
        <Kpi label="Completed" value={num(o?.purchaseSuccess)} />
        <Kpi label="Failed" value={num(o?.purchaseFailed)} />
        <Kpi label="Conversion" value={percent(o?.popupToSuccessConversion)} sub="popup → success" />
        <Kpi label="Total revenue" value={money(o?.totalRevenueMinor, o?.currency)} />
        <Kpi label="Avg revenue" value={money(o?.averageRevenueMinor, o?.currency)} sub="per purchase" />
        <Kpi label="Active entitlements" value={num(o?.activeEntitlements)} />
        <Kpi label="Restores" value={num(o?.restorePurchasesCount)} />
      </div>

      <Card className="mb-6">
        <h2 className="mb-1 text-sm font-semibold text-slate-900">Purchases by status</h2>
        <p className="mb-4 text-xs text-slate-500">
          From the purchase records (authoritative), not events. {num(statusTotal)} total in range.
        </p>
        {byStatus.isLoading && <Spinner />}
        {byStatus.error && <ErrorMessage error={byStatus.error} />}
        {!byStatus.isLoading && statusRows.length === 0 && (
          <p className="text-sm text-slate-500">No purchases in this range.</p>
        )}
        <div className="space-y-2">
          {statusRows.map((r) => {
            const pct = statusTotal > 0 ? r.count / statusTotal : 0;
            return (
              <div key={r.status}>
                <div className="mb-1 flex items-center justify-between text-xs">
                  <Badge tone={STATUS_TONE[r.status] ?? "slate"}>{r.status}</Badge>
                  <span className="text-slate-500">
                    {num(r.count)} · {percent(pct)}
                  </span>
                </div>
                <div className="h-5 w-full rounded bg-slate-100">
                  <div
                    className="h-5 rounded"
                    style={{
                      width: `${Math.max(2, pct * 100)}%`,
                      backgroundColor: STATUS_BAR[r.status] ?? STATUS.info,
                    }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </Card>

      <Card>
        <h2 className="mb-4 text-sm font-semibold text-slate-900">Purchase funnel</h2>
        {funnel.isLoading && <Spinner />}
        {funnel.data && (
          <div className="space-y-2">
            {funnel.data.mainFunnel.map((step) => (
              <div key={step.step}>
                <div className="mb-1 flex justify-between text-xs text-slate-500">
                  <span className="font-mono">{step.step}</span>
                  <span>
                    {num(step.count)} · {percent(step.percentOfTop)} of top · {percent(step.percentOfPrev)} of prev
                  </span>
                </div>
                <div className="h-6 w-full rounded bg-slate-100">
                  <div
                    className="h-6 rounded bg-primary"
                    style={{ width: `${Math.max(2, step.percentOfTop * 100)}%` }}
                  />
                </div>
              </div>
            ))}
            <div className="mt-4 border-t border-slate-100 pt-3">
              <div className="mb-1 flex justify-between text-xs text-slate-500">
                <span className="font-mono">{funnel.data.cancelBranch.step} (branch)</span>
                <span>
                  {num(funnel.data.cancelBranch.count)} · {percent(funnel.data.cancelBranch.percentOfTop)} of popups
                </span>
              </div>
              <div className="h-6 w-full rounded bg-amber-50">
                <div
                  className="h-6 rounded"
                  style={{
                    width: `${Math.max(2, funnel.data.cancelBranch.percentOfTop * 100)}%`,
                    backgroundColor: STATUS.warning,
                  }}
                />
              </div>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
