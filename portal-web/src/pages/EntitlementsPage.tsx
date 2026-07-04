import { useState } from "react";
import { useParams } from "react-router-dom";
import { useEntitlements, useGrantEntitlement, useItems, useRevokeEntitlement } from "../hooks/usePortal";
import { Alert, Badge, Button, Card, ErrorMessage, Field, Input, PageHeader, Select, Spinner } from "../components/ui";
import { dateTime } from "../lib/format";
import type { EntitlementStatus } from "../types";

const STATUSES: EntitlementStatus[] = ["ACTIVE", "EXPIRED", "REVOKED"];

export default function EntitlementsPage() {
  const { appId = "" } = useParams();
  // Roles were removed — every portal user can grant/revoke.
  const isAdmin = true;

  const [filters, setFilters] = useState<{ userId?: string; entitlementId?: string; status?: string; itemId?: string }>({});
  const entitlements = useEntitlements(appId, filters);
  const items = useItems(appId);
  const grant = useGrantEntitlement(appId);
  const revoke = useRevokeEntitlement(appId);

  const [grantForm, setGrantForm] = useState({ userId: "", entitlementId: "", sourceItemId: "", reason: "manual_test_grant" });
  const [error, setError] = useState<string | null>(null);

  async function submitGrant() {
    setError(null);
    if (!grantForm.userId || !grantForm.entitlementId) return setError("User ID and entitlement ID are required.");
    try {
      await grant.mutateAsync(grantForm);
      setGrantForm({ userId: "", entitlementId: "", sourceItemId: "", reason: "manual_test_grant" });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Grant failed");
    }
  }

  return (
    <div>
      <PageHeader title="Entitlements" subtitle="What access each user holds." />

      <Alert tone="warning">Manual entitlement changes are for testing/support only.</Alert>

      <div className="mt-4 mb-4 flex flex-wrap gap-2">
        <Input className="w-44" placeholder="User ID" value={filters.userId ?? ""} onChange={(e) => setFilters({ ...filters, userId: e.target.value || undefined })} />
        <Input className="w-44" placeholder="Entitlement ID" value={filters.entitlementId ?? ""} onChange={(e) => setFilters({ ...filters, entitlementId: e.target.value || undefined })} />
        <Select className="w-auto" value={filters.itemId ?? ""} onChange={(e) => setFilters({ ...filters, itemId: e.target.value || undefined })}>
          <option value="">All items</option>
          {(items.data ?? []).map((it) => (
            <option key={it.itemId} value={it.itemId}>{it.name}</option>
          ))}
        </Select>
        <Select className="w-auto" value={filters.status ?? ""} onChange={(e) => setFilters({ ...filters, status: e.target.value || undefined })}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </Select>
      </div>

      {isAdmin && (
        <Card className="mb-6">
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Grant entitlement manually</h2>
          {error && <div className="mb-3"><Alert tone="error">{error}</Alert></div>}
          <div className="grid gap-3 sm:grid-cols-4">
            <Field label="User ID"><Input value={grantForm.userId} onChange={(e) => setGrantForm({ ...grantForm, userId: e.target.value })} /></Field>
            <Field label="Entitlement ID"><Input value={grantForm.entitlementId} onChange={(e) => setGrantForm({ ...grantForm, entitlementId: e.target.value })} className="font-mono" /></Field>
            <Field label="Source item ID"><Input value={grantForm.sourceItemId} onChange={(e) => setGrantForm({ ...grantForm, sourceItemId: e.target.value })} className="font-mono" /></Field>
            <Field label="Reason"><Input value={grantForm.reason} onChange={(e) => setGrantForm({ ...grantForm, reason: e.target.value })} /></Field>
          </div>
          <div className="mt-3 flex justify-end">
            <Button onClick={submitGrant} disabled={grant.isPending}>{grant.isPending ? "Granting…" : "Grant"}</Button>
          </div>
        </Card>
      )}

      {entitlements.isLoading && <Spinner />}
      {entitlements.error && <ErrorMessage error={entitlements.error} />}

      <Card className="overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">User ID</th>
              <th className="px-4 py-3">Entitlement ID</th>
              <th className="px-4 py-3">Source item</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Starts</th>
              <th className="px-4 py-3">Expires</th>
              <th className="px-4 py-3">Purchase</th>
              {isAdmin && <th className="px-4 py-3"></th>}
            </tr>
          </thead>
          <tbody>
            {(entitlements.data ?? []).map((e) => (
              <tr key={e.id} className="border-b border-slate-100 last:border-0">
                <td className="px-4 py-3 font-mono text-xs">{e.userId}</td>
                <td className="px-4 py-3 font-mono text-xs">{e.entitlementId}</td>
                <td className="px-4 py-3">{e.sourceItemId ?? "—"}</td>
                <td className="px-4 py-3">
                  <Badge tone={e.status === "ACTIVE" ? "green" : e.status === "REVOKED" ? "red" : "amber"}>{e.status}</Badge>
                </td>
                <td className="px-4 py-3 text-slate-500">{dateTime(e.startsAt)}</td>
                <td className="px-4 py-3 text-slate-500">{dateTime(e.expiresAt)}</td>
                <td className="px-4 py-3 font-mono text-xs">{e.purchaseId ?? "—"}</td>
                {isAdmin && (
                  <td className="px-4 py-3 text-right">
                    {e.status === "ACTIVE" && (
                      <Button variant="danger" onClick={() => revoke.mutate({ userId: e.userId, entitlementId: e.entitlementId, reason: "admin_action" })}>
                        Revoke
                      </Button>
                    )}
                  </td>
                )}
              </tr>
            ))}
            {(entitlements.data ?? []).length === 0 && (
              <tr>
                <td colSpan={isAdmin ? 8 : 7} className="px-4 py-6 text-center text-slate-500">
                  No entitlements match these filters.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
