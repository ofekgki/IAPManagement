import { Link, useParams } from "react-router-dom";
import { useApiKeys, useApp, useEvents, useItems, useOverview, usePurchases, useUpdateApp } from "../hooks/usePortal";
import { Badge, Button, Card, ErrorMessage, Kpi, PageHeader, Spinner } from "../components/ui";
import { dateTime, money, num, percent } from "../lib/format";

export default function DashboardPage() {
  const { appId = "" } = useParams();
  const app = useApp(appId);
  const updateApp = useUpdateApp(appId);
  const items = useItems(appId);
  const keys = useApiKeys(appId);
  const overview = useOverview(appId, {});
  // Only the 6 most recent purchases are shown here — fetch exactly one small page.
  const purchases = usePurchases(appId, {}, 0, 6);
  const events = useEvents(appId, {});

  if (app.isLoading) return <Spinner label="Loading dashboard…" />;
  if (app.error) return <ErrorMessage error={app.error} />;

  const hasActiveKey = (keys.data ?? []).some((k) => k.status === "ACTIVE");
  const itemCount = items.data?.length ?? 0;
  const o = overview.data;
  // "Has any successful purchase" comes from the overview counters (index-only on the backend)
  // instead of downloading the purchase list.
  const successCount = o?.purchaseSuccess ?? 0;

  const checklist = [
    { label: "Create API key", done: hasActiveKey, to: `/apps/${appId}/api-keys` },
    { label: "Create first item", done: itemCount > 0, to: `/apps/${appId}/items/new` },
    { label: "Copy SDK init snippet", done: false, to: `/apps/${appId}/sdk-setup` },
    { label: "Test mock purchase", done: successCount > 0, to: `/apps/${appId}/sdk-setup` },
    { label: "Check analytics", done: (o?.popupShown ?? 0) > 0, to: `/apps/${appId}/analytics` },
  ];

  return (
    <div>
      <PageHeader
        title={app.data!.appName}
        subtitle={app.data!.packageName}
        actions={
          <div className="flex items-center gap-2">
            <Badge tone="blue">Default: {app.data!.defaultBillingMode}</Badge>
            <Badge tone={app.data!.isActive ? "green" : "red"}>{app.data!.isActive ? "Active" : "Disabled"}</Badge>
            <Button
              variant={app.data!.isActive ? "danger" : "primary"}
              disabled={updateApp.isPending}
              onClick={() => {
                const next = !app.data!.isActive;
                const msg = next
                  ? "Re-enable this app? The SDK will be able to authenticate again."
                  : "Disable this app? SDK requests with its API keys will be rejected (APP_DISABLED). Data is kept.";
                if (window.confirm(msg)) updateApp.mutate({ isActive: next });
              }}
            >
              {updateApp.isPending ? "Saving…" : app.data!.isActive ? "Disable app" : "Enable app"}
            </Button>
          </div>
        }
      />

      <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi label="API key" value={hasActiveKey ? "Active" : "None"} />
        <Kpi label="Items" value={num(itemCount)} />
        <Kpi label="Total purchases" value={num(o?.purchaseSuccess)} sub="successful" />
        <Kpi label="Total revenue" value={money(o?.totalRevenueMinor, o?.currency)} />
        <Kpi label="Popup → success" value={percent(o?.popupToSuccessConversion)} />
        <Kpi label="Active entitlements" value={num(o?.activeEntitlements)} />
        <Kpi label="Cancelled" value={num(o?.cancelClicked)} />
        <Kpi label="Failed" value={num(o?.purchaseFailed)} />
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Setup checklist</h2>
          <ul className="space-y-2">
            {checklist.map((c) => (
              <li key={c.label} className="flex items-center justify-between text-sm">
                <span className="flex items-center gap-2">
                  <span className={c.done ? "text-green-600" : "text-slate-300"}>{c.done ? "✓" : "○"}</span>
                  {c.label}
                </span>
                <Link to={c.to} className="text-xs text-ink hover:underline">
                  Go
                </Link>
              </li>
            ))}
          </ul>
        </Card>

        <Card className="lg:col-span-2">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900">Recent purchases</h2>
            <Link to={`/apps/${appId}/purchases`} className="text-xs text-ink hover:underline">
              View all
            </Link>
          </div>
          {(purchases.data?.items ?? []).map((p) => (
            <div key={p.purchaseId} className="flex items-center justify-between border-b border-slate-100 py-2 text-sm last:border-0">
              <span className="font-mono text-xs text-slate-500">{p.userId}</span>
              <span>{p.itemId}</span>
              <Badge tone={p.status === "SUCCESS" ? "green" : p.status === "FAILED" ? "red" : "slate"}>{p.status}</Badge>
              <span className="text-slate-500">{money(p.revenueMinor, p.currency ?? undefined)}</span>
            </div>
          ))}
          {(purchases.data?.items ?? []).length === 0 && <p className="text-sm text-slate-500">No purchases yet.</p>}
        </Card>
      </div>

      <Card className="mt-6">
        <h2 className="mb-3 text-sm font-semibold text-slate-900">Recent analytics events</h2>
        <div className="space-y-1">
          {(events.data ?? []).slice(0, 8).map((e) => (
            <div key={e.id} className="flex items-center justify-between text-sm">
              <span className="font-mono text-xs">{e.eventName}</span>
              <span className="text-slate-500">{e.itemId ?? "—"}</span>
              <span className="text-xs text-slate-400">{dateTime(e.createdAt)}</span>
            </div>
          ))}
          {(events.data ?? []).length === 0 && <p className="text-sm text-slate-500">No events yet.</p>}
        </div>
      </Card>
    </div>
  );
}
