import { useParams } from "react-router-dom";
import { useApiKeys, useApp, useItems } from "../hooks/usePortal";
import { Alert, Badge, Card, CodeBlock, PageHeader, Spinner } from "../components/ui";

const GP_CHECKLIST = [
  "Create products in the Google Play Console and set googlePlayProductId on each item.",
  "Add the Google Play Billing dependency and BillingClient setup in the Android app.",
  "Server: add Google Play Developer API credentials (service account).",
  "Server: verify purchase tokens before granting entitlements.",
  "Server: validate package name + signing certificate fingerprint.",
  "Server: handle subscriptions, refunds, cancellations, renewals.",
  "Server: connect Real-time Developer Notifications via Google Pub/Sub.",
];

export default function SdkSetupPage() {
  const { appId = "" } = useParams();
  const app = useApp(appId);
  const keys = useApiKeys(appId);
  const items = useItems(appId);

  if (app.isLoading) return <Spinner />;

  const activeKey = (keys.data ?? []).find((k) => k.status === "ACTIVE");
  const billingMode = app.data?.defaultBillingMode ?? "MOCK";
  const sampleItem = (items.data ?? [])[0];
  const itemId = sampleItem?.itemId ?? "remove_ads";
  const entId = sampleItem?.entitlementId ?? "ent_premium";

  const initSnippet = `PurchaseSdk.init(
    context = applicationContext,
    apiKey = "${activeKey ? `${activeKey.keyPrefix}…` : "YOUR_API_KEY"}",
    billingMode = BillingMode.${billingMode}
)`;

  const purchaseSnippet = `PurchaseSdk.showPurchasePopup(
    activity = this,
    itemId = "${itemId}",
    userId = currentUser.id
)`;

  const entitlementSnippet = `val hasPremium = PurchaseSdk.hasEntitlement(
    userId = currentUser.id,
    entitlementId = "${entId}"
)`;

  return (
    <div>
      <PageHeader title="SDK Setup" subtitle="Copy these into your Android app." />

      <div className="space-y-6">
        <Card>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-900">1. API key & billing mode</h2>
            <Badge tone={activeKey ? "green" : "amber"}>{activeKey ? "Key ready" : "No active key"}</Badge>
          </div>
          {!activeKey && (
            <Alert tone="warning">
              Create an API key on the API Keys page, then paste it into the snippet below (it is shown only once).
            </Alert>
          )}
          <div className="mt-3">
            <CodeBlock code={initSnippet} />
          </div>
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">2. Show the purchase popup</h2>
          <CodeBlock code={purchaseSnippet} />
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">3. Check an entitlement</h2>
          <CodeBlock code={entitlementSnippet} />
        </Card>

        <Card>
          <h2 className="mb-2 text-sm font-semibold text-slate-900">How purchases work</h2>
          <Alert tone="info">
            <p className="mb-2">The SDK custom popup is a pre-purchase UI.</p>
            <p className="mb-2">In MOCK mode, the purchase is simulated by the backend.</p>
            <p>
              In GOOGLE_PLAY mode, the final payment confirmation screen is always controlled by Google Play
              Billing and cannot be replaced by this SDK.
            </p>
          </Alert>
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Google Play — future setup checklist</h2>
          <ul className="space-y-2 text-sm text-slate-600">
            {GP_CHECKLIST.map((step) => (
              <li key={step} className="flex gap-2">
                <span className="text-slate-300">○</span>
                {step}
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </div>
  );
}
