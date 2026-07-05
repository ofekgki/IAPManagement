<div align="center">

<img src="docs/media/logo.png" alt="Purchase SDK Platform" width="420" />

# Purchase SDK Platform

**An end-to-end in-app purchase platform — Android SDK · Spring Boot backend · React developer portal.**

Configure products and API keys in the portal, drive purchases from a drop-in Android SDK, and watch
revenue, funnels, and entitlements update live — all in a fully working **MOCK** billing mode (no real
charges), with a **Google Play** path scaffolded and failing safe until configured.

![Android](https://img.shields.io/badge/Android-Kotlin%20%2B%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot%203.3-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/Portal-React%20%2B%20Vite%20%2B%20TS-2563EB?logo=react&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL%20%2F%20H2-4169E1?logo=postgresql&logoColor=white)
![License](https://img.shields.io/badge/License-Educational-64748B)
[![jitPack](https://jitpack.io/v/ofekgki/IAPManagement.svg)](https://jitpack.io/#ofekgki/IAPManagement)


**Documentation site** — ([docs/index.html](https://ofekgki.github.io/IAPManagement/index.html)), served via GitHub Pages.

</div>

---

## Table of contents

- [Overview](#overview)
- [Features](#features)
- [Screenshots & video](#screenshots--video)
- [Architecture](#architecture)
- [Data model (ERD)](#data-model-erd)
- [Purchase flow (sequence)](#purchase-flow-sequence)
- [Purchase lifecycle (state)](#purchase-lifecycle-state)
- [Quick start](#quick-start)
- [SDK usage](#sdk-usage)
- [Public & internal functions](#public--internal-functions)
- [API endpoints](#api-endpoints)
- [JSON snippets](#json-snippets)
- [Database snippets](#database-snippets)
- [Documentation](#documentation)

---

## Overview

| Component | Path | Tech | Role |
|---|---|---|---|
| **Android SDK** | [`iap-sdk/`](iap-sdk) | Kotlin, Android Views, Coroutines, Gson | Drop-in client library: catalog, purchase popup, restore, entitlement checks, analytics. |
| **Demo app** | [`app/`](app) | Kotlin, Jetpack Compose, Material 3 | Reference app driving the SDK end-to-end (Store, Entitlements, Restore, Premium gate). |
| **Backend** | [`backend/`](backend) | Java 17, Spring Boot 3.3, Spring Data JPA, H2 / PostgreSQL | Source of truth: apps, keys, items, purchases, entitlements, analytics. Serves SDK + portal + internal APIs. |
| **Developer portal** | [`portal-web/`](portal-web) | React 18, TypeScript, Vite, TanStack Query, Tailwind, Recharts | Dashboard to manage apps/keys/items and view revenue & analytics. |
| **Shared** | [`shared/`](shared) | TypeScript | Cross-project enums + the response envelope contract. |

**Three authentication surfaces** on the backend:

| Surface | Prefix | Auth |
|---|---|---|
| SDK | `/api/v1/sdk/**` | `X-SDK-API-Key: <raw key>` (only a hash is stored) |
| Portal | `/api/v1/portal/**` | `Authorization: Bearer <JWT>` |
| Internal | `/api/v1/internal/**` | `X-Internal-Admin-Token` (fails closed) |

> **No real payments.** MOCK mode simulates the whole flow for demo/education. GOOGLE_PLAY mode is
> scaffolded — every real step is a TODO and it fails with `GOOGLE_PLAY_NOT_CONFIGURED` rather than
> faking success. The SDK popup is a **pre-purchase** UI; Google Play's screen is never replaced.

---

## Features

**Android SDK**
- One-call init + drop-in **purchase popup** (bottom sheet) with a per-product **gradient artwork tile**.
- In-popup **payment-method picker** — Apple Pay / Google Pay / PayPal / Credit Card.
- **Restore / return** (refund) flow, all-items or **per-item**.
- **Entitlement checks** (network-first) to gate premium features.
- Automatic **analytics** events (views, popup, purchase, restore, entitlement checks).
- Self-themed UI (works even if the host app isn't a Material app); MOCK + Google Play providers.

**Backend**
- REST API for SDK, portal, and internal admin, with a uniform `{ success, data, error, requestId }` envelope.
- **Price snapshot** on every purchase → editing an item's price never rewrites historical revenue.
- **Revenue** net of restores, broken down **by payment method**, product, and a **zero-filled** time series.
- Funnel + **purchases-by-status** analytics; **paginated** purchases API.
- API keys stored **hashed**; idempotent purchase confirmation; deterministic, idempotent demo seeder.
- All queries are **derived Spring Data methods** (no hand-written SQL) — see the DB guide.

**Developer portal**
- Apps, API keys (create / revoke / rotate), items (create / edit **price** / enable-disable).
- Dashboard KPIs, **Revenue** charts, **Purchases** (with a per-purchase **event log** for debugging),
  Entitlements (manual grant/revoke), Users (add / delete), profile & password edit.
- Case-insensitive **substring search** for user/item/entitlement IDs.
- Centralized SaaS theme shared with the SDK popup and demo app.

---

## Screenshots & video

> Assets live in [`docs/media/`](docs/media) — see that folder's README for the expected file names.
> Placeholders below resolve once you add the images.

### Developer portal
| Dashboard | Revenue | Purchases |
|---|---|---|
| ![Portal dashboard](docs/media/portal-dashboard.png) | ![Portal revenue](docs/media/portal-revenue.png) | ![Portal purchases](docs/media/portal-purchases.png) |

### Demo app (Android)
| Home | Store | Purchase popup |
|---|---|---|
| ![Demo home](docs/media/demo-home.png) | ![Demo store](docs/media/demo-store.png) | ![Demo popup](docs/media/demo-popup.png) |

### Walkthrough videos
- Demo app: [`docs/media/demo-walkthrough.mp4`](docs/media/demo-walkthrough.mp4)
- Portal: [`docs/media/portal-walkthrough.mp4`](docs/media/portal-walkthrough.mp4)

---

## Architecture

```text
        ANDROID CLIENT                          BACKEND (Spring Boot)
 ┌───────────────────────┐            ┌──────────────────────────────────────┐
 │  Demo app (Compose)   │            │  SDK API      /api/v1/sdk/**          │
 │        │ calls        │   HTTPS    │  (X-SDK-API-Key) ─────────┐           │
 │        ▼              │   JSON     │                           │           │
 │  iap-sdk (Kotlin)     │ ─────────▶ │  Portal API   /api/v1/portal/**       │
 └───────────────────────┘            │  (JWT) ───────────────────┤           │
                                      │                           ▼           │
 ┌───────────────────────┐   HTTPS    │  Internal API /api/v1/internal/**     │
 │  Developer portal      │   JSON     │  (admin token) ──▶  Services          │
 │  (React + Vite + TS)   │ ─────────▶ │      Purchase · Entitlement ·         │
 └───────────────────────┘            │      Analytics · ApiKey · Item        │
                                      │                     │                 │
                                      │                     ▼                 │
                                      │           Spring Data JPA / Hibernate │
                                      └─────────────────────┬─────────────────┘
                                                            ▼
                                              ┌───────────────────────────┐
                                              │   PostgreSQL  /  H2 (dev)  │
                                              └───────────────────────────┘
```

---

## Data model (ERD)

```mermaid
erDiagram
    DEVELOPER_USER   ||--o{ DEVELOPER_APP      : owns
    DEVELOPER_APP    ||--o{ API_KEY            : issues
    DEVELOPER_APP    ||--o{ PURCHASE_ITEM      : catalogs
    DEVELOPER_APP    ||--o{ PURCHASE           : records
    DEVELOPER_APP    ||--o{ ENTITLEMENT        : grants
    DEVELOPER_APP    ||--o{ ANALYTICS_EVENT    : logs
    DEVELOPER_APP    ||--o{ IDEMPOTENCY_RECORD : dedupes
    PURCHASE_ITEM    ||--o{ PURCHASE           : sold_as
    PURCHASE_ITEM    ||--o{ ENTITLEMENT        : defines
    PURCHASE         ||--o{ ENTITLEMENT        : grants
    PURCHASE         ||--o{ ANALYTICS_EVENT    : emits

    DEVELOPER_USER {
        string   id            PK "User ID"
        string   email         UK "Email address"
        string   password_hash    "Password hash"
        string   display_name     "Display name"
        enum     role             "Role"
        datetime created_at        "Created date"
    }
    DEVELOPER_APP {
        string   id                  PK "App ID"
        string   owner_user_id       FK "Owner user"
        string   app_name               "App name"
        string   package_name           "Package name"
        enum     billing_mode_default   "Default billing mode"
        boolean  is_active              "Active flag"
    }
    API_KEY {
        string   id               PK "Key ID"
        string   developer_app_id FK "App"
        string   name                "Key name"
        string   key_prefix          "Key prefix"
        string   key_hash         UK "Hashed key"
        enum     status              "Status"
    }
    PURCHASE_ITEM {
        string   id                 PK "Item row ID"
        string   developer_app_id   FK "App"
        string   item_id               "Item ID"
        string   name                  "Product name"
        enum     type                  "Product type"
        long     price_amount_minor    "Price in minor units"
        string   currency              "Currency"
        string   entitlement_id        "Entitlement granted"
        boolean  is_active             "Active flag"
    }
    PURCHASE {
        string   id                 PK "Purchase ID"
        string   developer_app_id   FK "App"
        string   user_id               "End user"
        string   item_id            FK "Item"
        enum     status                "Status"
        enum     payment_method        "Payment method"
        enum     billing_mode          "Billing mode"
        long     price_amount_minor    "Price snapshot"
        string   price_currency        "Currency snapshot"
        string   idempotency_key       "Idempotency key"
        datetime completed_at          "Completed date"
    }
    ENTITLEMENT {
        string   id               PK "Entitlement row ID"
        string   developer_app_id FK "App"
        string   user_id             "End user"
        string   entitlement_id      "Entitlement ID"
        string   source_item_id   FK "Source item"
        string   purchase_id      FK "Source purchase"
        enum     status              "Status"
        datetime expires_at          "Expiry or null"
    }
    ANALYTICS_EVENT {
        string   id               PK "Event ID"
        string   developer_app_id FK "App"
        string   user_id             "End user"
        string   event_name          "Event name"
        string   item_id             "Item"
        string   purchase_id      FK "Purchase"
        string   metadata_json       "Metadata JSON"
        datetime created_at          "Created date"
    }
    IDEMPOTENCY_RECORD {
        string   id               PK "Record ID"
        string   developer_app_id FK "App"
        string   idempotency_key  UK "Idempotency key"
        string   purchase_id      FK "Purchase"
        string   response_json       "Stored response"
    }
```

**Enum values**

| Field | Values |
|---|---|
| `developer_user.role` | `OWNER` · `ADMIN` · `VIEWER` (roles are currently un-enforced — every user has full access) |
| `*.billing_mode` | `MOCK` · `GOOGLE_PLAY` |
| `api_key.status` | `ACTIVE` · `REVOKED` |
| `purchase_item.type` | `LIFETIME` · `CONSUMABLE` · `SUBSCRIPTION` |
| `purchase.status` | `CREATED` · `PENDING` · `SUCCESS` · `FAILED` · `CANCELLED` · `REQUIRES_VERIFICATION` · `RESTORED` |
| `purchase.payment_method` | `APPLE_PAY` · `GOOGLE_PLAY` · `PAYPAL` · `CREDIT_CARD` |
| `entitlement.status` | `ACTIVE` · `EXPIRED` · `REVOKED` |

> Relationships are enforced in the service layer via app-scoped string IDs (`item_id`,
> `entitlement_id`, `purchase_id`) rather than DB foreign keys, keeping each app's data self-contained.
> Full column & index detail: [`docs/DATABASE_QUERIES.md`](docs/DATABASE_QUERIES.md).

---

## Purchase flow (sequence)

```text
 User        Demo app        iap-sdk            Backend (SDK API)          Database
  │             │               │                     │                      │
  │  tap Buy    │               │                     │                      │
  ├────────────▶│ showPurchasePopup(itemId)           │                      │
  │             ├──────────────▶│                     │                      │
  │             │               │ POST /purchases/start (X-SDK-API-Key)      │
  │             │               ├────────────────────▶│ INSERT purchase      │
  │             │               │                     ├─(CREATED)+snapshot──▶ │
  │             │               │  { purchaseId, PENDING }                   │
  │             │               │◀────────────────────┤                      │
  │  pick payment method, Confirm│                    │                      │
  ├─────────────────────────────▶│ POST /purchases/confirm                   │
  │             │               ├────────────────────▶│ MOCK fulfil          │
  │             │               │                     ├─SUCCESS + grant ────▶ │
  │             │               │                     │  entitlement +        │
  │             │               │  { SUCCESS, entitlement }   analytics       │
  │             │               │◀────────────────────┤                      │
  │             │ onPurchaseSuccess(result)           │                      │
  │             │◀──────────────┤                     │                      │
  │  item shows "Owned"         │                     │                      │
  │◀────────────┤               │                     │                      │
```

> The `Idempotency-Key` is **unique per purchase attempt**. A stable key would make the backend replay
> the first confirm and leave a re-purchase stuck at `CREATED` (never re-granting the entitlement).

---

## Purchase lifecycle (state)

```text
                 POST /start
                     │
                     ▼
                 ┌─────────┐  user dismiss        ┌───────────┐
                 │ CREATED │────────────────────▶ │ CANCELLED │ (end)
                 └────┬────┘                       └───────────┘
        MOCK confirm  │  await provider
             │        ▼
             │   ┌─────────┐  verified            ┌─────────┐
             │   │ PENDING │────────────────────▶ │ SUCCESS │
             │   └────┬────┘                       └────┬────┘
             │        │ failed        ┌────────┐        │ restore / return (refund)
             └───────▶│──────────────▶│ FAILED │(end)   ▼
             SUCCESS  │               └────────┘   ┌──────────┐
                      │ not configured             │ RESTORED │ (end)
                      ▼                             └──────────┘
              ┌────────────────────────┐
              │ REQUIRES_VERIFICATION  │ (end — Google Play not configured, fails safe)
              └────────────────────────┘
```

**Terminal states:** `SUCCESS`, `FAILED`, `CANCELLED`, `REQUIRES_VERIFICATION`, `RESTORED`.

---

## Quick start

### Option A — Docker Compose (Postgres + backend + portal)
```bash
cp .env.example .env            # optional for local
docker compose up --build
# Portal:  http://localhost:5173   (login: demo@example.com / password123)
# Backend: http://localhost:8080   (health: /api/v1/health)
```

### Option B — run components directly
```bash
# Backend (in-memory H2 + demo seed)
cd backend && mvn spring-boot:run

# Portal (Vite dev server)
cd portal-web && cp .env.example .env && npm install && npm run dev   # http://localhost:5173

# Demo app: open the repo in Android Studio and run the `app` module on an emulator.
# It targets http://10.0.2.2:8080 (emulator -> host). For a device, set
# DemoConfig.BACKEND_SDK_BASE_URL to your LAN IP.
```

**Seeded demo:** user `demo@example.com` / `password123`, app `app_demo`, API key `demo_api_key_123`,
~21 items and a realistic purchase/analytics history from **2026-01-01 -> today**. Reset any time via
Portal -> **Settings -> Danger zone**.

---

## SDK usage

### Install (JitPack)

[![JitPack](https://jitpack.io/v/ofekgki/IAPManagement.svg)](https://jitpack.io/#ofekgki/IAPManagement)

**1. Add the JitPack repository** to `settings.gradle.kts` (or your root `build.gradle`):

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }   // <-- add this
    }
}
```

**2. Add the dependency** in your app module, using a released tag (or `main-SNAPSHOT` for the latest commit):

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.ofekgki.IAPManagement:iap-sdk:1.0.0")
}
```

> The coordinate is `com.github.<user>.<repo>:<module>` — here `com.github.ofekgki.IAPManagement:iap-sdk`.
> Replace `1.0.0` with any [release tag](https://github.com/ofekgki/IAPManagement/releases) or a commit SHA.

**Requirements:** `minSdk 24`. No Material Components theme required in the host app — the popup themes itself.

### Use it

```kotlin
// 1) Initialize once (e.g. Application.onCreate)
PurchaseSdk.init(
    context = applicationContext,
    apiKey = "demo_api_key_123",
    billingMode = BillingMode.MOCK,
    userId = "demo-user-001",
    config = PurchaseSdkConfig(baseUrl = "http://10.0.2.2:8080/api/v1/sdk"),
)

// 2) Load the catalog (suspend)
val items: List<PurchaseItem> = PurchaseSdk.getItems()

// 3) Show the drop-in purchase popup
PurchaseSdk.showPurchasePopup(
    activity = this,
    itemId = "remove_ads",
    listener = object : PurchaseListener {
        override fun onPurchaseSuccess(result: PurchaseResult) { /* unlock */ }
        override fun onPurchaseCancelled() { }
        override fun onPurchaseFailed(error: PurchaseSdkError) { }
    },
)

// 4) Gate a premium feature
if (PurchaseSdk.hasEntitlement("remove_ads")) { showPremiumUi() }

// 5) Restore / return (all or one item)
val restored = PurchaseSdk.restorePurchases(itemId = "remove_ads")
```

---

## Public & internal functions

### Public API — `PurchaseSdk` (object)
| Function | Description |
|---|---|
| `init(context, apiKey, billingMode, userId?, config?)` | Initialize the SDK (once). |
| `isInitialized(): Boolean` | Whether `init` has run. |
| `suspend getItems(): List<PurchaseItem>` | Load the active catalog from the backend. |
| `suspend getItem(itemId): PurchaseItem` | Fetch a single product. |
| `showPurchasePopup(activity, itemId, listener)` | Present the drop-in purchase sheet. |
| `suspend makePurchase(itemId, paymentMethodId?): PurchaseResult` | Headless purchase (build your own UI). |
| `suspend restorePurchases(itemId?): List<PurchaseResult>` | Restore/return all owned items, or one. |
| `suspend hasEntitlement(itemId): Boolean` | Network-first entitlement check. |
| `suspend listEntitlements(): List<UserEntitlement>` | The user's entitlements. |
| `trackEvent(eventName, properties)` | Emit a custom analytics event. |
| `logout()` | Clear cached user/entitlement state. |

### Internal building blocks (`internal` — not part of the public API)
| Class | Key functions |
|---|---|
| `PurchaseManager` | `loadItem`, `loadItems`, `makePurchase`, `restorePurchases`, `handlePurchaseFailure` |
| `BillingProvider` (interface) | `getItem`, `getItems`, `makePurchase(itemId, userId, paymentMethod?)`, `restorePurchases(userId, itemId?)` — impls: `MockBillingProvider`, `GooglePlayBillingProvider` |
| `ApiClient` | `fetchItems`, `fetchItem`, `createPurchase`, `confirmPurchase`, `cancelPurchase`, `restorePurchases`, `fetchEntitlements`, `sendAnalyticsEvent` |
| `EntitlementManager` | `hasEntitlement`, `listEntitlements`, `refreshEntitlements`, `cacheEntitlements`, `addOrUpdateEntitlement`, `clearEntitlements` |
| `PurchasePopupController` | `showPopup`, `dismissPopup`, `isPopupShowing` |
| `PurchasePopupView` | `bind`, `bindItem`, `setLoadingState`, `showError` + generated per-product artwork |
| `AnalyticsTracker` | `trackPopupShown`, `trackPurchaseStarted/Success/Failed`, `trackRestoreStarted/Success/Failed`, `trackHasEntitlementChecked`, ... |

---

## API endpoints

Full reference with request/response shapes: **[`docs/API_ENDPOINTS.md`](docs/API_ENDPOINTS.md)**.

**SDK** (`X-SDK-API-Key`)
```
POST /api/v1/sdk/init
GET  /api/v1/sdk/items · /items/{itemId}
POST /api/v1/sdk/purchases/start · /confirm (Idempotency-Key) · /restore
GET  /api/v1/sdk/entitlements · /entitlements/check
POST /api/v1/sdk/analytics/events
```

**Portal** (`Authorization: Bearer <JWT>`)
```
POST /api/v1/portal/auth/register · /login · /logout   ·   GET/PATCH /auth/me
GET/POST /api/v1/portal/users   ·   DELETE /users/{id}
GET/POST /api/v1/portal/apps   ·   GET/PATCH/DELETE /apps/{appId}
GET/POST .../apps/{appId}/api-keys   ·   POST .../{keyId}/revoke · /rotate
GET/POST .../apps/{appId}/items   ·   GET/PATCH .../{itemId}   ·   POST .../{itemId}/enable · /disable
GET .../apps/{appId}/purchases (paginated)   ·   GET .../purchases/{purchaseId}
GET .../apps/{appId}/entitlements   ·   POST .../entitlements/grant · /revoke
GET .../apps/{appId}/analytics/overview · /funnel · /revenue · /revenue/by-product · /revenue/by-time · /purchases-by-status · /events
POST /api/v1/portal/maintenance/reset-demo-data?reseed=true|false
```

**Internal** (`X-Internal-Admin-Token`)
```
POST /api/v1/internal/items · PATCH /items/{itemId}
GET  /api/v1/internal/purchases · /analytics/summary
POST /api/v1/internal/entitlements/grant · /revoke
```

---

## JSON snippets

**Response envelope** (every endpoint)
```json
{ "success": true, "data": { }, "error": null, "requestId": "req_8f3c" }
```

**Start a purchase** — `POST /api/v1/sdk/purchases/start`
```json
{ "userId": "demo-user-001", "itemId": "remove_ads", "billingMode": "MOCK", "paymentMethod": "GOOGLE_PLAY" }
```

**Revenue summary** — `GET /api/v1/portal/apps/{appId}/analytics/revenue`
```json
{
  "totalRevenueMinor": 422639,
  "currency": "USD",
  "byPaymentMethod": [
    { "paymentMethod": "CREDIT_CARD", "revenueMinor": 139050, "purchases": 214 },
    { "paymentMethod": "GOOGLE_PLAY",  "revenueMinor": 93726,  "purchases": 141 }
  ],
  "overTime": [ { "bucket": "2026-06-30", "revenueMinor": 0, "purchases": 0 } ],
  "restoredValueMinor": 999,
  "restoredCount": 1
}
```

**Error**
```json
{ "success": false, "data": null,
  "error": { "code": "GOOGLE_PLAY_NOT_CONFIGURED", "message": "Google Play Billing is not configured." },
  "requestId": "req_2f7a" }
```

---

## Database snippets

All persistence is **Spring Data JPA** — derived query methods, no hand-written SQL. Examples of the
SQL Hibernate generates (see [`docs/DATABASE_QUERIES.md`](docs/DATABASE_QUERIES.md) for every method):

```sql
-- Revenue window (served by idx_purchase_app_status_completed)
SELECT * FROM purchase
WHERE developerAppId = ? AND status = 'SUCCESS'
  AND completedAt >= ? AND completedAt < ?;

-- Paginated portal listing (served by idx_purchase_app_created)
SELECT * FROM purchase
WHERE developerAppId = ? AND createdAt >= ? AND createdAt < ?
ORDER BY createdAt DESC;

-- Index-only event count for the funnel (idx_evt_app_name_time)
SELECT COUNT(*) FROM analytics_event
WHERE developerAppId = ? AND eventName = ? AND createdAt BETWEEN ? AND ?;
```

Key columns worth knowing (`purchase` table):

| Column | Notes |
|---|---|
| `priceAmountMinor`, `priceCurrency` | **Price snapshot** at purchase time -> historical revenue is immune to later price edits. |
| `paymentMethod` | `APPLE_PAY` / `GOOGLE_PLAY` / `PAYPAL` / `CREDIT_CARD` — the revenue breakdown dimension. |
| `status` | `CREATED · PENDING · SUCCESS · FAILED · CANCELLED · REQUIRES_VERIFICATION · RESTORED`. |
| `idempotencyKey` | Unique per attempt; dedupes confirm retries on `(developerAppId, idempotencyKey)`. |

---

## Documentation

| Doc | What's inside |
|---|---|
| [`docs/API_ENDPOINTS.md`](docs/API_ENDPOINTS.md) | Every REST endpoint with auth, request & response shapes. |
| [`docs/DATABASE_QUERIES.md`](docs/DATABASE_QUERIES.md) | Every repository method, generated SQL, tables & index design. |
| [`docs/DEMO_GUIDE.md`](docs/DEMO_GUIDE.md) | Run-the-demo walkthrough and click-by-click script. |
| [`docs/DEVELOPER_GUIDE.md`](docs/DEVELOPER_GUIDE.md) | Deeper developer/integration notes. |
| [`docs/media/`](docs/media) | Screenshots & videos referenced by this README. |

---

<div align="center">

**Educational project — no real payment processing.** MOCK mode is for demo/testing/learning only;
GOOGLE_PLAY mode requires real server-side verification before it may grant entitlements.

</div>
