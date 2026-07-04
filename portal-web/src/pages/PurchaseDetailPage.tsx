import { Link, useParams } from "react-router-dom";
import { usePurchase } from "../hooks/usePortal";
import { Alert, Badge, Card, ErrorMessage, PageHeader, Spinner } from "../components/ui";
import { dateTime, money } from "../lib/format";
import { paymentMethodLabel } from "../types";

export default function PurchaseDetailPage() {
  const { appId = "", purchaseId = "" } = useParams();
  const detail = usePurchase(appId, purchaseId);

  if (detail.isLoading) return <Spinner />;
  if (detail.error) return <ErrorMessage error={detail.error} />;
  const d = detail.data!;
  const p = d.purchase;

  return (
    <div>
      <Link to={`/apps/${appId}/purchases`} className="text-sm text-slate-500 hover:text-slate-800">
        ← Purchases
      </Link>
      <PageHeader
        title="Purchase detail"
        subtitle={p.purchaseId}
        actions={
          <Badge
            tone={p.status === "SUCCESS" ? "green" : p.status === "FAILED" ? "red" : p.status === "RESTORED" ? "blue" : "slate"}
          >
            {p.status}
          </Badge>
        }
      />

      <PurchaseStatusNote status={p.status} failureCode={p.failureCode} failureMessage={d.failureMessage} />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Purchase</h2>
          <dl className="space-y-2 text-sm">
            <Row label="User ID" value={p.userId} />
            <Row label="Item" value={`${d.itemName ?? p.itemId} (${p.itemId})`} />
            <Row label="Item type" value={d.itemType ?? "—"} />
            <Row label="Payment method" value={paymentMethodLabel(p.paymentMethod)} />
            <Row label="Provider" value={p.provider} />
            <Row label="Provider order ID" value={d.providerOrderId ?? "—"} />
            <Row label="Original price" value={money(p.originalPriceMinor, p.currency ?? undefined)} />
            <Row
              label="Revenue"
              value={
                p.status === "RESTORED"
                  ? `${money(0, p.currency ?? undefined)} (restored — no new revenue)`
                  : money(p.revenueMinor, p.currency ?? undefined)
              }
            />
            <Row label="Created" value={dateTime(p.createdAt)} />
            <Row label="Completed" value={dateTime(p.completedAt)} />
          </dl>
          {p.status === "FAILED" || p.failureCode ? (
            <div className="mt-3">
              <Alert tone="error">
                {p.failureCode}: {d.failureMessage ?? "—"}
              </Alert>
            </div>
          ) : null}
          <p className="mt-3 text-xs text-slate-400">
            {/* Provider purchase tokens are never shown here — only a shortened/hashed value is logged server-side. */}
            Note: raw Google purchase tokens are never displayed.
          </p>
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Entitlement granted</h2>
          {d.entitlement ? (
            <dl className="space-y-2 text-sm">
              <Row label="Entitlement ID" value={d.entitlement.entitlementId} />
              <Row label="Status" value={d.entitlement.status} />
              <Row label="Expires" value={dateTime(d.entitlement.expiresAt)} />
            </dl>
          ) : (
            <p className="text-sm text-slate-500">No entitlement granted for this purchase.</p>
          )}

          <h2 className="mb-1 mt-5 text-sm font-semibold text-slate-900">Purchase log</h2>
          <p className="mb-3 text-xs text-slate-400">
            Chronological events for this purchase. A healthy purchase shows{" "}
            <span className="font-mono">purchase_started</span> then{" "}
            <span className="font-mono">purchase_success</span>; a missing success event means it never
            completed.
          </p>
          <ol className="space-y-1">
            {[...d.events]
              .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
              .map((e, i) => (
                <li key={i} className="flex items-center justify-between gap-3 text-sm">
                  <span className="flex items-center gap-2">
                    <span
                      className={
                        e.eventName.includes("success")
                          ? "h-1.5 w-1.5 rounded-full bg-green-500"
                          : e.eventName.includes("failed")
                            ? "h-1.5 w-1.5 rounded-full bg-red-500"
                            : "h-1.5 w-1.5 rounded-full bg-slate-300"
                      }
                    />
                    <span className="font-mono text-xs">{e.eventName}</span>
                  </span>
                  <span className="text-xs text-slate-400">{dateTime(e.createdAt)}</span>
                </li>
              ))}
            {d.events.length === 0 && (
              <li className="text-sm text-slate-500">No log entries linked to this purchase.</li>
            )}
          </ol>
        </Card>
      </div>
    </div>
  );
}

/** A short, human explanation of a non-success purchase state, to aid debugging from the log below. */
function PurchaseStatusNote({
  status,
  failureCode,
  failureMessage,
}: {
  status: string;
  failureCode: string | null;
  failureMessage: string | null;
}) {
  if (status === "SUCCESS" || status === "RESTORED") return null;

  const note =
    status === "CREATED"
      ? "Started but never confirmed — the confirm step didn't complete (interrupted, or it hit an error before granting). Check the purchase log below for a purchase_started with no purchase_success."
      : status === "PENDING"
        ? "Awaiting confirmation from the billing provider."
        : status === "CANCELLED"
          ? "The user dismissed the purchase before confirming."
          : status === "REQUIRES_VERIFICATION"
            ? "Google Play verification is not configured, so the purchase was not granted (fails safe)."
            : status === "FAILED"
              ? `Confirmation failed${failureCode ? ` (${failureCode})` : ""}.${failureMessage ? " " + failureMessage : ""}`
              : "This purchase did not complete successfully.";

  return (
    <div className="mb-4">
      <Alert tone={status === "FAILED" || status === "REQUIRES_VERIFICATION" ? "error" : "warning"}>
        <strong>{status}</strong> — {note}
      </Alert>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-slate-500">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}
