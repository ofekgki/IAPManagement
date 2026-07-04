import { Link, useParams } from "react-router-dom";
import { useItems, useSetItemActive } from "../hooks/usePortal";
import { Badge, Button, Card, CopyButton, ErrorMessage, PageHeader, Spinner } from "../components/ui";
import { date } from "../lib/format";

export default function ItemsPage() {
  const { appId = "" } = useParams();
  const items = useItems(appId);
  const setActive = useSetItemActive(appId);

  return (
    <div>
      <PageHeader
        title="Items"
        subtitle="Products your SDK can sell. Copy the item ID into PurchaseSdk calls."
        actions={
          <Link to={`/apps/${appId}/items/new`}>
            <Button>+ New item</Button>
          </Link>
        }
      />

      {items.isLoading && <Spinner />}
      {items.error && <ErrorMessage error={items.error} />}

      <Card className="overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Item ID</th>
              <th className="px-4 py-3">Type</th>
              <th className="px-4 py-3">Entitlement ID</th>
              <th className="px-4 py-3">Price</th>
              <th className="px-4 py-3">GP Product</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Created</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {(items.data ?? []).map((it) => (
              <tr key={it.id} className="border-b border-slate-100 last:border-0">
                <td className="px-4 py-3 font-medium">{it.name}</td>
                <td className="px-4 py-3">
                  <span className="font-mono text-xs">{it.itemId}</span>
                  <span className="ml-2 inline-block align-middle">
                    <CopyButton value={it.itemId} />
                  </span>
                </td>
                <td className="px-4 py-3">{it.type}</td>
                <td className="px-4 py-3">
                  {it.entitlementId ? (
                    <>
                      <span className="font-mono text-xs">{it.entitlementId}</span>
                      <span className="ml-2 inline-block align-middle">
                        <CopyButton value={it.entitlementId} />
                      </span>
                    </>
                  ) : (
                    <span className="text-slate-400">—</span>
                  )}
                </td>
                <td className="px-4 py-3">{it.priceDisplay ?? "—"}</td>
                <td className="px-4 py-3 text-xs text-slate-500">{it.googlePlayProductId ?? "—"}</td>
                <td className="px-4 py-3">
                  {it.isActive ? <Badge tone="green">Active</Badge> : <Badge tone="slate">Disabled</Badge>}
                </td>
                <td className="px-4 py-3 text-slate-500">{date(it.createdAt)}</td>
                <td className="px-4 py-3 text-right">
                  <div className="flex justify-end gap-2">
                    <Link to={`/apps/${appId}/items/${it.itemId}`}>
                      <Button variant="secondary">View</Button>
                    </Link>
                    <Button
                      variant="ghost"
                      onClick={() => setActive.mutate({ itemId: it.itemId, active: !it.isActive })}
                    >
                      {it.isActive ? "Disable" : "Enable"}
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
            {(items.data ?? []).length === 0 && (
              <tr>
                <td colSpan={9} className="px-4 py-6 text-center text-slate-500">
                  No items yet. Create your first item.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
