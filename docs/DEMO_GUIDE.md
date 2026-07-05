# Demo Guide — End-to-End SDK Flow + Seeded Analytics

This guide covers the **demo experience**: the Android demo app, the realistic seeded database, the
portal "reset" control, and exactly how to run and test the whole loop. It complements
[`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md) (which documents every file/class).

> **Billing mode:** everything here runs in **MOCK** mode. Purchases are simulated but **real rows
> are written to the database** (purchases, entitlements, analytics), so the portal shows real
> numbers. No real payment, no Google Play, no card data. Places that would need real Google Play
> Billing are marked `TODO(google-billing)` / `TODO(google-play)` in code.

---

## 1. What the demo demonstrates

The demo app (`app/`) is a small but realistic app that uses the SDK exactly how a third-party app
would. It shows the full lifecycle:

1. **SDK initialization** — `IapDemoApplication` calls `PurchaseSdk.init(...)` once at startup.
2. **Loading items from the backend** — Store screen → `PurchaseSdk.getItems()`.
3. **Showing products** — name, description, type, price, currency, and an **Owned** badge.
4. **Opening the SDK purchase popup** — Buy/Unlock → `PurchaseSdk.showPurchasePopup(...)`.
5. **Confirming a purchase** — popup: pick a **payment method** (Apple Pay / Google Pay / PayPal /
   Credit Card), Confirm → `makePurchase(itemId, paymentMethod)` → backend `start` + `confirm`.
6. **Cancelling a purchase** — popup Cancel/back → cancel analytics, popup closes.
7. **Returning purchases** — Restore screen → `PurchaseSdk.restorePurchases(itemId?)` (a **return/refund**:
   releases the item, drops its price from portal revenue; all items or one).
8. **Checking entitlement** — Premium screen → `PurchaseSdk.hasEntitlement(itemId)`.
9. **Listing entitlements** — Entitlements screen → `PurchaseSdk.listEntitlements()`.
10. **Sending analytics** — automatic across the SDK, plus explicit `item_viewed` /
    `entitlement_checked` events from the demo.
11. **Success / cancel / failure / restore states** — every screen has loading + success + error UI.

### Screens

| Screen | SDK calls used | Notes |
|---|---|---|
| **Home** | — | Explains the demo; cards navigate to each flow; shows the demo user. |
| **Store** | `getItems()`, popup `showPurchasePopup()`, `trackEvent("item_viewed")` | Color-coded product cards with per-type badges, price chip, and Buy / Owned state. |
| **My Entitlements** | `listEntitlements()` | Active entitlements with type / granted / expiry; empty state. |
| **Restore / Return** | `restorePurchases(itemId?)` | Lists owned items with per-item **Return** (refund) buttons; loading → success / error. |
| **Premium Feature** | `hasEntitlement()`, popup `showPurchasePopup()`, `trackEvent("entitlement_checked")` | Locked vs unlocked; "Unlock Now" opens the popup. |

---

## 2. SDK public functions used by the demo

The demo only ever touches the **public** `PurchaseSdk` facade — never internal classes
(`ApiClient`, `PurchaseManager`, `EntitlementManager`, `PurchasePopupView`, `LocalStorage`,
`AnalyticsTracker`, `ErrorMapper` are all `internal`).

| Function | Where in the demo |
|---|---|
| `init(context, apiKey, billingMode, userId, config)` | `IapDemoApplication.onCreate` |
| `getItems()` *(added for this flow)* | `DemoViewModel.loadStore()` |
| `getItem(itemId)` | available; popup loads the item internally |
| `showPurchasePopup(activity, itemId, listener)` | `MainActivity.startPurchase` |
| `makePurchase(itemId)` | invoked by the popup's Confirm (via `PurchaseManager`) |
| `restorePurchases()` | `DemoViewModel.restorePurchases()` |
| `hasEntitlement(itemId)` | `DemoViewModel.checkPremiumAccess()` |
| `listEntitlements()` | `DemoViewModel.loadEntitlements()` |
| `trackEvent(name, props)` | `item_viewed`, `entitlement_checked` |

**Architecture:** UI (Compose screens) → `DemoViewModel` (state + the only SDK caller) → `PurchaseSdk`.
No business logic lives in the screens; they are pure functions of `DemoUiState`.

---

## 3. How the SDK now talks to the backend

`ApiClient` is **no longer an in-memory mock** — it is a real HTTP client (JDK `HttpURLConnection` +
Gson, **no new dependencies**) that calls the backend SDK API under `/api/v1/sdk/**`, sending the
`X-SDK-API-Key` header and parsing the standard `{ success, data, error, requestId }` envelope.

| SDK call | Backend endpoint |
|---|---|
| `getItems()` | `GET /api/v1/sdk/items` |
| `getItem(id)` | `GET /api/v1/sdk/items/{id}` |
| `makePurchase` (start) | `POST /api/v1/sdk/purchases/start` |
| `makePurchase` (confirm) | `POST /api/v1/sdk/purchases/confirm` (`Idempotency-Key` header) |
| `restorePurchases()` | `POST /api/v1/sdk/purchases/restore` |
| `hasEntitlement()` / `listEntitlements()` | `GET /api/v1/sdk/entitlements/check` / `GET /api/v1/sdk/entitlements` |
| `trackEvent()` + lifecycle events | `POST /api/v1/sdk/analytics/events` |

The backend's two-step purchase (`start` then `confirm`) is bridged inside `ApiClient`: `createPurchase`
remembers the in-flight `(itemId, userId, idempotencyKey)` so the existing `confirmPurchase(purchaseId)`
signature still works.

**Simulated vs real:**
- **Simulated (MOCK):** the "payment" itself. `confirm` simply marks the purchase `SUCCESS` and grants
  the entitlement server-side. No Google UI, no token, no charge.
- **Real:** the HTTP calls, the database writes, the entitlement logic, and the analytics — these are
  all genuine.
- **Needs real Google Billing (all `TODO`-marked):** `BillingClient` + `launchBillingFlow`,
  `ProductDetails` pricing, purchase-token + package-name + product-id verification, acknowledge/
  consume, verified subscription expiry. See `GooglePlayBillingProvider` and
  `GooglePlayVerificationService`.

---

## 4. Seeded demo data

Seeding lives in `backend/.../config/SeedDataLoader.java`, gated by `app.seed.enabled=true` (dev +
docker profiles only — **never production**). It is **idempotent**: it only seeds when the demo app is
absent, so restarts don't duplicate data. Data generation is **deterministic** (fixed random seed),
so a reset reproduces the same dataset.

**Created on first run:**
- **Portal user:** `demo@example.com` / `password123` (owner of the demo app).
- **Demo app:** `app_demo` ("Demo Game") with fixed API key **`demo_api_key_123`**.
- **Catalog:** **21 items** — lifetime unlocks, monthly + yearly subscriptions, consumable coin packs,
  and feature unlocks (e.g. `remove_ads`, `pro_monthly`, `pro_yearly`, `coins_100/500/1200/5000`,
  `unlimited_yearly`, …). All prices are **USD**, stored in **minor units**.
- **History** (`app.seed.sample-data=true`) from **`app.seed.start-date`** (default `2026-01-01`) to
  **`app.seed.end-date`** (blank ⇒ today): a realistic stream of **purchases, entitlements, and
  analytics events** for ~40 simulated users.
- **Default end-user `demo-user-001`** is given known recent ownership (an active *Remove Ads*
  lifetime, an active *Pro Monthly* subscription, and a *100 Coins* consumable) so the demo app shows
  data immediately.

**Why it looks realistic (not random):** volume follows a monthly story — January quiet, February
up, **March campaign spike**, April stable, **May subscription push**, **June yearly push** — plus a
weekend bump. Cheap consumables convert better than expensive subscriptions; subscriptions started
long ago are already **EXPIRED** while recent ones are **ACTIVE**; some purchases are **CANCELLED**,
**FAILED**, or **PENDING**.

**Analytics event names** (the canonical snake_case names the backend aggregates and the portal
charts read):

| Concept | Event name used |
|---|---|
| Popup opened | `purchase_popup_shown` |
| Confirm tapped | `purchase_confirm_clicked` |
| Purchase started | `purchase_started` |
| Purchase completed | `purchase_success` |
| Purchase failed | `purchase_failed` |
| Purchase canceled | `purchase_cancel_clicked` |
| Restore clicked / ok / fail | `restore_started` / `restore_success` / `restore_failed` |
| Entitlement checked | `entitlement_checked` |
| Item viewed | `item_viewed` |

> The task brief lists conceptual names like `PURCHASE_POPUP_OPENED`. We map those to the existing
> snake_case names above so the portal's aggregation keeps working end-to-end (renaming would break
> the funnel). The mapping is 1:1.

### Which analytics this powers (portal)
Popup opens, cancels, completions, total revenue, revenue by product, revenue over time
(day/week/month), conversion rate, cancellation rate, failed rate, top items, purchases by status,
active vs expired entitlements, and restore success/failure.

---

## 5. Resetting / regenerating data

Two ways:

**A) From the portal (recommended):** *Settings → Danger zone — demo data*:
- **Reset & regenerate demo data** — wipes everything and rebuilds the full dataset.
- **Delete all data (empty)** — wipes everything and leaves the database empty.

This calls `POST /api/v1/portal/maintenance/reset-demo-data?reseed=true|false` (JWT-protected). The
endpoint **only exists when `app.seed.enabled=true`** (dev/docker), so production has no
delete-everything button. Portal user accounts are **kept** so you stay logged in.

**B) Full restart of the stack:**
```bash
# Drop the Postgres volume and rebuild — the database is recreated and reseeded
docker compose down -v && docker compose up --build
```

> **Docker gotchas (important):**
> - **Always pass `--build`.** `docker compose up` reuses cached images, so without it you keep
>   running old code.
> - **Use `-v` after schema changes.** `ddl-auto: update` won't change an existing column's type, so a
>   plain restart can keep a stale schema. (This is why analytics/idempotency JSON is stored as plain
>   `varchar`, never `@Lob` — `@Lob String` becomes a Postgres `oid` that 500s analytics reads.)
> - On startup the seeder also **auto-reseeds** when it detects an outdated/smaller catalog, so an old
>   4-item seed in a persistent volume is refreshed to the current 21-item dataset automatically.

> **API key gotcha:** any reset (the portal button **or** `down -v`) wipes portal-created API keys and
> recreates only the seeded `demo_api_key_123`. If your `DemoConfig.API_KEY` points at a
> portal-created `psdk_live_…` key, it will be gone after a reset and the demo app gets
> `INVALID_API_KEY`. Use `demo_api_key_123` in `DemoConfig` so it survives every reset.

**Change the timeline:** set `SEED_START_DATE` / `SEED_END_DATE` (e.g. `2026-01-01` / empty for
"today") via env or `application-dev.yml` / `application-docker.yml`, then reset.

---

## 6. How to run the full demo

1. **Start the stack** (seeds automatically):
   ```bash
   docker compose up --build                # Postgres + backend + portal
   ```
2. **Open the portal** at <http://localhost:5173>, log in `demo@example.com` / `password123`, open
   **Demo Game** → check **Analytics / Revenue** (charts from Jan 2026 → today).
3. **Run the Android demo app** (`app/`) on an **emulator** (it targets `http://10.0.2.2:8080`).
   - Physical device: edit `DemoConfig.BACKEND_SDK_BASE_URL` to your computer's LAN IP.
   - Cleartext HTTP to the dev host is allowed via `res/xml/network_security_config.xml`.

---

## 7. Manual test checklist

| # | Action | Expected |
|---|---|---|
| 1 | Launch app | Home screen; SDK initialized (Logcat tag `IapApiClient`). |
| 2 | Home → **Store** | Catalog loads from backend; *Remove Ads* shows the **Owned** state (seeded for `demo-user-001`). |
| 3 | Tap **Buy** on an unowned item | SDK popup opens (emits `purchase_popup_shown`); shows product artwork + payment picker. |
| 4 | Tap **Cancel** | Popup closes; "Purchase cancelled." banner (`purchase_cancel_clicked`). |
| 5 | Tap **Buy** again → pick a payment method → **Confirm** | "Purchase complete"; item flips to **Owned**; a `SUCCESS` purchase row + entitlement appear in the DB/portal. |
| 6 | Home → **My Entitlements** | Lists active entitlements (incl. the one just bought). |
| 7 | Home → **Restore / Return** → **Return** an item | The item is released (buyable again) and its price is refunded from portal revenue. |
| 8 | Home → **Premium Feature** | If owned → **Unlocked**; else **Locked** + **Unlock Now** → popup → buy → unlocks. |
| 9 | Portal → **Analytics** (refresh) | Event counts/funnel reflect your taps; **Reset & regenerate** restores the rich dataset. |

**Troubleshooting:** if the Store shows "Is the backend running?", confirm the backend is up,
you're on an **emulator** (or set the LAN IP), and the API key in `DemoConfig` matches the seeded
`demo_api_key_123`.
