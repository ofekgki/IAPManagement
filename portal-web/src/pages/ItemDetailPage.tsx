import { useState, type FormEvent, type ReactNode } from "react";
import { Link, useParams } from "react-router-dom";
import { useEntitlements, useItem, usePurchases, useRevenueByProduct, useSetItemActive, useUpdateItem } from "../hooks/usePortal";
import { Alert, Badge, Button, Card, CodeBlock, CopyButton, ErrorMessage, Field, Input, Kpi, PageHeader, Spinner } from "../components/ui";
import { money, num, percent } from "../lib/format";
import type { Item } from "../types";

export default function ItemDetailPage() {
  const { appId = "", itemId = "" } = useParams();
  const item = useItem(appId, itemId);
  const byProduct = useRevenueByProduct(appId, {});
  // Only the 8 most recent purchases for this item are shown — fetch exactly one small page.
  const purchases = usePurchases(appId, { itemId }, 0, 8);
  const entitlements = useEntitlements(appId, { itemId });
  const setActive = useSetItemActive(appId);

  if (item.isLoading) return <Spinner />;
  if (item.error) return <ErrorMessage error={item.error} />;
  const it = item.data!;
  const stats = (byProduct.data ?? []).find((r) => r.itemId === itemId);

  const snippet = `PurchaseSdk.getItem("${it.itemId}")

PurchaseSdk.showPurchasePopup(
    activity = this,
    itemId = "${it.itemId}",
    userId = currentUser.id
)`;

  return (
    <div>
      <Link to={`/apps/${appId}/items`} className="text-sm text-slate-500 hover:text-slate-800">
        ← Items
      </Link>
      <PageHeader
        title={it.name}
        subtitle={it.description ?? undefined}
        actions={
          <div className="flex gap-2">
            {it.isActive ? <Badge tone="green">Active</Badge> : <Badge tone="slate">Disabled</Badge>}
            <Button variant="secondary" onClick={() => setActive.mutate({ itemId, active: !it.isActive })}>
              {it.isActive ? "Disable" : "Enable"}
            </Button>
          </div>
        }
      />

      <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Kpi label="Successful purchases" value={num(stats?.successfulPurchases)} />
        <Kpi label="Revenue" value={money(stats?.totalRevenueMinor, it.currency ?? "USD")} />
        <Kpi label="Conversion" value={percent(stats?.conversionRate)} sub="popup → success" />
        <Kpi label="Entitlements granted" value={num(entitlements.data?.length)} />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Item metadata</h2>
          <dl className="space-y-2 text-sm">
            <Row label="Item ID" value={<Mono value={it.itemId} />} />
            <Row label="Type" value={it.type} />
            <Row label="Entitlement ID" value={it.entitlementId ? <Mono value={it.entitlementId} /> : "—"} />
            <Row label="Price" value={`${it.priceDisplay ?? "—"} (${it.priceAmountMinor ?? 0} minor)`} />
            <Row label="Currency" value={it.currency ?? "—"} />
            <Row label="Google Play product" value={it.googlePlayProductId ?? "—"} />
          </dl>
          <PriceEditor appId={appId} item={it} />
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">SDK usage</h2>
          <CodeBlock code={snippet} />
          <Alert tone="info">
            <p className="mb-1">
              For <strong>MOCK</strong> mode, this item can be purchased without Google Play setup.
            </p>
            <p>
              For <strong>GOOGLE_PLAY</strong> mode, this item must be connected to a real Google Play product ID.
            </p>
          </Alert>
        </Card>
      </div>

      <Card className="mt-6">
        <h2 className="mb-3 text-sm font-semibold text-slate-900">Recent purchases</h2>
        {(purchases.data?.items ?? []).map((p) => (
          <div key={p.purchaseId} className="flex items-center justify-between border-b border-slate-100 py-2 text-sm last:border-0">
            <span className="font-mono text-xs text-slate-500">{p.userId}</span>
            <Badge tone={p.status === "SUCCESS" ? "green" : "slate"}>{p.status}</Badge>
            <Link to={`/apps/${appId}/purchases/${p.purchaseId}`} className="text-xs text-ink hover:underline">
              View
            </Link>
          </div>
        ))}
        {(purchases.data?.items ?? []).length === 0 && <p className="text-sm text-slate-500">No purchases yet.</p>}
      </Card>
    </div>
  );
}

/** Inline editor for an item's price. Updates priceAmountMinor + a formatted priceDisplay together. */
function PriceEditor({ appId, item }: { appId: string; item: Item }) {
  const update = useUpdateItem(appId, item.id);
  const currency = item.currency ?? "USD";
  // Edit in major units (dollars) for a friendly input; convert to minor units on save.
  const [amount, setAmount] = useState(((item.priceAmountMinor ?? 0) / 100).toFixed(2));
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  async function save(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSaved(false);
    const dollars = Number(amount);
    if (!Number.isFinite(dollars) || dollars < 0) {
      return setError("Enter a valid, non-negative price.");
    }
    const minor = Math.round(dollars * 100);
    try {
      await update.mutateAsync({
        priceAmountMinor: minor,
        priceDisplay: money(minor, currency),
      });
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not update price.");
    }
  }

  return (
    <form onSubmit={save} className="mt-4 border-t border-border pt-4">
      <Field label={`Edit price (${currency})`} hint="Applies to future purchases; past revenue keeps its snapshot.">
        <div className="flex gap-2">
          <Input
            type="number"
            step="0.01"
            min="0"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="w-40"
          />
          <Button type="submit" disabled={update.isPending}>
            {update.isPending ? "Saving…" : "Save price"}
          </Button>
        </div>
      </Field>
      {error && <div className="mt-2"><Alert tone="error">{error}</Alert></div>}
      {saved && <div className="mt-2"><Alert tone="success">Price updated.</Alert></div>}
    </form>
  );
}

function Row({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}

function Mono({ value }: { value: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span className="font-mono text-xs">{value}</span>
      <CopyButton value={value} />
    </span>
  );
}
