# Purchase SDK Platform — Developer Guide

> An educational, deep-dive guide to the whole codebase: architecture, database, backend, Android
> SDK, web portal, flows, and the reasoning behind the design. Written for a student who wants to
> understand *why* each part exists, not just *what* it does.

**How the pieces are connected today (all three hops are now wired):**

- **Portal ↔ Backend**: fully wired over HTTP. The React portal calls the Spring Boot backend's
  `/api/v1/portal/**` endpoints with a JWT. This works end to end.
- **Backend ↔ Database**: fully wired (JPA → H2 in dev, PostgreSQL in Docker).
- **Android SDK ↔ Backend**: **now wired over HTTP.** The Kotlin SDK's `ApiClient` was rewritten from
  an in-memory mock into a **real HTTP client** (JDK `HttpURLConnection` + Gson, no extra
  dependency) that calls the backend's `/api/v1/sdk/**` endpoints with the `X-SDK-API-Key` header and
  parses the standard `{ success, data, error }` envelope. The demo app loads items, makes purchases,
  restores, checks entitlements, and sends analytics **for real** against the backend (in MOCK
  billing mode — the purchases are written to the database).

So the whole loop runs end to end: **demo app → SDK (HTTP) → backend → database → portal analytics**.
What remains simulated is the *payment* itself (MOCK mode never charges) and the real Google Play
path, which is still scaffolded and fails safely until configured — every such spot is `TODO`-marked.

---

## Table of contents

1. [Project Overview](#1-project-overview)
2. [Architecture Design](#2-architecture-design)
3. [Database Design](#3-database-design)
4. [Backend Guide](#4-backend-guide)
5. [API Documentation](#5-api-documentation)
6. [Android SDK Guide](#6-android-sdk-guide)
7. [Portal Guide](#7-portal-guide)
8. [Purchase Flow](#8-purchase-flow)
9. [Analytics Flow](#9-analytics-flow)
10. [Billing Mode](#10-billing-mode)
11. [Configuration & Environment](#11-configuration--environment-variables)
12. [How to Run](#12-how-to-run-the-project)
13. [Testing Guide](#13-testing-guide)
14. [Common Bugs & Debugging](#14-common-bugs--debugging)
15. [Code Quality Review](#15-code-quality-review)
16. [Future Improvements](#16-future-improvements)

---

## 1. Project Overview

### What it is

A learning platform that models how a commercial **in-app purchase (IAP) product** is built. Think
RevenueCat / Adapty in miniature. It has four runnable parts plus shared contracts:

| Part | Folder | Language/Tech | Runs as |
|---|---|---|---|
| Backend | `backend/` | Java 17, Spring Boot 3.3, JPA | A web server on `:8080` |
| Developer portal | `portal-web/` | React + TS + Vite + Tailwind | A SPA on `:5173` |
| Android SDK | `iap-sdk/` | Kotlin, Android library | A `.aar` library |
| Demo app | `app/` | Kotlin + Compose | An Android app that uses the SDK |
| Shared contracts | `shared/` | TypeScript types | Reference only |

### The problem it solves

A game/app developer wants to sell things ("Remove Ads", "Premium", coin packs). Doing that properly
requires a lot more than a button:

- A **catalog** of items with stable IDs and prices.
- A **backend** that records purchases and decides who owns what (**entitlements**).
- **Analytics** to understand the funnel (how many saw the paywall, how many bought).
- A **portal** so the developer can self-serve: make an app, get an API key, define items, watch
  revenue.
- An **SDK** so the mobile app can show a purchase UI and ask "does this user own Premium?" without
  re-implementing all that logic.

This project builds all of those in a deliberately simplified, **educational** way, with two billing
modes:

- **MOCK** — the *payment* is simulated by the server. The SDK still calls the backend over HTTP and
  real purchase/entitlement/analytics rows are written; there's just no Google Play and no real money.
  Perfect for demos and tests.
- **GOOGLE_PLAY** — a *scaffold* for real Google Play Billing. It is intentionally **not** implemented;
  it fails safely with `GOOGLE_PLAY_NOT_CONFIGURED` and is littered with `TODO`s showing exactly what
  a production implementation must add.

### Responsibilities of each part

- **Backend** is the **source of truth**. It owns the database and all business rules: validating API
  keys, creating purchases, granting entitlements, storing analytics, and (in MOCK mode) simulating
  the purchase. It serves three audiences via three URL prefixes and three auth schemes.
- **Database** persists everything: users, apps, keys, items, purchases, entitlements, analytics.
- **Portal** is the developer's control panel. It never talks to the database directly — it calls the
  backend's portal API.
- **SDK** is what an app developer embeds. It exposes a tiny public API (`init`, `getItem`,
  `showPurchasePopup`, `makePurchase`, `restorePurchases`, `hasEntitlement`, `listEntitlements`) and
  hides everything else.
- **Demo app** is a full multi-screen app (Home, Store, Entitlements, Restore, Premium Feature) that
  drives the SDK end to end against the backend, including the SDK's purchase popup. It also adds a
  public `getItems()` call to load the whole catalog.

---

## 2. Architecture Design

### The intended end-to-end architecture

```
                         ┌──────────────────────────┐
                         │   Developer (human)      │
                         └────────────┬─────────────┘
                                      │ browser (JWT)
                                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│  PORTAL  (portal-web, React SPA :5173)                               │
│  - login/register, apps, API keys, items, analytics, revenue        │
└───────────────────────────────┬─────────────────────────────────────┘
                                 │ HTTPS  /api/v1/portal/**  (Authorization: Bearer <JWT>)
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│  BACKEND  (Spring Boot :8080)  — the source of truth                │
│                                                                     │
│   /api/v1/portal/**     JWT          → portal services              │
│   /api/v1/sdk/**        X-SDK-API-Key→ SDK services  ◀── the seam   │
│   /api/v1/internal/**   admin token  → internal/admin services      │
│                                                                     │
│   Services: ApiKey, Item, Purchase, Entitlement, Analytics,         │
│             MockBilling, GooglePlayVerification(stub), Portal*      │
└───────────────────────────────┬─────────────────────────────────────┘
                                 │ JPA / SQL
                                 ▼
                    ┌──────────────────────────┐
                    │  DATABASE (H2 / Postgres)│
                    │  8 tables (see §3)       │
                    └──────────────────────────┘

                                 ▲
                                 │ HTTP(S)  /api/v1/sdk/**  (X-SDK-API-Key)
                                 │ ✓ WIRED — ApiClient makes real HTTP calls
                                 │
┌─────────────────────────────────────────────────────────────────────┐
│  ANDROID APP  (app/) ── embeds ──▶  ANDROID SDK (iap-sdk)            │
│                                                                     │
│   PurchaseSdk (public facade)                                       │
│     → PurchaseManager → BillingProvider                             │
│            ├── MockBillingProvider ── ApiClient (REAL HTTP client)   │
│            └── GooglePlayBillingProvider (stub, TODOs)               │
│     → EntitlementManager → LocalStorage (SharedPreferences)         │
│     → AnalyticsTracker, ErrorMapper                                 │
│     → PurchasePopupController → PurchasePopupView (the popup UI)     │
└─────────────────────────────────────────────────────────────────────┘

         ┌───────────────────────────────────────────────┐
         │  GOOGLE PLAY BILLING (future)                 │
         │  Integrates in TWO places, both stubbed:      │
         │   • SDK: GooglePlayBillingProvider            │
         │   • Backend: GooglePlayVerificationService    │
         └───────────────────────────────────────────────┘
```

### How the pieces are *meant* to connect (and what's real today)

1. **Developer → Portal → Backend → DB**: real and working. The developer logs in (JWT), creates an
   app (`DeveloperApp` row), generates an API key (`ApiKey` row, raw shown once), and creates items
   (`PurchaseItem` rows).
2. **App → SDK → Backend → DB**: real and working. The SDK's `ApiClient` sends the API key in
   `X-SDK-API-Key`, calls `/api/v1/sdk/...` over HTTP, and the backend creates purchases/entitlements
   and stores analytics — actual database rows the portal then reports on.
3. **Portal shows analytics from backend data**: real. The portal's analytics pages read aggregates
   the backend computes from the `analytics_event` and `purchase` tables.

### Where API keys are used

- **Created** in the portal (`POST /portal/apps/{appId}/api-keys`). Stored **hashed** (`SHA-256(pepper
  + raw)`) in the `api_key` table; the raw value is shown exactly once.
- **Used** by the SDK on every `/api/v1/sdk/**` request via the `X-SDK-API-Key` header. The backend's
  `SdkApiKeyFilter` hashes the presented key, looks it up, checks it's `ACTIVE`, resolves the owning
  app, and stamps `lastUsedAt`.

### Where Google Billing will be integrated

Two stubs, both fail safe:

- **SDK side**: `iap-sdk/.../billing/GooglePlayBillingProvider.kt` — would launch the Play billing
  flow; currently throws `PurchaseSdkError.GooglePlayNotConfigured`.
- **Backend side**: `backend/.../service/googleplay/GooglePlayVerificationService.java` — would verify
  the purchase token with Google's Developer API before granting an entitlement; currently returns
  `VerificationResult.notConfigured()`.

### How data moves

- **Items**: portal creates → DB `purchase_item` → SDK reads (`getItem`) to render the popup.
- **Purchases**: SDK starts/confirms → backend writes `purchase` rows → grants `entitlement`.
- **Entitlements**: backend writes → SDK/app reads (`hasEntitlement`) to unlock features.
- **Analytics**: SDK/app emits events → backend writes `analytics_event` → portal aggregates.

---

## 3. Database Design

The schema is defined **by the JPA entities** (`backend/.../domain/*.java`) with `ddl-auto: update`,
so Hibernate creates the tables. There are **8 tables**.

### Text ERD

```
 developer_user (portal accounts)
   id (PK)
     │ 1
     │
     │ owns  (developer_app.ownerUserId → developer_user.id, soft FK)
     ▼ N
 developer_app (a project/app using the SDK)
   id (PK)
     │ 1                         │ 1                        │ 1
     ├── has many ──▶ api_key    ├── has many ─▶ purchase_item ├── has many ─▶ purchase
     │   (developerAppId)        │   (developerAppId)          │   (developerAppId)
     │                           │                             │
     ├── has many ──▶ entitlement (developerAppId)             │
     ├── has many ──▶ analytics_event (developerAppId)         │
     └── has many ──▶ idempotency_record (developerAppId)      │
                                                               ▼
 purchase ──(itemId, soft)──▶ purchase_item                purchase
 entitlement ──(sourceItemId/purchaseId, soft)──▶ purchase_item / purchase
 analytics_event ──(itemId/purchaseId, soft)──▶ purchase_item / purchase
```

> **Important design note:** relationships are modeled with **plain string columns** (e.g.
> `developerAppId`, `itemId`), **not** JPA `@ManyToOne` foreign keys. This is a deliberate
> simplification for an educational project: it keeps entities flat, avoids lazy-loading/`@Transactional`
> subtleties, and mirrors how a sharded/event-sourced system often stores soft references. The cost is
> that referential integrity is enforced in **service code**, not by the database. A production system
> would likely add real FKs.

### Table-by-table

Conventions: every id is a **String** like `app_a1b2c3d4e5f6` (prefix + 12 hex chars, see
`common/Ids.java`). Timestamps are `Instant` (UTC).

#### `developer_user` — portal accounts

| Column | Type | Notes |
|---|---|---|
| `id` | String | **PK** |
| `email` | String | **unique** (index `uq_user_email`) |
| `passwordHash` | String | PBKDF2 `iterations:salt:hash`, never the raw password |
| `displayName` | String | optional |
| `role` | enum String | `OWNER` / `ADMIN` / `VIEWER` |
| `createdAt`, `updatedAt` | Instant | set by `@PrePersist`/`@PreUpdate` |

**Why it exists:** the portal needs authenticated developer accounts. **Example row:**
`usr_… , demo@example.com , <pbkdf2> , "Demo Developer" , OWNER`.

#### `developer_app` — a project that uses the SDK

| Column | Type | Notes |
|---|---|---|
| `id` | String | **PK** |
| `ownerUserId` | String | soft FK → `developer_user.id` (index `idx_app_owner`); null for legacy/seed |
| `appName` | String | required |
| `packageName` | String | e.g. `com.example.demogame` |
| `description` | String(1000) | optional |
| `billingModeDefault` | enum String | `MOCK` / `GOOGLE_PLAY` (exposed as `defaultBillingMode` in DTOs) |
| `isActive` | boolean | soft-delete flag |
| `createdAt`, `updatedAt` | Instant | |

**Why:** the unit that owns keys, items, purchases. A user can have many apps. **Example:**
`app_demo , usr_… , "Demo Game" , com.example.demogame , MOCK , true`.

#### `api_key` — SDK credentials for an app

| Column | Type | Notes |
|---|---|---|
| `id` | String | **PK** |
| `developerAppId` | String | soft FK → `developer_app.id` (index `idx_apikey_app`) |
| `name` | String | label, e.g. "Production key" |
| `keyPrefix` | String | non-secret display prefix, e.g. `psdk_live_abc123` |
| `keyHash` | String | **unique** (index `idx_apikey_hash`); `SHA-256(pepper + rawKey)` |
| `status` | enum String | `ACTIVE` / `REVOKED` |
| `createdAt`, `revokedAt`, `lastUsedAt` | Instant | |

**Why a separate table (not a column on the app)?** One app may have several keys (rotate without
downtime), each independently revocable, with its own last-used timestamp. The raw key is never
stored — only its hash, so a DB leak doesn't leak usable keys. **Example:** `key_… , app_demo ,
"Demo SDK Key" , demo_api_key_ , <sha256> , ACTIVE`.

#### `purchase_item` — the catalog

| Column | Type | Notes |
|---|---|---|
| `id` | String | **PK** |
| `developerAppId` | String | soft FK; part of unique constraint |
| `itemId` | String | SDK-facing id, e.g. `remove_ads`; **unique per app** (`uq_item_app_itemid`) |
| `name`, `description` | String | |
| `type` | enum String | `NON_CONSUMABLE` / `CONSUMABLE` / `SUBSCRIPTION` |
| `entitlementId` | String | what owning this grants, e.g. `ent_remove_ads`; null for pure consumables |
| `priceAmountMinor` | Long | **authoritative price in cents** — revenue uses this |
| `currency` | String | e.g. `USD` |
| `priceDisplay` | String | e.g. `$1.99` (display only, never used for revenue math) |
| `googlePlayProductId` | String | required later for GOOGLE_PLAY |
| `isActive` | boolean | |
| `createdAt`, `updatedAt` | Instant | |

**Why two prices?** `priceDisplay` is a human string that may be localized/odd; `priceAmountMinor` is
an integer you can sum safely. Mixing them is a classic money bug, so they're separate. **Example:**
`item_… , app_demo , remove_ads , "Remove Ads" , NON_CONSUMABLE , ent_remove_ads , 199 , USD , $1.99`.

#### `purchase` — a purchase attempt/result

| Column | Type | Notes |
|---|---|---|
| `id` | String | **PK** |
| `developerAppId`, `userId`, `itemId` | String | soft FKs (indexes `idx_purchase_app_user`, `idx_purchase_status`) |
| `billingMode` | enum String | `MOCK` / `GOOGLE_PLAY` |
| `status` | enum String | `CREATED`/`PENDING`/`SUCCESS`/`FAILED`/`CANCELLED`/`REQUIRES_VERIFICATION` |
| `provider` | enum String | `MOCK` / `GOOGLE_PLAY` |
| `providerPurchaseToken` | String | Google token (stored for verification, **never logged in full**) |
| `providerOrderId` | String | safe to display |
| `idempotencyKey` | String | from the `Idempotency-Key` header |
| `failureCode`, `failureMessage` | String | populated on failure |
| `createdAt`, `updatedAt`, `completedAt` | Instant | |

**Why:** the audit trail of every attempt, including failures. Revenue counts only `status = SUCCESS`.

#### `entitlement` — what a user owns

| Column | Type | Notes |
|---|---|---|
| `id` | String | **PK** |
| `developerAppId`, `userId`, `entitlementId` | String | soft FKs (indexes `idx_ent_app_user*`) |
| `sourceItemId`, `purchaseId` | String | provenance |
| `status` | enum String | `ACTIVE` / `EXPIRED` / `REVOKED` |
| `startsAt`, `expiresAt` | Instant | `expiresAt` null = never expires |
| `createdAt`, `updatedAt` | Instant | |

**Why separate from purchase?** Access is a different concept from a transaction. One purchase grants
one entitlement, but entitlements can also be granted manually (support), revoked (refund), or expire
(subscription) independently of the purchase row. `hasEntitlement` reads this table.

#### `analytics_event` — the event log

| Column | Type | Notes |
|---|---|---|
| `id` | String | **PK** |
| `developerAppId` | String | soft FK (index `idx_evt_app_name_time` on app+name+time) |
| `userId`, `itemId`, `purchaseId` | String | optional soft FKs |
| `eventName` | String | e.g. `purchase_popup_shown` |
| `billingMode` | enum String | |
| `metadataJson` | varchar(4000) | arbitrary JSON (plain text column — **not** `@Lob`; `@Lob String` becomes a Postgres `oid` and breaks reads) |
| `createdAt` | Instant | back-dateable (seed uses this for charts) |

**Why:** the raw material for the funnel and conversion metrics. Append-only.

#### `idempotency_record` — exactly-once confirmation

| Column | Type | Notes |
|---|---|---|
| `id` | String | **PK** |
| `developerAppId`, `idempotencyKey` | String | **unique together** (`uq_idem_app_key`) |
| `purchaseId` | String | |
| `responseJson` | TEXT | the serialized first response |
| `createdAt` | Instant | |

**Why:** if the SDK retries `confirm` (flaky network, double tap), we must not charge/grant twice. The
first response is stored and replayed for the same key.

### How the DB supports each concept

- **Users/apps/keys**: `developer_user` → `developer_app` → `api_key`. Auth + ownership.
- **Items**: `purchase_item`, unique `(app, itemId)`.
- **Purchases**: `purchase`, plus `idempotency_record` for safety.
- **Entitlements**: `entitlement`, queried by `(app, user, entitlementId)`.
- **Analytics/Revenue**: `analytics_event` (counts) + `purchase` joined to `purchase_item` for money.

---

## 4. Backend Guide

Package root: `com.example.purchasebackend`. Below, files are grouped by layer. The **request flow**
is: *Filters → Controller → Service → Repository → DB*, with `GlobalExceptionHandler` shaping errors
and `ApiResponse` shaping success.

### 4.0 Entry point & cross-cutting

**`PurchaseBackendApplication.java`** — the `@SpringBootApplication` main class. Component-scans the
whole package so entities, repositories, services, controllers, and filters are all auto-wired.

#### `common/` — the response/error contract

- **`ApiResponse<T>`** (record `{success, data, error, requestId}`) — the single envelope every
  endpoint returns. Static factories `ok(data, requestId)` / `fail(error, requestId)`. *Why:* a
  uniform shape means the portal's `api.ts` can unwrap every response the same way.
- **`ApiError`** (record `{code, message, details}`) — the error payload. Built from an `ErrorCode`.
- **`ErrorCode`** (enum) — the **stable error vocabulary**: each constant maps to an `HttpStatus` and a
  default message (e.g. `ITEM_NOT_FOUND → 404`, `GOOGLE_PLAY_NOT_CONFIGURED → 501`,
  `INVALID_CREDENTIALS → 401`). *Why:* clients switch on a string code, not on brittle HTTP details.
- **`ApiException`** (RuntimeException) — carries an `ErrorCode` (+ optional message/details). Business
  code throws this; the handler turns it into the right HTTP status + envelope. *Why:* services can
  fail loudly without knowing about HTTP.
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) — catches `ApiException` (→ mapped status),
  `MethodArgumentNotValidException` (bean-validation failures → `INVALID_REQUEST` with field details),
  and anything else (→ `INTERNAL_ERROR`, logged). Delegates payload shaping to `ErrorMapper`.
- **`RequestContext`** — a `ThreadLocal` holder for the current `requestId` and the resolved
  `DeveloperApp` (for SDK requests). *Why:* controllers/services can read these without threading them
  through every method signature. Cleared at the end of each request by `RequestIdFilter`.
- **`Ids`** — `newId(prefix)` → `prefix_<12 hex>`. Used everywhere ids are minted.

**Error handling summary:** all failures funnel through `ApiException` + `ErrorCode` +
`GlobalExceptionHandler`, so responses are consistent. **Security:** error messages are generic enough
not to leak internals (e.g. login returns `INVALID_CREDENTIALS` for both unknown email and wrong
password).

### 4.1 Domain entities (`domain/` and `domain/enums/`)

Each entity is a plain JPA `@Entity` with hand-written getters/setters and `@PrePersist`/`@PreUpdate`
timestamp hooks. They map 1:1 to the tables in §3. Files: `DeveloperUser`, `DeveloperApp`, `ApiKey`,
`PurchaseItem`, `Purchase`, `Entitlement`, `AnalyticsEvent`, `IdempotencyRecord`.

Notable detail: `Purchase` and `AnalyticsEvent` `@PrePersist` only set `createdAt` **if null**, so the
seed loader can back-date rows for realistic charts. *Why this matters:* without it, every seeded
event would have "now" as its timestamp and the time-series charts would be a single spike.

**Enums** (`domain/enums/`): `BillingMode {MOCK, GOOGLE_PLAY}`, `BillingProviderType {MOCK,
GOOGLE_PLAY}`, `ItemType {NON_CONSUMABLE, CONSUMABLE, SUBSCRIPTION}`, `PurchaseStatus {CREATED, PENDING,
SUCCESS, FAILED, CANCELLED, REQUIRES_VERIFICATION}`, `EntitlementStatus {ACTIVE, EXPIRED, REVOKED}`,
`DeveloperUserRole {OWNER, ADMIN, VIEWER}`, `ApiKeyStatus {ACTIVE, REVOKED}`. They're stored as strings
(`@Enumerated(EnumType.STRING)`) so adding a value doesn't shift ordinals.

### 4.2 Repositories (`repository/`)

Spring Data JPA interfaces — Spring generates the implementations from method names. No SQL written by
hand. Each touches exactly one table.

| Repository | Key methods | Table |
|---|---|---|
| `DeveloperUserRepository` | `findByEmailIgnoreCase`, `existsByEmailIgnoreCase` | developer_user |
| `DeveloperAppRepository` | `findByOwnerUserIdOrderByCreatedAtDesc`, `findByIdAndOwnerUserId` | developer_app |
| `ApiKeyRepository` | `findByKeyHash`, `findByDeveloperAppIdOrderByCreatedAtDesc`, `findByIdAndDeveloperAppId` | api_key |
| `PurchaseItemRepository` | `findByDeveloperAppIdAndItemId`, `findByDeveloperAppIdAndIsActiveTrue`, `findByDeveloperAppId`, `existsByDeveloperAppIdAndItemId` | purchase_item |
| `PurchaseRepository` | `findByIdAndDeveloperAppId`, `findByDeveloperAppIdAndUserIdAndStatus`, `findByDeveloperAppIdAndStatus`, `findByDeveloperAppId` | purchase |
| `EntitlementRepository` | `findFirst…AndEntitlementIdOrderByCreatedAtDesc`, `findByDeveloperAppId`, `countByDeveloperAppIdAndStatus` | entitlement |
| `AnalyticsEventRepository` | `findByDeveloperAppIdAndCreatedAtBetween`, `findTop200…OrderByCreatedAtDesc`, `findByPurchaseId`, `countBy…` | analytics_event |
| `IdempotencyRecordRepository` | `findByDeveloperAppIdAndIdempotencyKey` | idempotency_record |

*Why interfaces only:* Spring Data removes boilerplate; the method name *is* the query. The cost is
that complex aggregations (analytics) are done in Java by loading rows and streaming — fine at this
scale, not at millions of rows (see §15).

### 4.3 Core services (`service/`)

These hold the business rules. They are stateless `@Service` beans.

- **`ApiKeyService`** — *the API-key authority.*
  - `hashApiKey(raw)` → `SHA-256(pepper + raw)` hex. Deterministic so it can be looked up.
  - `validateAndGetApp(raw)` → finds the `ApiKey` by hash, checks `ACTIVE`, loads the owning
    `DeveloperApp`, checks it's active, stamps `lastUsedAt`. Throws `INVALID_API_KEY`/`APP_DISABLED`.
    Called by `SdkApiKeyFilter`.
  - `create/list/revoke/rotate` for the portal. `create` mints a raw key `psdk_live_…`, stores only the
    hash, and returns a `CreatedApiKey(rawApiKey, apiKey)` — the **only** moment the raw key exists.
  - *Security/TODOs:* the key is not a strong secret (ships in the app); the class header lists the
    hardening TODOs (Play Integrity, package-name + cert validation, rate limiting). Touches `api_key`,
    `developer_app`.
- **`DeveloperAppService`** — `getById` with `INVALID_REQUEST` if unknown. Used by internal controllers
  that receive a `developerAppId` directly.
- **`ItemService`** — the **SDK/internal** catalog service: `listActiveItems`, `getActiveItem`
  (throws `ITEM_NOT_FOUND`), `createItem`, `updateItem`. Touches `purchase_item`. (The portal uses a
  separate `PortalItemService` with richer rules — see §4.6. This split is a mild duplication, noted in
  §15.)
- **`PurchaseService`** — *the heart of the system.* Drives start/confirm/restore.
  - `startPurchase(app, req)` → validates user/item, creates a `CREATED` purchase, emits
    `purchase_started`. Returns the `purchaseId`.
  - `confirmPurchase(app, req, idempotencyKey)`:
    1. **Idempotency replay**: if this key was processed, return the stored response (no double work).
    2. Load + validate the purchase; block `CANCELLED`; if already `SUCCESS`, return the original
       result (idempotent without a key too).
    3. Branch by billing mode (a `switch` expression):
       - **MOCK** → `MockBillingService.confirm` (status `SUCCESS`) → `EntitlementService.grantForPurchase`
         → emit `purchase_success` → store idempotency → return.
       - **GOOGLE_PLAY** → `GooglePlayVerificationService.verifyPurchase` → on `NOT_CONFIGURED`: mark
         purchase `REQUIRES_VERIFICATION`, emit `purchase_failed`, throw `GOOGLE_PLAY_NOT_CONFIGURED`.
         (On a hypothetical `VERIFIED` it would grant; on `FAILED` it throws `PURCHASE_VERIFICATION_FAILED`.)
  - `restorePurchases(app, req)` → MOCK returns prior `SUCCESS` purchases + active entitlements;
    GOOGLE_PLAY throws `GOOGLE_PLAY_NOT_CONFIGURED`.
  - *Design subtlety:* `confirm` is intentionally **not** `@Transactional`, so the "mark
    REQUIRES_VERIFICATION then throw" path actually persists the status before the exception rolls
    nothing back. Touches `purchase`, `purchase_item`, `entitlement`, `idempotency_record`,
    `analytics_event`.
- **`EntitlementService`** — *who owns what.* `grantForPurchase(purchase, item, expiryOverride)`
  implements the rules:
  - **NON_CONSUMABLE** → grant `ACTIVE`, no expiry; if one already exists active, return it (no
    duplicates).
  - **SUBSCRIPTION** → grant `ACTIVE` with `expiresAt = now + app.mock-subscription-days` (default 30);
    in real GOOGLE_PLAY the expiry must come from verified Google data (TODO).
  - **CONSUMABLE** → only grant if the item declares an `entitlementId` (TODO: real coin balances).
  - Also `checkEntitlement` (lazily flips expired→`EXPIRED`, emits `entitlement_checked`),
    `listEntitlements`, `grantManual`, `revoke`. Touches `entitlement`, `purchase_item`.
- **`AnalyticsService`** — `record(...)` writes one `analytics_event`; **never throws** (analytics must
  not break a purchase) — it logs and swallows failures. `summary(...)` counts events for the internal
  summary endpoint. Touches `analytics_event`.
- **`IdempotencyService`** — `findStored` / `store` keyed by `(app, idempotencyKey)`, serializing the
  response as JSON. Touches `idempotency_record`.
- **`MockBillingService`** — `confirm(purchase)` flips a purchase to `SUCCESS` with a fake order id. The
  class comment is explicit: *MOCK is not proof of payment.*
- **`ErrorMapper`** — `classify(throwable)` / `toApiError(ApiException)`. Used by
  `GlobalExceptionHandler` so error shaping lives in one place.

#### `service/googleplay/`

- **`GooglePlayVerificationService`** — the production seam. `verifyPurchase(req)` currently returns
  `VerificationResult.notConfigured()` and logs only a **shortened** token. The class is a checklist of
  TODOs (Developer API client, service-account creds, verify token/package/product/state, prevent token
  reuse, acknowledge/consume, subscription expiry, Pub/Sub notifications).
- **`GooglePlayVerificationRequest`** / **`VerificationResult`** (with `Outcome {NOT_CONFIGURED,
  VERIFIED, FAILED}`) — the request/result records. *Why a 3-state outcome:* it lets the purchase flow
  distinguish "not built yet" from "checked and invalid", returning different error codes.

#### `service/support/` & `service/mapper/`

- **`BillingModes`** — `parseOrDefault` / `parseOrNull` to turn the client's billing-mode string into
  the enum (throws `BILLING_MODE_NOT_SUPPORTED` on garbage).
- **`DateRanges`** — turns optional `from`/`to` date params into an `Instant` range, defaulting to the
  **last 365 days** (wide enough to show the full seeded Jan→today demo history by default).
- **`DtoMapper`** — entity → DTO conversions for the SDK/internal DTOs (`toItemDto`, `toAdminItemDto`,
  `toEntitlementSummary`, `toEntitlementDto`, `toAdminPurchaseDto`).

### 4.4 SDK-facing controllers (`web/`)

All under `/api/v1`, protected by `SdkApiKeyFilter` (except health). They are thin: read the resolved
app from `RequestContext`, call a service, wrap in `ApiResponse`.

- **`HealthController`** — `GET /api/v1/health` (public). Returns `{status, service}` (unwrapped).
- **`SdkInitController`** — `POST /sdk/init`. Validates the app (via the filter), records
  `sdk_initialized`, returns feature flags (`mockBillingEnabled=true`, `googlePlayBillingEnabled=false`,
  `analyticsEnabled=true`). TODOs: package-name / signing-cert / Play Integrity validation.
- **`ItemController`** — `GET /sdk/items`, `GET /sdk/items/{itemId}`.
- **`PurchaseController`** — `POST /sdk/purchases/start`, `/confirm` (reads the optional
  `Idempotency-Key` header), `/restore`.
- **`EntitlementController`** — `GET /sdk/entitlements/check` (by `entitlementId` or `itemId`),
  `GET /sdk/entitlements`.
- **`AnalyticsController`** — `POST /sdk/analytics/events`.

### 4.5 Internal/admin controllers (`web/`)

Under `/api/v1/internal/**`, protected by `InternalAdminTokenFilter` (`X-Internal-Admin-Token`). Meant
for back-office tooling, not the portal UI.

- **`InternalItemController`** — create/patch items by `developerAppId`.
- **`InternalPurchaseController`** — list purchases with filters.
- **`InternalEntitlementController`** — manual grant/revoke.
- **`InternalAnalyticsController`** — basic count summary.

### 4.6 Portal controllers & services (`web/portal/`, `service/portal/`)

Under `/api/v1/portal/**`, protected by `PortalAuthFilter` (JWT). Controllers read the current user
from `PortalContext`, authorize app ownership via `PortalAppService.requireOwnedApp`, and delegate.

- **`PortalAuthController`** + **`PortalAuthService`** — `register`/`login` (public) issue a JWT;
  `me` returns the current user; `logout` is a no-op (stateless JWT, client drops the token).
  `register` hashes the password (PBKDF2) and rejects duplicate emails (`EMAIL_ALREADY_EXISTS`).
- **`PortalAppController`** + **`PortalAppService`** — app CRUD with ownership checks; `DELETE` is a
  **soft** deactivate (`isActive=false`). `requireOwnedApp` is the shared guard used by every
  sub-resource controller.
- **`PortalApiKeyController`** — list/create/revoke/rotate, delegating to `ApiKeyService`. `create`/
  `rotate` return the raw key once.
- **`PortalItemController`** + **`PortalItemService`** — list/create/get/update/disable/enable. The
  service auto-generates `itemId` (snake_case from name), auto-generates `entitlementId`
  (`ent_<itemId>`) for non-consumables, enforces per-app uniqueness (`ITEM_ID_ALREADY_EXISTS`), and
  stores `priceAmountMinor`.
- **`PortalPurchaseController`** + **`PortalPurchaseService`** — list (with filters) + detail. Detail
  includes the item, the granted entitlement, and the linked analytics events — but **never** the raw
  provider token (only `providerOrderId`).
- **`PortalEntitlementController`** + **`PortalEntitlementService`** — list + manual grant/revoke, which
  require `OWNER`/`ADMIN` (`VIEWER` gets `FORBIDDEN`). Delegates to the core `EntitlementService`.
- **`PortalAnalyticsController`** + **`PortalAnalyticsService`** — the dashboards:
  - `overview` — all KPI counts + conversions (division-by-zero safe) + revenue (SUCCESS purchases ×
    `priceAmountMinor`) + active entitlements.
  - `funnel` — popup → confirm → start → success, with % of top and % of previous, plus a cancel branch.
  - `revenue`, `revenue/by-product`, `revenue/by-time` (`groupBy=day|week|month`), `events`.
  - *Why a separate analytics service from the internal one:* the portal needs rich,
    filter-and-aggregate analytics; the internal `AnalyticsService.summary` is a minimal count helper.

### 4.7 Security & filters (`security/`)

Servlet filters run **before** controllers, in `@Order`. They're the authentication layer (this project
deliberately does **not** use Spring Security — hand-rolled filters are easier to read for learning).

- **`RequestIdFilter`** (`@Order HIGHEST_PRECEDENCE`) — resolves/creates `X-Request-Id`, puts it in
  `RequestContext` and the response header, and **clears** the context in a `finally` (prevents
  thread-local leakage across pooled requests).
- **`SdkApiKeyFilter`** (`@Order 10`) — only for `/api/v1/sdk/**`. Validates `X-SDK-API-Key` →
  `ApiKeyService.validateAndGetApp` → stores the app in `RequestContext`. Skips `OPTIONS` (CORS
  preflight). On failure writes the error envelope directly.
- **`InternalAdminTokenFilter`** (`@Order 11`) — only for `/api/v1/internal/**`. Compares
  `X-Internal-Admin-Token` to the configured token; **fails closed** if no token is configured.
- **`PortalAuthFilter`** (`@Order 12`) — only for `/api/v1/portal/**` except `register`/`login`.
  Verifies the JWT, loads the user, stores it in `PortalContext`.
- **`FilterSupport`** — shared helper to write an `ApiResponse` error from inside a filter (filters run
  before the `@RestControllerAdvice`, so they can't rely on it).
- **`PasswordHasher`** — PBKDF2-HMAC-SHA256 with per-password salt; constant-time compare. JDK-only (no
  dependency). Stored as `iterations:salt:hash`.
- **`JwtService`** — a **hand-rolled HS256 JWT** (header.payload.signature, base64url, HMAC-SHA256).
  `issue(userId, email, role)` and `verify(token)` (checks signature + `exp`). *Why hand-rolled:* avoids
  a JWT-library dependency and is transparent for learning. TODO: refresh tokens, revocation.
- **`PortalContext`** — `ThreadLocal` for the current `DeveloperUser`.

### 4.8 Config (`config/`, `resources/`)

- **`WebConfig`** — CORS so the Vite dev server (`portal-frontend-url`) can call the backend.
- **`SeedDataLoader`** — a dev/docker-only `CommandLineRunner` (`@ConditionalOnProperty
  app.seed.enabled`). Creates the demo user, app, fixed key `demo_api_key_123`, a **21-item catalog**,
  and (if `app.seed.sample-data`) a **realistic, deterministic history** of purchases, entitlements,
  and analytics from `app.seed.start-date` (default `2026-01-01`) to `app.seed.end-date` (blank ⇒
  today) shaped like a real app: a month-over-month **growth trend**, the monthly campaign story,
  weekend bumps, occasional **sale/viral spike days** (with a featured item that shifts the top
  seller), quiet days, and wide daily noise — so the graphs are jagged and believable. Idempotent
  (seeds only if the demo app is absent); on startup it also **auto-reseeds** when it finds an
  outdated/smaller catalog (e.g. an old 4-item seed left in a persistent Postgres volume), so upgrades
  aren't stuck on stale data. Also exposes a public `reset(boolean reseed)` that wipes all
  transactional data and optionally regenerates — used by the portal's "danger zone" (see §7).
  **Never runs in production.**
- **`application.yml`** (default), **`application-dev.yml`** (H2 + seed), **`application-docker.yml`**
  (Postgres). See §11.

---

## 5. API Documentation

All responses use the envelope `{ "success", "data", "error", "requestId" }`. Below, only `data` /
`error` are shown for brevity. Headers common to SDK calls: `X-SDK-API-Key`, optional `X-Request-Id`.

### Health (public)
`GET /api/v1/health` → `{ "status": "ok", "service": "purchase-sdk-backend" }`. *Caller:* anyone.
*DB:* none.

### SDK API (header `X-SDK-API-Key`) — *caller: the Android SDK*

#### `POST /api/v1/sdk/init`
Validate app, record `sdk_initialized`, return features.
Request: `{ "sdkVersion": "1.0.0", "packageName": "com.example.demogame", "userId": "user_123", "billingMode": "MOCK" }`
Response `data`: `{ "appId": "app_demo", "billingMode": "MOCK", "serverTime": "…", "features": { "mockBillingEnabled": true, "googlePlayBillingEnabled": false, "analyticsEnabled": true } }`
*DB:* insert `analytics_event`. *TODO:* validate package name / signing cert / Play Integrity.

#### `GET /api/v1/sdk/items` · `GET /api/v1/sdk/items/{itemId}`
List active items / fetch one. *DB:* read `purchase_item`. Errors: `ITEM_NOT_FOUND` (404).

#### `POST /api/v1/sdk/purchases/start`
Request: `{ "userId": "user_123", "itemId": "remove_ads", "billingMode": "MOCK" }`
Response: `{ "purchaseId": "pur_…", "status": "CREATED", "billingMode": "MOCK", "itemId": "remove_ads" }`
*DB:* insert `purchase` (CREATED) + `analytics_event` (`purchase_started`).

#### `POST /api/v1/sdk/purchases/confirm`  (header `Idempotency-Key`)
MOCK request: `{ "purchaseId": "pur_…", "userId": "user_123", "itemId": "remove_ads", "billingMode": "MOCK" }`
Success: `{ "purchaseId": "pur_…", "status": "SUCCESS", "entitlementGranted": true, "entitlement": { "entitlementId": "ent_remove_ads", "status": "ACTIVE", "expiresAt": null } }`
GOOGLE_PLAY request adds `"googlePlay": { "purchaseToken": "…", "orderId": "…", "productId": "…" }` and currently returns error `GOOGLE_PLAY_NOT_CONFIGURED` (501).
*DB:* update `purchase`; insert `entitlement`; insert `analytics_event`; insert `idempotency_record`.
*Errors:* `PURCHASE_NOT_FOUND`, `PURCHASE_CANCELLED`, `GOOGLE_PLAY_NOT_CONFIGURED`,
`PURCHASE_VERIFICATION_FAILED`.

#### `POST /api/v1/sdk/purchases/restore`
Request: `{ "userId": "user_123", "billingMode": "MOCK" }` → `{ "restoredPurchases": [...], "entitlements": [...] }`.
GOOGLE_PLAY → `GOOGLE_PLAY_NOT_CONFIGURED`. *DB:* read `purchase`, `entitlement`; insert `analytics_event`.

#### `GET /api/v1/sdk/entitlements/check?userId=&entitlementId=` (or `&itemId=`)
`{ "hasEntitlement": true, "entitlementId": "ent_remove_ads", "status": "ACTIVE", "expiresAt": null }`.
*DB:* read/maybe-update `entitlement` (lazy expiry); insert `analytics_event` (`entitlement_checked`).

#### `GET /api/v1/sdk/entitlements?userId=`
List a user's entitlements. *DB:* read `entitlement`.

#### `POST /api/v1/sdk/analytics/events`
Request: `{ "userId", "eventName", "billingMode", "itemId", "purchaseId", "metadata": {…} }` →
`{ "stored": true }`. *DB:* insert `analytics_event` (best-effort; failure never breaks the caller).

### Portal API (header `Authorization: Bearer <JWT>`) — *caller: the portal*

| Method · Path | Purpose | DB |
|---|---|---|
| `POST /portal/auth/register` · `/login` | get a JWT | insert/read `developer_user` |
| `GET /portal/auth/me` · `POST /logout` | current user / no-op | read `developer_user` |
| `GET·POST /portal/apps` | list/create apps | `developer_app` |
| `GET·PATCH·DELETE /portal/apps/{appId}` | read/update/deactivate | `developer_app` |
| `GET·POST /portal/apps/{appId}/api-keys` | list/create keys | `api_key` |
| `POST …/api-keys/{keyId}/revoke·rotate` | revoke/rotate | `api_key` |
| `GET·POST /portal/apps/{appId}/items` | list/create items | `purchase_item` |
| `GET·PATCH …/items/{itemId}` · `POST …/disable·enable` | read/update/toggle | `purchase_item` |
| `GET /portal/apps/{appId}/purchases` · `/{purchaseId}` | list/detail | `purchase`,`purchase_item`,`entitlement`,`analytics_event` |
| `GET /portal/apps/{appId}/entitlements` · `POST …/grant·revoke` | list / admin actions | `entitlement` |
| `GET …/analytics/overview·funnel·revenue·revenue/by-product·revenue/by-time·purchases-by-status·events` | dashboards | `analytics_event`,`purchase`,`purchase_item`,`entitlement` |
| `POST /portal/maintenance/reset-demo-data?reseed=` | wipe + optional re-seed (dev/demo only) | all |

Common query params: `from`,`to` (`yyyy-MM-dd`), `itemId`, `billingMode`, `groupBy=day|week|month`.
Errors: `UNAUTHORIZED` (401), `FORBIDDEN` (403), `APP_NOT_FOUND`, `ITEM_ID_ALREADY_EXISTS`,
`INVALID_CREDENTIALS`, `EMAIL_ALREADY_EXISTS`.

### Internal API (header `X-Internal-Admin-Token`)
`POST/PATCH /internal/items`, `GET /internal/purchases`, `POST /internal/entitlements/grant·revoke`,
`GET /internal/analytics/summary`. Fails closed if no admin token configured.

---

## 6. Android SDK Guide

Module `iap-sdk/`, package `com.example.iapsdk`. The SDK is **View-based** (not Compose) so any host
app can embed it. Public surface is tiny; everything else is `internal`.

> **Note:** the SDK's `ApiClient` makes **real HTTP calls** to the backend's `/api/v1/sdk/**` API
> (JDK `HttpURLConnection` + Gson). The methods below genuinely communicate with the backend; in MOCK
> mode the *payment* is simulated server-side but the purchase/entitlement/analytics rows are real.

### 6.1 Public API — `PurchaseSdk` (the facade)

**File:** `PurchaseSdk.kt`. **Visibility:** public `object` (singleton). This is the **only** entry
point. It holds a nullable `SdkRuntime` (built on `init`, dropped on `logout`) — so "initialized" is
just "is the runtime non-null".

| Function | Signature (simplified) | What it does |
|---|---|---|
| `init` | `init(context, apiKey, billingMode = MOCK, userId? = null, config = …)` | Validates the key, builds the runtime for the chosen billing mode, emits `sdk_initialized`. **Default mode is MOCK.** Only `applicationContext` is kept (no Activity leak). |
| `isInitialized` | `(): Boolean` | Has `init` run? |
| `getItem` | `suspend (itemId): PurchaseItem` | Load one item (cache-first). Throws `PurchaseException(ItemNotFound)`. |
| `getItems` | `suspend (): List<PurchaseItem>` | Load the full catalog from the backend. *(Added for the demo store.)* |
| `showPurchasePopup` | `(activity, itemId, listener)` | Loads the item, shows the bottom-sheet popup, drives the purchase, delivers the result to `PurchaseListener`. |
| `makePurchase` | `suspend (itemId, paymentMethodId? = null): PurchaseResult` | Headless purchase (no UI), for custom UIs. |
| `restorePurchases` | `suspend (): List<PurchaseResult>` | Restore prior purchases + refresh entitlements. |
| `hasEntitlement` | `suspend (itemId): Boolean` | Does the user have active access? Cache-first. |
| `listEntitlements` | `suspend (): List<UserEntitlement>` | All active entitlements. |
| `trackEvent` | `(eventName, properties = {})` | Send a custom analytics event. No-op if uninitialized. |
| `logout` | `()` | Clear user data + entitlement cache, tear down the runtime. |

**Errors:** suspend functions throw `PurchaseException` wrapping a **`PurchaseSdkError`** (sealed class:
`NotInitialized`, `ItemNotFound`, `BillingUnavailable`, `PurchaseCancelled`, `PurchaseFailed`,
`GooglePlayNotConfigured`, `VerificationRequired`, `Unknown`). The popup path delivers the same
`PurchaseSdkError` to `PurchaseListener.onPurchaseFailed`.

**Example usage (Kotlin):**
```kotlin
PurchaseSdk.init(context = applicationContext, apiKey = "psdk_live_…", billingMode = BillingMode.MOCK)

// Headless:
lifecycleScope.launch {
    val result = PurchaseSdk.makePurchase("remove_ads")
    if (PurchaseSdk.hasEntitlement("remove_ads")) unlockFeature()
}

// With UI:
PurchaseSdk.showPurchasePopup(this, "remove_ads", object : PurchaseListener {
    override fun onPurchaseSuccess(r: PurchaseResult) { /* unlock */ }
    override fun onPurchaseCancelled() {}
    override fun onPurchaseFailed(e: PurchaseSdkError) { /* show e.message */ }
})
```

### 6.2 Public models & contracts

- **`config/BillingMode`** (`MOCK`/`GOOGLE_PLAY`) and **`config/PurchaseSdkConfig`** (+`SdkEnvironment`):
  init options (environment, base URL, toggles for analytics/logs/cache).
- **`models/`**: `PurchaseItem`, `PurchaseItemType`, `PurchaseResult`, `PurchaseStatus`,
  `UserEntitlement`, `PurchaseSdkError` (sealed), `PurchaseException`. These are the public types the
  app sees.
- **`listener/PurchaseListener`** — popup callbacks (`onPurchaseSuccess/Cancelled/Failed`).
- **`callbacks/PurchasePopupCallback`** — Java-friendly callback for the low-level dialog.
- **`model/PurchasePopupData`** (note: singular `model`, distinct from plural `models`) — the data the
  low-level popup renders.

### 6.3 Internal implementation

These are `internal` — invisible to the host app. They're what the prompt calls "internal SDK
components".

- **`core/SdkRuntime`** — an immutable container built at `init`: holds the config, user id, billing
  mode, and all managers, plus a coroutine scope. Picks the provider by billing mode. *Why:* keeps
  `PurchaseSdk` a thin facade and makes teardown (`logout`) trivial.
- **`api/ApiClient`** — **the network seam: a real HTTP client.** Suspend functions `fetchItem`,
  `fetchItems`, `createPurchase`, `confirmPurchase`, `cancelPurchase`, `restorePurchases`,
  `fetchEntitlements`, `sendAnalyticsEvent`, implemented with JDK `HttpURLConnection` + Gson (no extra
  dependency). Sends `X-SDK-API-Key`, parses the `{ success, data, error }` envelope, and maps backend
  error codes to `PurchaseSdkError`. The base URL comes from `PurchaseSdkConfig.baseUrl` (e.g.
  `http://10.0.2.2:8080/api/v1/sdk` from the emulator). It bridges the backend's two-step purchase
  (`start` then `confirm`) by remembering the in-flight `(itemId, userId, idempotencyKey)` so the
  `confirmPurchase(purchaseId)` signature stays unchanged. *(The backend has no SDK cancel endpoint, so
  `cancelPurchase` just forgets the in-flight purchase locally — a `TODO`.)*
- **`billing/BillingProvider`** (interface: `getItem`, `getItems`, `makePurchase`, `restorePurchases`)
  + **`MockBillingProvider`** + **`GooglePlayBillingProvider`** — the SDK-side billing strategy.
  `PurchaseManager` picks one by `BillingMode`. `MockBillingProvider` runs the create→confirm against
  the backend via `ApiClient` and grants the entitlement locally for an instant UI update.
  `GooglePlayBillingProvider` throws `GooglePlayNotConfigured` for every method and is a TODO checklist
  (mirrors the backend stub).
- **`purchase/PurchaseManager`** — orchestrates `loadItem`/`loadItems`/`makePurchase`/
  `restorePurchases` over the chosen provider, wrapping each with start/success/failure analytics and
  error mapping.
- **`purchase/PurchaseValidator`** — precondition checks (initialized, api key, item id, price) that
  throw `PurchaseException`.
- **`entitlement/EntitlementManager`** — **network-first** `hasEntitlement`/`listEntitlements`: the
  backend is the source of truth, so reads refresh from `/sdk/entitlements` and fall back to the local
  cache only when offline. This is what makes the app reflect a portal **revoke**, a subscription
  **expiry**, or a **restore** instead of stale "owned" state. Also upserts optimistically after a
  purchase. Backed by an in-memory mirror + `LocalStorage`.
- **`storage/LocalStorage`** — the only thing touching `SharedPreferences`. Stores credentials, config,
  the item cache, and the entitlement cache as Gson JSON. *Why a cache:* `hasEntitlement` should be
  instant/offline-tolerant; the cache is the source for quick checks, refreshed from the API as needed.
- **`analytics/AnalyticsTracker`** — fire-and-forget `track*` helpers, each tagged with the active
  `billingMode`, launched on a scope; failures swallowed. Forwards events to the backend via
  `ApiClient` (`POST /api/v1/sdk/analytics/events`).
- **`error/ErrorMapper`** — `mapThrowableToSdkError(t)` → a stable `PurchaseSdkError` (the place to map
  future Google `BillingResponseCode`s).
- **`util/Ids`** — id/idempotency-key helpers.
- **`ui/PurchasePopupView`** — the bottom-sheet popup `View` (Material). `bind`/`bindItem`,
  `setLoadingState`, `showError`, `clearError`. Themes itself via a `ContextThemeWrapper`, so the host
  app needs no Material theme.
- **`ui/PurchasePopupController`** — hosts the view in a `Dialog`, wires Confirm → `PurchaseManager`,
  routes the outcome to the `PurchaseListener`. Contains the comment that **Google Play's final payment
  UI cannot be replaced**.
- **`ui/PurchaseDialog`** — a public, low-level convenience facade to show the popup with
  `PurchasePopupData` *without* `init`. It predates the full `PurchaseSdk` facade and is kept as an
  alternative entry point; the demo app no longer uses it.

### 6.4 The demo app (`app/`)

The demo is a **full multi-screen Compose app** that uses only the public `PurchaseSdk` facade
(no internal SDK classes). Structure (ViewModel/Repository style, no extra dependency):

- **`IapDemoApplication`** — calls `PurchaseSdk.init(...)` once at startup (MOCK mode, user
  `demo-user-001`, `baseUrl` from `DemoConfig`).
- **`demo/DemoConfig`** — backend base URL (`http://10.0.2.2:8080/api/v1/sdk`), API key
  `demo_api_key_123`, demo user/account ids, premium item id.
- **`demo/DemoViewModel`** + **`demo/DemoUiState`** — the single state owner and the *only* caller of
  the SDK (`getItems`, `listEntitlements`, `restorePurchases`, `hasEntitlement`, `trackEvent`). Screens
  are pure functions of the state.
- **`ui/DemoApp`** — a tiny hand-rolled router (no Navigation dependency) + top bar + message banner.
- **`ui/screens/`** — `HomeScreen`, `StoreScreen` (catalog + Buy → SDK popup), `EntitlementsScreen`,
  `RestoreScreen` (loading/success/error), `PremiumScreen` (`hasEntitlement` gate → "Unlock Now").
- **`MainActivity`** — hosts the UI and bridges Buy/Unlock taps to `PurchaseSdk.showPurchasePopup(...)`
  (the popup needs an Activity, so it lives here, not in the ViewModel).

The app manifest registers `IapDemoApplication` and references
`res/xml/network_security_config.xml`, which permits cleartext HTTP **only** to the dev hosts
(`10.0.2.2`, `localhost`). See [`DEMO_GUIDE.md`](DEMO_GUIDE.md) for the click-by-click demo script.

---

## 7. Portal Guide

Module `portal-web/`, React 18 + TypeScript + Vite + React Router v6 + TanStack Query + Axios +
Tailwind + Recharts.

### 7.1 Foundations

- **`src/main.tsx`** — bootstraps React, wraps the app in `QueryClientProvider` (TanStack Query),
  `BrowserRouter`, and `AuthProvider`.
- **`src/App.tsx`** — the **router**. Public routes `/login`, `/register`; protected routes wrapped in
  `ProtectedRoute`; app-scoped routes nested under `/apps/:appId/*` inside `Layout`.
- **`src/lib/api.ts`** — the Axios instance. A request interceptor attaches the JWT; a response
  interceptor redirects to `/login` on 401; `unwrap()` turns the `{success,data,error}` envelope into
  either `data` or a thrown `PortalApiError(code, message)`. *Why centralize:* every page calls
  `api.get/post/patch/del` and gets clean data or a typed error.
- **`src/lib/format.ts`** — `money(minor, currency)` (divides by 100!), `percent`, `dateTime`, `num`.
- **`src/types/index.ts`** — TypeScript mirrors of the backend DTOs (the same enums as `shared/types.ts`).
- **`src/auth/AuthContext.tsx`** — holds the current `user`, exposes `login`/`register`/`logout`. On
  mount, if a token exists, calls `/auth/me` to restore the session.
- **`src/hooks/usePortal.ts`** — **all** server state, as TanStack Query hooks (`useApps`, `useItems`,
  `useOverview`, `useRevenue`, `useCreateItem`, …). Mutations invalidate the relevant query keys so the
  UI refreshes automatically. *Why one file:* a single, discoverable data layer; pages stay declarative.
- **`src/components/`**: `ui.tsx` (Button, Card, Input, Select, Badge, Kpi, CopyButton, CodeBlock,
  Alert, …), `Layout.tsx` (sidebar + top bar with app selector + logout), `ProtectedRoute.tsx`
  (redirects to `/login` if unauthenticated), `Filters.tsx` (date/item/billing-mode/groupBy bar).

### 7.2 Pages (each is a route)

| Route | File | What it does |
|---|---|---|
| `/login`, `/register` | `LoginPage`, `RegisterPage` | Email/password forms → `AuthContext` → JWT. Login pre-fills demo creds. |
| `/apps` | `AppsPage` | Lists the user's apps as cards; "New app". |
| `/apps/new` | `NewAppPage` | Create-app form; validates package name `com.example.app`. |
| `/apps/:appId/dashboard` | `DashboardPage` | App summary, KPI cards, recent purchases/events, **setup checklist**. |
| `/apps/:appId/api-keys` | `ApiKeysPage` | List keys; create (shows raw **once** with a copy + warning); revoke/rotate; the "not a strong secret" note. |
| `/apps/:appId/items` | `ItemsPage` | Item table; copy `itemId`/`entitlementId`; enable/disable; view. |
| `/apps/:appId/items/new` | `NewItemPage` | Create item; auto snake_case `itemId`, auto `ent_…`, decimal price (e.g. `0.59`) converted to minor units, currency dropdown (`ILS/GBP/USD/EUR`); client validation. |
| `/apps/:appId/items/:itemId` | `ItemDetailPage` | Metadata, **SDK snippet**, per-item stats (purchases/revenue/conversion/entitlements), recent purchases. |
| `/apps/:appId/analytics` | `AnalyticsPage` | 10 KPI cards + the funnel bars; filters. |
| `/apps/:appId/analytics/revenue` | `RevenuePage` | Recharts: revenue over time, purchases over time, revenue by product, revenue by billing mode (pie), + the by-product table. |
| `/apps/:appId/purchases` | `PurchasesPage` | Purchases table with date/status/item/mode/user filters. |
| `/apps/:appId/purchases/:purchaseId` | `PurchaseDetailPage` | Full purchase, granted entitlement, linked events, failure reason; **no raw token**. |
| `/apps/:appId/entitlements` | `EntitlementsPage` | Entitlements table + filters; admin-only manual grant/revoke with a warning. |
| `/apps/:appId/sdk-setup` | `SdkSetupPage` | Copy-paste `init`/popup/entitlement snippets, MOCK-vs-GOOGLE_PLAY explanation, Google Play checklist. |
| `/settings` | `SettingsPage` | Account info. |

**State management:** local form state with `useState`; **server** state entirely via TanStack Query
(caching, refetch, invalidation). **Validation:** client-side in the forms (mirrors backend rules) plus
the backend's bean validation as the authority. **How developers do the key tasks:**

- *Create an API key:* ApiKeysPage → "Create key" → raw key shown once → copy.
- *Create an item:* NewItemPage → fill form → backend stores it → ItemsPage lists it.
- *View items:* ItemsPage (table) and ItemDetailPage (stats + snippet).
- *Analytics/revenue:* AnalyticsPage + RevenuePage read the backend's aggregates.

---

## 8. Purchase Flow

### Narrative (the intended full loop)

1. **Developer creates an app** in the portal → `POST /portal/apps` → `developer_app` row.
2. **Developer creates an item** → `POST /portal/apps/{id}/items` → `purchase_item` row (e.g.
   `remove_ads`, `199`, `ent_remove_ads`).
3. **Developer generates an API key** → `POST …/api-keys` → `api_key` row; raw key shown once.
4. **App initializes the SDK** → `PurchaseSdk.init(apiKey=…, billingMode=MOCK)`; emits
   `sdk_initialized` (`POST /sdk/analytics/events`).
5. **User opens the popup** → `showPurchasePopup` → `purchase_popup_shown` analytics event.
6. **User confirms** → `purchase_confirm_clicked` → `POST /sdk/purchases/start` (`CREATED`,
   `purchase_started`) then `POST /sdk/purchases/confirm` with an `Idempotency-Key`.
   (**Cancel** → `purchase_cancel_clicked`, no purchase.)
7. **Backend validates** the key (filter), the user, the item, and idempotency.
8. **Backend creates/confirms** the purchase → MOCK marks it `SUCCESS`.
9. **Backend grants the entitlement** (`ent_remove_ads`, `ACTIVE`) and emits `purchase_success`.
10. **SDK updates local state** → `EntitlementManager` caches the new entitlement in `LocalStorage`.
11. **App checks `hasEntitlement("remove_ads")`** → true → unlock.

> Steps 4–11 now run **for real over HTTP**: the SDK calls `/api/v1/sdk/**`, the backend writes the
> purchase/entitlement/analytics rows, and the portal reports on them. The only simulated part in MOCK
> mode is the payment itself (the backend marks the purchase `SUCCESS` without charging).

### Sequence diagram (MOCK, over HTTP)

```
App        PurchaseSdk     PopupController   PurchaseManager   ApiClient/Backend   EntitlementMgr
 │  show(...)   │               │                  │                 │                  │
 │─────────────▶│  showPopup    │                  │                 │                  │
 │              │──────────────▶│  (render sheet)  │                 │                  │
 │              │   track popup_shown ───────────────────────────────▶ analytics_event │
 │   user taps Confirm          │                  │                 │                  │
 │              │   track confirm_clicked ─────────────────────────── ▶                 │
 │              │               │  makePurchase    │                 │                  │
 │              │               │─────────────────▶│  start ────────▶│ insert purchase  │
 │              │               │                  │  confirm ──────▶│ SUCCESS          │
 │              │               │                  │                 │  grant ─────────▶│ insert entitlement
 │              │               │                  │◀── result ──────│                  │
 │              │◀── onPurchaseSuccess ────────────│                 │                  │
 │◀─ listener   │               │                  │                 │                  │
 │   hasEntitlement("remove_ads") ───────────────────────────────────────────────────▶ true
```

---

## 9. Analytics Flow

### What is collected and when

| Event | Emitted when | Sent by |
|---|---|---|
| `sdk_initialized` | `PurchaseSdk.init` | SDK / backend `/sdk/init` |
| `item_viewed` | a store item is shown | SDK demo (`trackEvent`) |
| `purchase_popup_shown` | popup opens | SDK popup controller |
| `purchase_confirm_clicked` | user taps Confirm | SDK |
| `purchase_cancel_clicked` | user cancels | SDK |
| `purchase_started` | `start` | backend `PurchaseService.startPurchase` |
| `purchase_success` | confirm succeeds | backend |
| `purchase_failed` | confirm fails | backend |
| `restore_started/success/failed` | restore | backend |
| `entitlement_checked` | `hasEntitlement`/check | backend `EntitlementService.checkEntitlement` |

**Which SDK functions send events:** `init`, `showPurchasePopup` (popup/confirm/cancel), `makePurchase`
(started/success/failed via `PurchaseManager`), `restorePurchases`, `hasEntitlement`, and the explicit
`trackEvent`. **Which endpoint receives them:** `POST /api/v1/sdk/analytics/events` (plus the backend
emits its own server-side events during start/confirm/restore). **Where stored:** the
`analytics_event` table.

### How metrics are computed (backend `PortalAnalyticsService`)

- **Counts** (popup opened, cancelled, completed, failed, …) = `COUNT` of events by name in range.
- **Conversion** (all division-by-zero safe):
  - popup→success = `purchase_success / purchase_popup_shown`
  - confirm→success = `purchase_success / purchase_confirm_clicked`
  - cancel rate = `purchase_cancel_clicked / purchase_popup_shown`
- **Revenue** = `Σ (priceAmountMinor)` over **SUCCESS** purchases (joined to `purchase_item`). Never
  failed/cancelled/pending.
- **Revenue by product** = group SUCCESS purchases by `itemId` (one row per item; powers the Revenue
  page table and the per-item stats on the item-detail page).
- **Revenue over time** = bucket SUCCESS purchases by day/week/month (`groupBy`).
- **Purchases by status** (`/analytics/purchases-by-status`) = `COUNT` of **purchase rows** grouped by
  `PurchaseStatus` (SUCCESS / FAILED / CANCELLED / PENDING / …) — from the authoritative purchase
  table, not events. Shown as a status breakdown card on the Analytics page.

All ranges default to the **last 12 months** (`DateRanges`), so the full seeded history is visible
without picking dates.

---

## 10. Billing Mode

### The two modes

- **`MOCK`** (default): the SDK calls the backend over HTTP and the **backend** simulates a successful
  purchase and grants the entitlement. No Google Play, no token, no money. For demos/tests/education.
  **Not proof of payment.**
- **`GOOGLE_PLAY`**: real Play billing. **Scaffolded only** — every real step is a TODO and the code
  **fails safe** rather than faking success.

### How the mode flows through the system

- The mode is chosen at `PurchaseSdk.init(billingMode=…)` (SDK) and/or per request (backend), defaulting
  to the app's `billingModeDefault`.
- **SDK**: `PurchaseManager` selects a `BillingProvider` — `MockBillingProvider` or
  `GooglePlayBillingProvider`. The latter throws `GooglePlayNotConfigured`.
- **Backend**: `PurchaseService.confirmPurchase` branches — MOCK simulates success;
  GOOGLE_PLAY calls `GooglePlayVerificationService`, which returns `NOT_CONFIGURED` → the request fails
  with `GOOGLE_PLAY_NOT_CONFIGURED` and the purchase is marked `REQUIRES_VERIFICATION`.
- Every analytics event is tagged with the mode, so funnels can be split by mode.

### Where the Google TODOs live

- SDK: `billing/GooglePlayBillingProvider.kt`, `error/ErrorMapper.kt` (response-code mapping).
- Backend: `service/googleplay/GooglePlayVerificationService.java`, `PurchaseService` (acknowledge/
  consume), `EntitlementService` (real subscription expiry), `ItemService`/`PurchaseItem`
  (`googlePlayProductId`).

### What real production support still needs

1. Add the Google Play Billing dependency + `BillingClient` in the Android app/SDK; launch
   `launchBillingFlow`.
2. Create Play Console products; set `googlePlayProductId` on items.
3. Backend: Google Play Developer API client + service-account credentials.
4. **Verify the purchase token** (package, product, purchase state, no reuse) **before** granting.
5. Acknowledge non-consumables/subscriptions; consume consumables.
6. Read subscription expiry/renewal from verified data; handle refunds/cancellations.
7. Real-time Developer Notifications via Pub/Sub. **Only grant after verification succeeds.**

---

## 11. Configuration & Environment Variables

### Backend (`backend/src/main/resources/`)

- **`application.yml`** (default): app name; default profile `dev`; H2 datasource; JPA `ddl-auto:
  update`; Jackson (ISO dates, keep nulls); and an `app:` block:
  - `app.internal-admin-token` ← `INTERNAL_ADMIN_TOKEN` (blank → internal endpoints fail closed).
    **TODO:** move to a secret manager.
  - `app.mock-subscription-days: 30` (MOCK subscription demo expiry).
  - `app.jwt-secret` ← `JWT_SECRET`. **TODO:** must be a long random secret in prod.
  - `app.jwt-ttl-seconds: 86400`.
  - `app.api-key-pepper` ← `API_KEY_PEPPER`. **TODO:** per-env random pepper.
  - `app.portal-frontend-url` ← `PORTAL_FRONTEND_URL` (CORS origin).
- **`application-dev.yml`**: H2 console on; `app.seed.enabled: true`, `app.seed.sample-data: true`,
  `app.seed.start-date`/`end-date` (← `SEED_START_DATE`/`SEED_END_DATE`, default `2026-01-01`→today);
  **insecure dev defaults** for `jwt-secret`/`api-key-pepper`/`internal-admin-token` so it boots with no
  env. **TODO:** never use these defaults outside local dev.
- **`application-docker.yml`**: Postgres datasource from `DATABASE_URL`/`DATABASE_USER`/
  `DATABASE_PASSWORD`; seed toggles (including the same `start-date`/`end-date`).

### Root (`.env.example`, `docker-compose.yml`)

Env vars: `DATABASE_NAME/USER/PASSWORD/URL`, `JWT_SECRET`, `API_KEY_PEPPER`, `INTERNAL_ADMIN_TOKEN`,
`PORTAL_FRONTEND_URL`, `VITE_API_BASE_URL`. The compose file passes these to the containers with
**insecure dev defaults**. **TODO:** generate strong random values for any real deployment; never commit
real secrets.

### Portal (`portal-web/`)

- `.env` (`VITE_API_BASE_URL`, default `http://localhost:8080`) — the backend base URL, baked in at
  build time. `vite-env.d.ts` types it.

### SDK config

- `PurchaseSdkConfig` (environment, base URL, analytics/logs/cache toggles). The demo sets
  `baseUrl = "http://10.0.2.2:8080/api/v1/sdk"` in `DemoConfig`; the demo app's
  `res/xml/network_security_config.xml` allows cleartext to the dev hosts only.

### Secrets/placeholders requiring future work (all TODO)

- Google Play Developer API service-account credentials (backend verification).
- A real `JWT_SECRET`, `API_KEY_PEPPER`, `INTERNAL_ADMIN_TOKEN`.
- Play Integrity / signing-cert / package-name validation material for `SdkApiKeyFilter`.

---

## 12. How to Run the Project

### Everything via Docker Compose (easiest)
```bash
cp .env.example .env            # optional locally
docker compose up --build
# Portal:  http://localhost:5173   (login demo@example.com / password123)
# Backend: http://localhost:8080   (health: /api/v1/health)
# Postgres: localhost:5432
```
Backend uses the `docker` profile (Postgres) and seeds demo data. Watch it come up:
`docker compose logs -f backend` → look for `Started PurchaseBackendApplication` and the seed line.

### Backend only (H2, no Docker)
```bash
cd backend && mvn spring-boot:run     # dev profile: in-memory H2 + seed
```

### Portal only
```bash
cd portal-web && cp .env.example .env && npm install && npm run dev   # :5173
```

### SDK demo app
Open the repo root in Android Studio (it's a Gradle project: `app`, `iap-sdk`), let it sync, run the
`app` configuration on an **emulator** (it targets `http://10.0.2.2:8080` — the emulator's alias for
your host's `localhost` — so start the backend first). For a physical device, set
`DemoConfig.BACKEND_SDK_BASE_URL` to your computer's LAN IP. The demo is a full multi-screen flow
(Home → Store → Entitlements → Restore → Premium) driving the SDK against the backend; see
[`DEMO_GUIDE.md`](DEMO_GUIDE.md) for the step-by-step script.

### Testing API calls manually (MOCK end-to-end via backend)
```bash
KEY=demo_api_key_123
curl -s localhost:8080/api/v1/sdk/items -H "X-SDK-API-Key: $KEY"
curl -s -X POST localhost:8080/api/v1/sdk/purchases/start -H "X-SDK-API-Key: $KEY" \
  -H 'Content-Type: application/json' \
  -d '{"userId":"user_1","itemId":"remove_ads","billingMode":"MOCK"}'
# take the purchaseId from the response, then:
curl -s -X POST localhost:8080/api/v1/sdk/purchases/confirm -H "X-SDK-API-Key: $KEY" \
  -H 'Idempotency-Key: k1' -H 'Content-Type: application/json' \
  -d '{"purchaseId":"pur_xxx","userId":"user_1","itemId":"remove_ads","billingMode":"MOCK"}'
curl -s "localhost:8080/api/v1/sdk/entitlements/check?userId=user_1&itemId=remove_ads" -H "X-SDK-API-Key: $KEY"
```

### Resetting the database
- **From the portal (any DB):** *Settings → Danger zone* → **Reset & regenerate demo data** (or
  **Delete all data**). Calls `POST /api/v1/portal/maintenance/reset-demo-data` (dev/demo only).
- **H2 (dev):** in-memory — just restart the backend.
- **Postgres (docker):** `docker compose down -v && docker compose up --build` (the `-v` drops the
  `pgdata` volume, so the seed runs fresh).

---

## 13. Testing Guide

> The repo ships **no automated tests yet** — this is the single biggest quality gap (see §15). Below is
> how you *should* test, with concrete cases. Adding these is an excellent learning exercise.

### Unit tests (backend, JUnit + Mockito)
- `EntitlementService.grantForPurchase`: NON_CONSUMABLE grants once and is idempotent; SUBSCRIPTION sets
  `expiresAt ≈ now+30d`; CONSUMABLE without `entitlementId` grants nothing.
- `ApiKeyService.hashApiKey`: same input → same hash; different pepper → different hash.
- `PasswordHasher`: `matches(raw, hash(raw))` is true; wrong password false.
- `JwtService`: `verify(issue(...))` returns claims; a tampered token → null; an expired token → null.
- `BillingModes.parseOrNull`: `"MOCK"`→enum, `""`→null, `"x"`→`ApiException(BILLING_MODE_NOT_SUPPORTED)`.

### Integration tests (`@SpringBootTest` + MockMvc, H2)
- Register → login → create app → create key → create item → `/sdk/...` start+confirm → entitlement
  exists. (The full happy path.)
- `confirm` twice with the same `Idempotency-Key` → identical response, **one** entitlement.
- `confirm` in GOOGLE_PLAY mode → 501 `GOOGLE_PLAY_NOT_CONFIGURED`, purchase `REQUIRES_VERIFICATION`.
- Wrong/missing `X-SDK-API-Key` → 401 `INVALID_API_KEY`.

### Manual API tests
Use the curl block in §12. Also test error paths: unknown item (`ITEM_NOT_FOUND`), duplicate item id
(`ITEM_ID_ALREADY_EXISTS`), portal call without JWT (`UNAUTHORIZED`).

### Portal manual tests
Register/login; create app; create key (verify it's shown once and copyable); create items of each
type; toggle disable/enable; open Analytics/Revenue and confirm charts render (seed data exists in
dev/docker); grant/revoke an entitlement as OWNER vs VIEWER (VIEWER blocked); *Settings → Danger zone*
→ **Reset & regenerate** and confirm the dataset rebuilds.

### SDK / demo-app manual tests
Run the demo app against a running backend (see the checklist in [`DEMO_GUIDE.md`](DEMO_GUIDE.md)):
Store loads from the backend; Buy → SDK popup → Confirm writes a `SUCCESS` purchase + entitlement
(visible in the portal); Cancel writes none; Restore lists prior purchases; the Premium screen gates
on `hasEntitlement`. Also: rotate the device (popup survives) and check dark mode.

### Purchase / analytics / entitlement test ideas
- Purchase: start→confirm grants; cancel grants nothing.
- Analytics: after N popups and M confirms, `overview` shows the right counts and conversion = M/N.
- Entitlement: subscription with a past `expiresAt` → `check` flips it to `EXPIRED` and returns
  `hasEntitlement=false`.

---

## 14. Common Bugs & Debugging

| Symptom | Likely cause | How to debug / fix |
|---|---|---|
| **Backend won't connect to DB** | Postgres not ready; wrong `DATABASE_URL`; stale `pgdata` volume from older settings | `docker compose logs postgres`; ensure healthcheck passes; `docker compose down -v` to reset. The `FATAL: database "purchase" does not exist` line is the **healthcheck** probing the wrong db name (benign; fixed by `-d` in the healthcheck). |
| **API key invalid (`INVALID_API_KEY`)** | Wrong/blank `X-SDK-API-Key`; key `REVOKED`; **pepper mismatch** (key created with a different `API_KEY_PEPPER` than the server now uses) | Confirm the header; check `api_key.status`; ensure `API_KEY_PEPPER` is stable across runs (changing it invalidates all existing hashes). In dev the seed key is `demo_api_key_123`. |
| **Purchase not created** | `userId`/`itemId` blank; item inactive/unknown; wrong billing mode string | Check the 400/404 code in the response `error.code`; verify the item exists and `isActive`. |
| **Entitlement not updated** | Item has no `entitlementId` (consumable); confirm returned an error; reading the wrong `userId` | Check `confirm` actually returned `SUCCESS`; verify the item's `entitlementId`; query `/sdk/entitlements?userId=`. |
| **Portal shows no items** | Looking at the wrong app; not logged in (401 redirect); backend down | Check the app selector; open dev tools network tab; confirm `GET …/items` returns data. |
| **SDK can't reach backend** (Store shows "Is the backend running?") | Backend down; running on a physical device (can't reach `10.0.2.2`); wrong `DemoConfig.BACKEND_SDK_BASE_URL`; cleartext blocked | Start the backend; use an **emulator** (its `10.0.2.2` aliases the host's `localhost`) or set the LAN IP for a device; the demo's `network_security_config.xml` already permits cleartext to `10.0.2.2`/`localhost`. Check Logcat tag `IapApiClient`. |
| **Analytics not saved** | Event name blank; backend error swallowed | `AnalyticsService.record` logs failures — check backend logs; confirm `POST /sdk/analytics/events` returns `{stored:true}`. |
| **Docker image platform error** | `eclipse-temurin:17-jre-alpine` has no arm64 build (Apple Silicon) | Use the multi-arch `eclipse-temurin:17-jre` (already fixed in `backend/Dockerfile`). |
| **Portal analytics / purchase-detail 500 *after a purchase* (Postgres only)** | `@Lob String` (`metadataJson`/`responseJson`) mapped to a Postgres `oid` large object; reading it outside a transaction throws `Large Objects may not be used in auto-commit mode`. Only triggers once a non-null metadata row exists (a purchase) — the seed writes null, and H2 never reproduces it. | Already fixed: those fields are plain `varchar` (no `@Lob`). Because `ddl-auto: update` won't convert an existing `oid` column, **drop the volume** so the column is recreated: `docker compose down -v && docker compose up --build`. |
| **Stale code/data in Docker** | `docker compose up` reuses cached images; the `pgdata` volume keeps old seed data (e.g. only 4 items) | Always `docker compose up --build` to rebuild images; `down -v` to drop the volume. The seeder also **auto-reseeds** if it finds an outdated (smaller) catalog. |
| **Portal 401 loops** | Expired/missing JWT | `api.ts` redirects to `/login` on 401; just log in again. JWT TTL is 24h. |

---

## 15. Code Quality Review

### What's well organized
- **Clear layering** (filters → controllers → services → repositories) and a **single response
  envelope** + **single error vocabulary** (`ErrorCode`) make the backend predictable.
- **Three auth schemes cleanly separated** by URL prefix and dedicated filters.
- **Money handled correctly** (`priceAmountMinor`, not display strings).
- **Security basics done right**: passwords PBKDF2-salted, API keys stored hashed with a pepper, raw
  key shown once, internal endpoints fail closed.
- **SDK has a genuinely small public surface** with everything else `internal`.
- **Portal data layer is centralized** (`usePortal.ts` + `api.ts`), so pages are declarative.
- **Google Play is honestly stubbed** — it never fakes success.

### What could be improved / where responsibilities blur
- **SDK networking is hand-rolled** (`HttpURLConnection` + Gson). This deliberately avoids adding a
  dependency, but a real SDK would use Retrofit/OkHttp (or Ktor) for connection pooling, interceptors,
  retries, and timeouts. The transport is isolated in `ApiClient`, so swapping it is localized.
- **Two layers still simulate billing**: the SDK calls the backend for real, but the *payment* is faked
  in both `MockBillingProvider` (grants locally for instant UI) and the backend (`MockBillingService`).
  That's intentional for MOCK mode; real billing replaces both with verified Google Play results.
- **Duplicate item logic**: `ItemService` (SDK/internal) vs `PortalItemService` (portal). They overlap;
  a single `ItemService` with portal-specific helpers would reduce drift.
- **Analytics aggregation in Java memory**: `PortalAnalyticsService` loads rows and streams. Correct but
  O(rows). At scale this should be SQL `GROUP BY` / a reporting table.
- **Soft foreign keys**: string references with integrity enforced in code. Easy to read, but real FKs
  (or at least DB constraints) would prevent orphan rows.
- **No transactions around multi-write flows**: `confirmPurchase` does several independent saves
  (purchase, entitlement, analytics, idempotency). A crash mid-way can leave partial state. Deliberate
  (so a thrown `GOOGLE_PLAY_NOT_CONFIGURED` still persists status), but a real system needs careful
  transaction boundaries + an outbox for analytics.
- **Duplicated enums/DTOs across 3 languages** (Java, Kotlin, TS). `shared/types.ts` documents the
  contract but nothing enforces it. Codegen (OpenAPI) would keep them in sync.
- **No tests** (see §13).
- **JWT/auth are hand-rolled** — fine for learning, but a real product should use a vetted library
  (and refresh tokens, revocation).

### Where duplication exists
- MOCK billing (backend + SDK). Item mapping/validation (two services). Enums/DTOs (three languages).
  `ErrorMapper` and `BillingMode`/`PurchaseStatus` concepts exist in both backend and SDK.

### Suggestions for cleaner architecture
1. Swap the hand-rolled `HttpURLConnection` transport in `ApiClient` for Retrofit/OkHttp or Ktor
   (timeouts, retries, interceptors). *(The SDK is already wired to the backend over HTTP.)*
2. Generate clients/types from an OpenAPI spec to kill the tri-language duplication.
3. Introduce real FKs + `@Transactional` boundaries (and an analytics outbox).
4. Move analytics aggregation into SQL or a small reporting layer.
5. Add a test suite (unit + integration) and CI.

---

## 16. Future Improvements

- **Real Google Play Billing** — implement both stubs (`GooglePlayBillingProvider`,
  `GooglePlayVerificationService`); verify tokens server-side before granting; acknowledge/consume;
  subscriptions/refunds/renewals; Pub/Sub notifications.
- **Better authentication** — vetted JWT lib, refresh tokens, email verification, password reset, OAuth,
  MFA; rate limiting per API key + Play Integrity.
- **Developer accounts / teams** — multiple users per app, roles, invitations, audit logs (especially
  for manual entitlement changes — already a TODO).
- **Better analytics dashboard** — SQL-backed aggregates, cohort/retention, real cancellation-rate-over-
  time chart, CSV export, date-range presets.
- **Refunds & subscription lifecycle** — refund → revoke entitlement; renewal/expiry jobs; grace periods.
- **Webhooks** — notify the developer's server on purchase/entitlement changes.
- **Admin panel** — the internal endpoints exist; add a guarded UI on top.
- **Better SDK docs & a complete demo** — wire the demo app to `PurchaseSdk` + the backend; publish the
  SDK as an artifact; KDoc.
- **More tests + CI/CD** — unit, integration, contract tests; GitHub Actions.
- **Production deployment** — real secrets management, migrations (Flyway/Liquibase instead of
  `ddl-auto: update`), HTTPS/ingress, observability (metrics/tracing/structured logs), backups.

---

---

## Appendix A — Complete File Index (every file, nothing skipped)

This appendix gives **one dedicated entry per file** so no file is skipped. Format:
**`path`** — what it is · responsibility · key detail · *(TODO)* where relevant. Sections §4/§6/§7
above give the deeper "why"; this is the exhaustive checklist.

### A.1 Backend — Java (`backend/src/main/java/com/example/purchasebackend/`)

**Entry point**
- **`PurchaseBackendApplication.java`** — `@SpringBootApplication` main class; component-scans the whole
  package. Boots the embedded server.

**`common/` (response & error plumbing)**
- **`ApiResponse.java`** — generic record `{success,data,error,requestId}`; factories `ok`/`fail`. The
  universal success/error envelope.
- **`ApiError.java`** — record `{code,message,details}`; built from an `ErrorCode`.
- **`ErrorCode.java`** — enum of every stable error code → `HttpStatus` + default message. The error
  vocabulary (e.g. `ITEM_NOT_FOUND`, `GOOGLE_PLAY_NOT_CONFIGURED`, `INVALID_CREDENTIALS`).
- **`ApiException.java`** — `RuntimeException` carrying an `ErrorCode` (+message/details); thrown by all
  business code.
- **`GlobalExceptionHandler.java`** — `@RestControllerAdvice`; maps `ApiException`/validation/unexpected
  errors to the envelope with the right status. Logs unexpected ones.
- **`RequestContext.java`** — `ThreadLocal` holder for `requestId` + the resolved `DeveloperApp` (SDK
  requests). Cleared per request.
- **`Ids.java`** — `newId(prefix)` → `prefix_<12 hex>`. All entity ids minted here.

**`domain/` (JPA entities — map 1:1 to tables in §3)**
- **`DeveloperUser.java`** — table `developer_user`; portal account (email unique, PBKDF2 hash, role).
- **`DeveloperApp.java`** — table `developer_app`; a project; `ownerUserId`, `billingModeDefault`,
  `isActive`. *(TODO in header: validate package name ownership / signing cert.)*
- **`ApiKey.java`** — table `api_key`; one app's SDK key; stores `keyHash` (not raw), `keyPrefix`,
  status, `lastUsedAt`.
- **`PurchaseItem.java`** — table `purchase_item`; catalog row; `priceAmountMinor` (money) vs
  `priceDisplay` (text). *(TODO: GOOGLE_PLAY prices from ProductDetails.)*
- **`Purchase.java`** — table `purchase`; one attempt; status/provider/idempotencyKey; `@PrePersist`
  keeps a pre-set `createdAt` (for seeding).
- **`Entitlement.java`** — table `entitlement`; access record; status + `expiresAt`.
- **`AnalyticsEvent.java`** — table `analytics_event`; one event; `metadataJson` (plain `varchar`,
  **not** `@Lob` — avoids the Postgres `oid` large-object trap); back-dateable `createdAt`.
- **`IdempotencyRecord.java`** — table `idempotency_record`; stored response per `(app, key)`
  (`responseJson` is plain `varchar`, not `@Lob`, for the same Postgres reason).

**`domain/enums/`** — string-stored enums:
- **`BillingMode.java`** `{MOCK, GOOGLE_PLAY}` · **`BillingProviderType.java`** `{MOCK, GOOGLE_PLAY}` ·
  **`ItemType.java`** `{NON_CONSUMABLE, CONSUMABLE, SUBSCRIPTION}` · **`PurchaseStatus.java`**
  `{CREATED,PENDING,SUCCESS,FAILED,CANCELLED,REQUIRES_VERIFICATION}` · **`EntitlementStatus.java`**
  `{ACTIVE,EXPIRED,REVOKED}` · **`DeveloperUserRole.java`** `{OWNER,ADMIN,VIEWER}` ·
  **`ApiKeyStatus.java`** `{ACTIVE,REVOKED}`.

**`repository/` (Spring Data JPA interfaces — one table each)**
- **`DeveloperUserRepository.java`** — `findByEmailIgnoreCase`, `existsByEmailIgnoreCase`.
- **`DeveloperAppRepository.java`** — `findByOwnerUserIdOrderByCreatedAtDesc`, `findByIdAndOwnerUserId`.
- **`ApiKeyRepository.java`** — `findByKeyHash`, `findByDeveloperAppIdOrderByCreatedAtDesc`,
  `findByIdAndDeveloperAppId`.
- **`PurchaseItemRepository.java`** — by app/itemId, active-only, exists-check.
- **`PurchaseRepository.java`** — by id+app, by app+user(+status), by app(+status).
- **`EntitlementRepository.java`** — latest by `(app,user,entitlementId)`, by app, count by status.
- **`AnalyticsEventRepository.java`** — between dates, top-200 recent, by purchaseId, count by name.
- **`IdempotencyRecordRepository.java`** — `findByDeveloperAppIdAndIdempotencyKey`.

**`dto/sdk/` (SDK request/response records)**
- **`InitRequest.java`/`InitResponse.java`/`FeaturesDto.java`** — `/sdk/init` payloads + feature flags.
- **`ItemDto.java`/`ItemsResponse.java`** — item views for the SDK.
- **`StartPurchaseRequest.java`/`StartPurchaseResponse.java`** — `/purchases/start`.
- **`ConfirmPurchaseRequest.java`/`GooglePlayConfirmData.java`/`ConfirmPurchaseResponse.java`/
  `EntitlementSummaryDto.java`** — `/purchases/confirm` (+ embedded Google data + granted entitlement).
- **`RestoreRequest.java`/`RestoredPurchaseDto.java`/`RestoreResponse.java`** — `/purchases/restore`.
- **`CheckEntitlementResponse.java`/`EntitlementDto.java`/`EntitlementsResponse.java`** — entitlement
  reads.
- **`TrackEventRequest.java`/`TrackEventResponse.java`** — analytics ingestion.

**`dto/internal/`** — **`CreateItemRequest`/`UpdateItemRequest`/`AdminItemDto`** (admin item CRUD),
  **`AdminPurchaseDto`** (admin purchase view, no token), **`GrantEntitlementRequest`/
  `RevokeEntitlementRequest`/`EntitlementActionResponse`** (manual grant/revoke),
  **`AnalyticsSummaryResponse`** (count summary).

**`dto/portal/` (grouped holder classes with nested records)**
- **`AuthDtos.java`** — `RegisterRequest`, `LoginRequest`, `UserDto`, `AuthResponse`.
- **`AppDtos.java`** — `CreateAppRequest`/`UpdateAppRequest` (package-name regex), `AppDto`.
- **`ApiKeyDtos.java`** — `CreateApiKeyRequest`, `ApiKeyDto`, `CreatedApiKeyResponse` (raw key, once).
- **`ItemDtos.java`** — `CreateItemRequest`/`UpdateItemRequest`/`PortalItemDto` (with `priceAmountMinor`).
- **`PurchaseDtos.java`** — `PortalPurchaseDto`, `PurchaseDetailDto`, nested event/entitlement views.
- **`EntitlementDtos.java`** — `PortalEntitlementDto`, `GrantRequest`, `RevokeRequest`.
- **`AnalyticsDtos.java`** — `OverviewResponse`, `FunnelResponse`/`FunnelStep`, revenue summary/by-mode/
  by-time/by-product, `EventDto`.

**`dto/common/`** — **`HealthResponse.java`** — `{status,service}` for the health endpoint (unwrapped).

**`service/` (business logic)**
- **`ApiKeyService.java`** — hash/validate keys; create/list/revoke/rotate; stamps `lastUsedAt`. *(TODOs:
  Play Integrity, package/cert validation, rate limiting.)* Touches `api_key`, `developer_app`.
- **`DeveloperAppService.java`** — `getById` for internal endpoints.
- **`ItemService.java`** — SDK/internal catalog (list/get/create/update). Touches `purchase_item`.
- **`PurchaseService.java`** — start/confirm/restore; idempotency; MOCK vs GOOGLE_PLAY branch. **The core
  flow.** Touches `purchase`,`purchase_item`,`entitlement`,`idempotency_record`,`analytics_event`.
- **`EntitlementService.java`** — grant rules (non-consumable/subscription/consumable), check (lazy
  expiry), list, manual grant/revoke. Touches `entitlement`. *(TODO: audit log; real sub expiry.)*
- **`AnalyticsService.java`** — `record` (never throws) + `summary`. Touches `analytics_event`.
- **`IdempotencyService.java`** — find/store idempotent responses. Touches `idempotency_record`.
- **`MockBillingService.java`** — flips a purchase to SUCCESS (demo only).
- **`ErrorMapper.java`** — classify throwable → `ErrorCode`/`ApiError` (used by the handler).
- **`service/googleplay/GooglePlayVerificationService.java`** — **stub**; returns `notConfigured()`;
  logs only a shortened token. *(The big Google TODO list.)*
- **`service/googleplay/GooglePlayVerificationRequest.java`** — verification input record.
- **`service/googleplay/VerificationResult.java`** — `{NOT_CONFIGURED, VERIFIED, FAILED}` + factories.
- **`service/mapper/DtoMapper.java`** — entity→DTO for SDK/internal.
- **`service/support/BillingModes.java`** — parse mode string (`parseOrDefault`/`parseOrNull`).
- **`service/support/DateRanges.java`** — resolve from/to → Instant range (default last 365 days).
- **`service/portal/PortalAuthService.java`** — register/login → JWT; password hashing; duplicate-email
  guard. Touches `developer_user`.
- **`service/portal/PortalAppService.java`** — app CRUD + `requireOwnedApp` ownership guard.
- **`service/portal/PortalItemService.java`** — portal item CRUD; auto itemId/entitlementId; uniqueness.
- **`service/portal/PortalPurchaseService.java`** — purchase list/detail (no raw token).
- **`service/portal/PortalEntitlementService.java`** — list + manual grant/revoke (OWNER/ADMIN only).
- **`service/portal/PortalAnalyticsService.java`** — overview/funnel/revenue aggregations from DB;
  division-by-zero safe; revenue from SUCCESS × `priceAmountMinor`.

**`security/` (servlet filters + crypto)**
- **`RequestIdFilter.java`** (`@Order` highest) — request id in/out; clears `RequestContext`.
- **`SdkApiKeyFilter.java`** (`@Order 10`) — validates `X-SDK-API-Key` for `/sdk/**`; skips `OPTIONS`.
- **`InternalAdminTokenFilter.java`** (`@Order 11`) — `X-Internal-Admin-Token` for `/internal/**`; fails
  closed.
- **`PortalAuthFilter.java`** (`@Order 12`) — JWT for `/portal/**` (except register/login); skips
  `OPTIONS`.
- **`FilterSupport.java`** — writes an error envelope from inside a filter.
- **`PasswordHasher.java`** — PBKDF2-HMAC-SHA256, salted, constant-time compare.
- **`JwtService.java`** — hand-rolled HS256 issue/verify. *(TODO: refresh tokens, revocation.)*
- **`PortalContext.java`** — `ThreadLocal` current `DeveloperUser`.

**`config/`**
- **`WebConfig.java`** — CORS for the portal origin.
- **`SeedDataLoader.java`** — dev/docker seed: user/app/key + **21-item catalog** + a deterministic,
  realistic history of purchases/entitlements/analytics from `app.seed.start-date`→`end-date`
  (default `2026-01-01`→today). Idempotent; also exposes `reset(reseed)` (wipe + optional regen).
  Gated by `app.seed.enabled`. *(TODO: replace with portal-created data; multi-currency.)*

**`web/` (SDK + internal controllers)**
- **`HealthController.java`** — `GET /api/v1/health` (public).
- **`SdkInitController.java`** — `POST /sdk/init`. *(TODOs: package/cert/Integrity validation.)*
- **`ItemController.java`** — `GET /sdk/items`, `/sdk/items/{itemId}`.
- **`PurchaseController.java`** — `POST /sdk/purchases/start|confirm|restore` (reads `Idempotency-Key`).
- **`EntitlementController.java`** — `GET /sdk/entitlements/check`, `/sdk/entitlements`.
- **`AnalyticsController.java`** — `POST /sdk/analytics/events`.
- **`InternalItemController.java`** — `POST/PATCH /internal/items`.
- **`InternalPurchaseController.java`** — `GET /internal/purchases`.
- **`InternalEntitlementController.java`** — `POST /internal/entitlements/grant|revoke`.
- **`InternalAnalyticsController.java`** — `GET /internal/analytics/summary`.

**`web/portal/` (JWT controllers)**
- **`PortalAuthController.java`** — `/portal/auth/register|login|logout|me`.
- **`PortalAppController.java`** — apps CRUD (delete = soft deactivate).
- **`PortalApiKeyController.java`** — keys list/create/revoke/rotate.
- **`PortalItemController.java`** — items list/create/get/update/disable/enable.
- **`PortalPurchaseController.java`** — purchases list/detail.
- **`PortalEntitlementController.java`** — entitlements list/grant/revoke.
- **`PortalAnalyticsController.java`** — overview/funnel/revenue(+by-product/by-time)/events.
- **`PortalMaintenanceController.java`** — `POST /portal/maintenance/reset-demo-data?reseed=` (wipe +
  optional re-seed). `@ConditionalOnProperty app.seed.enabled` ⇒ absent in production (fail-safe).

### A.2 Backend — resources (`backend/src/main/resources/`)
- **`application.yml`** — default config: H2, JPA, Jackson, `app.*` (admin token, mock-sub-days,
  jwt-secret, api-key-pepper, portal-frontend-url). *(TODOs: real secrets.)*
- **`application-dev.yml`** — dev profile: H2 console, seed on, **insecure dev defaults**.
- **`application-docker.yml`** — docker profile: Postgres datasource + seed toggles.

### A.3 Android SDK — Kotlin (`iap-sdk/src/main/java/com/example/iapsdk/`)

**Public facade & models**
- **`PurchaseSdk.kt`** — **PUBLIC** singleton facade: `init/isInitialized/getItem/getItems/
  showPurchasePopup/makePurchase/restorePurchases/hasEntitlement/listEntitlements/trackEvent/logout`.
  Holds an `SdkRuntime`. *(`getItems` added for the demo store.)*
- **`config/BillingMode.kt`** — **PUBLIC** `{MOCK, GOOGLE_PLAY}` (default MOCK).
- **`config/PurchaseSdkConfig.kt`** — **PUBLIC** init options (+`SdkEnvironment`).
- **`models/PurchaseItem.kt`**, **`PurchaseItemType.kt`**, **`PurchaseResult.kt`**, **`PurchaseStatus.kt`**,
  **`UserEntitlement.kt`** — **PUBLIC** data the app sees.
- **`models/PurchaseSdkError.kt`** — **PUBLIC** sealed error type (NotInitialized, ItemNotFound,
  BillingUnavailable, PurchaseCancelled, PurchaseFailed, GooglePlayNotConfigured, VerificationRequired,
  Unknown).
- **`models/PurchaseException.kt`** — **PUBLIC** throwable wrapping a `PurchaseSdkError`.
- **`listener/PurchaseListener.kt`** — **PUBLIC** popup callbacks.
- **`callbacks/PurchasePopupCallback.kt`** — **PUBLIC** Java-friendly callback for the low-level dialog.
- **`model/PurchasePopupData.kt`** — **PUBLIC** data for the low-level popup (note singular `model`).

**Internal implementation**
- **`core/SdkRuntime.kt`** — *internal* — immutable container built on `init`; picks the provider by
  mode; holds managers + scope.
- **`api/ApiClient.kt`** — *internal* — **real HTTP client** (`HttpURLConnection` + Gson) to
  `/api/v1/sdk/**`: `X-SDK-API-Key`, envelope parsing, error-code → `PurchaseSdkError` mapping, and the
  `start`→`confirm` bridge. Base URL from `PurchaseSdkConfig.baseUrl`. *(TODO: swap for Retrofit/OkHttp;
  add a real `/purchases/cancel`.)*
- **`api/Mappers.kt`** — *internal* — DTO↔model conversions (lenient enum parsing).
- **`api/dto/PurchaseItemDto.kt`/`PurchaseDto.kt`/`UserEntitlementDto.kt`/`AnalyticsEventDto.kt`** —
  *internal* — wire DTOs (already match backend shapes).
- **`api/request/CreatePurchaseRequest.kt`/`AnalyticsEventRequest.kt`** — *internal* — request bodies
  (idempotency key lives here).
- **`billing/BillingProvider.kt`** — *internal* — strategy interface (`getItem/getItems/makePurchase/
  restorePurchases`).
- **`billing/MockBillingProvider.kt`** — *internal* — runs create→confirm against the backend via
  `ApiClient` and grants the entitlement locally for an instant UI update.
- **`billing/GooglePlayBillingProvider.kt`** — *internal* — **stub**; throws `GooglePlayNotConfigured`.
  *(Google TODO checklist.)*
- **`purchase/PurchaseManager.kt`** — *internal* — orchestrates over the chosen provider + analytics +
  error mapping.
- **`purchase/PurchaseValidator.kt`** — *internal* — precondition checks → `PurchaseException`.
- **`entitlement/EntitlementManager.kt`** — *internal* — network-first entitlement reads (cache
  fallback when offline) so revoke/expiry/restore are reflected + optimistic upsert after
  purchase.
- **`storage/LocalStorage.kt`** — *internal* — the only `SharedPreferences` user (Gson JSON: creds,
  config, item/entitlement caches).
- **`analytics/AnalyticsTracker.kt`** — *internal* — fire-and-forget `track*`, tagged with billing mode;
  posts to the backend via `ApiClient`.
- **`error/ErrorMapper.kt`** — *internal* — throwable → `PurchaseSdkError`. *(TODO: map Google response
  codes.)*
- **`util/Ids.kt`** — *internal* — id/idempotency-key helpers.
- **`ui/PurchasePopupView.kt`** — *internal* — the bottom-sheet popup `View`
  (bind/loading/error/self-theming).
- **`ui/PurchasePopupController.kt`** — *internal* — hosts the view in a `Dialog`, wires Confirm →
  `PurchaseManager`. Has the "Google's final UI can't be replaced" note.
- **`ui/PurchaseDialog.kt`** — **PUBLIC (low-level)** — show the popup with `PurchasePopupData` without
  `init`. Kept as an alternative entry point; the demo no longer uses it.

### A.4 Android SDK — resources (`iap-sdk/src/main/res/`)
- **`layout/iap_view_purchase_popup.xml`** — the popup layout (image + item + price + buttons).
- **`values/iap_colors.xml`** / **`values-night/iap_colors.xml`** — light/dark palette.
- **`values/iap_dimens.xml`**, **`values/iap_strings.xml`**, **`values/iap_styles.xml`** — metrics,
  strings, Material styles for the popup.
- **`drawable/iap_ic_account.xml`/`iap_ic_card.xml`/`iap_ic_chevron.xml`** — popup row icons.

### A.5 Demo app (`app/src/main/java/com/example/iapmanagement/`)
Full multi-screen Compose demo using **only** the public `PurchaseSdk` facade.
- **`IapDemoApplication.kt`** — `Application` that calls `PurchaseSdk.init(...)` once at startup
  (MOCK, user `demo-user-001`, backend base URL from `DemoConfig`).
- **`MainActivity.kt`** — hosts `DemoApp`; bridges Buy/Unlock taps to `PurchaseSdk.showPurchasePopup`
  (Activity stays out of the ViewModel).
- **`demo/DemoConfig.kt`** — backend base URL (`http://10.0.2.2:8080/api/v1/sdk`), API key
  `demo_api_key_123`, demo user/account ids, premium item id.
- **`demo/DemoUiState.kt`** — immutable UI snapshot + `RestoreState` machine.
- **`demo/DemoViewModel.kt`** — the single SDK caller (state holder; no AAC dependency).
- **`ui/DemoApp.kt`** — hand-rolled router (no Navigation dep) + top bar + message banner.
- **`ui/DemoFormat.kt`** — price/type/date display helpers.
- **`ui/screens/HomeScreen.kt`/`StoreScreen.kt`/`EntitlementsScreen.kt`/`RestoreScreen.kt`/
  `PremiumScreen.kt`** — the five screens (loading/success/error states throughout).
- **`ui/theme/Color.kt`/`Theme.kt`/`Type.kt`** — standard Compose Material3 theme scaffolding.
- **`res/xml/network_security_config.xml`** — permits cleartext HTTP to `10.0.2.2`/`localhost` only;
  the manifest registers `IapDemoApplication` and declares `INTERNET`.

### A.6 Portal — source (`portal-web/src/`)
- **`main.tsx`** — bootstraps React + QueryClient + Router + AuthProvider.
- **`App.tsx`** — the route table (public vs protected vs app-scoped under `Layout`).
- **`index.css`** — Tailwind entry + base styles.
- **`vite-env.d.ts`** — types `import.meta.env.VITE_API_BASE_URL`.
- **`types/index.ts`** — TS mirrors of backend DTOs/enums.
- **`lib/api.ts`** — Axios instance; JWT interceptor; 401→login; envelope unwrap → `PortalApiError`.
- **`lib/format.ts`** — `money` (÷100), `percent`, `dateTime`, `num`.
- **`auth/AuthContext.tsx`** — current user; login/register/logout; restores session via `/auth/me`.
- **`hooks/usePortal.ts`** — **all** TanStack Query hooks (queries + mutations with invalidation),
  including `useResetDemoData` (calls the maintenance reset endpoint, then invalidates every query).
- **`components/ui.tsx`** — design-system primitives (Button, Card, Input, Kpi, CopyButton, CodeBlock…).
- **`components/Layout.tsx`** — sidebar + top bar (app selector, user menu, logout); `<Outlet/>`.
- **`components/ProtectedRoute.tsx`** — redirects to `/login` if unauthenticated.
- **`components/Filters.tsx`** — date/item/billing-mode/groupBy filter bar.
- **`pages/LoginPage.tsx`/`RegisterPage.tsx`** — auth forms (login pre-fills demo creds).
- **`pages/AppsPage.tsx`** — app cards + "New app".
- **`pages/NewAppPage.tsx`** — create-app form (package-name validation).
- **`pages/DashboardPage.tsx`** — KPIs, recent purchases/events, setup checklist.
- **`pages/ApiKeysPage.tsx`** — list/create (raw shown once)/revoke/rotate + "not a strong secret" note.
- **`pages/ItemsPage.tsx`** — item table; copy ids; enable/disable.
- **`pages/NewItemPage.tsx`** — create item (auto snake_case id, auto `ent_`, decimal price → minor
  units, currency dropdown from `lib/constants.ts`).
- **`pages/ItemDetailPage.tsx`** — metadata + SDK snippet + per-item stats.
- **`pages/AnalyticsPage.tsx`** — 10 KPI cards + a **purchases-by-status** breakdown + funnel bars.
- **`pages/RevenuePage.tsx`** — Recharts (revenue/purchases over time, by product, by mode) + table.
- **`pages/PurchasesPage.tsx`** — purchases table with filters.
- **`pages/PurchaseDetailPage.tsx`** — purchase + entitlement + events; no raw token.
- **`pages/EntitlementsPage.tsx`** — list (newest-first by date) + admin-only grant/revoke + warning.
- **`pages/SdkSetupPage.tsx`** — copy-paste SDK snippets + MOCK/GOOGLE_PLAY explanation + GP checklist.
- **`pages/SettingsPage.tsx`** — account info + a **"Danger zone"** (dev/demo) to reset & regenerate or
  delete all demo data via `useResetDemoData`. *(TODO: profile edit, password reset.)*

### A.7 Portal — config (`portal-web/`)
- **`package.json`** — deps (React, Router, TanStack Query, Axios, Tailwind, Recharts) + scripts.
- **`tsconfig.json`** — strict TS, bundler resolution.
- **`vite.config.ts`** — React plugin; dev/preview on `:5173`.
- **`index.html`** — SPA shell (`#root`).
- **`tailwind.config.js`/`postcss.config.js`** — Tailwind setup + theme colors (`ink`, `surface`).
- **`.env.example`** — `VITE_API_BASE_URL`.
- **`Dockerfile`** — build with Node, serve with nginx (multi-stage).
- **`nginx.conf`** — SPA fallback to `index.html`.
- **`.gitignore`** — node_modules/dist.

### A.8 Root / infra / shared
- **`README.md`** — platform overview, monorepo map, setup, flows, endpoint reference.
- **`docker-compose.yml`** — Postgres + backend (docker profile) + portal (nginx); dev-default secrets.
  *(Healthcheck targets the real DB.)*
- **`.env.example`** — `DATABASE_*`, `JWT_SECRET`, `API_KEY_PEPPER`, `INTERNAL_ADMIN_TOKEN`,
  `PORTAL_FRONTEND_URL`, `VITE_API_BASE_URL`. *(TODO: real secrets.)*
- **`shared/types.ts`** — canonical cross-project enums + `ApiResponse`.
- **`shared/README.md`** — explains the shared contract and the three places to keep in sync.
- **`build.gradle.kts`/`settings.gradle.kts`/`gradle.properties`/`gradlew`/`gradlew.bat`/
  `local.properties`** — the **Android** Gradle project (covers `app` + `iap-sdk`). The backend/portal
  are intentionally **not** part of this Gradle build.
- **`backend/Dockerfile`** + **`backend/.dockerignore`** — multi-stage Maven build → `eclipse-temurin:
  17-jre` (multi-arch; the `-alpine` JRE was amd64-only and caused the earlier compose error).
- **`backend/README.md`** — backend-only API/setup reference.
- **`iap-sdk/README.md`** — SDK-only reference (billing modes, popup, error model).

> **Files intentionally not enumerated:** generated/output (`build/`, `target/`, `node_modules/`,
> `dist/`), the Gradle wrapper jar, default Android launcher icons/`mipmap` resources, and IDE files —
> none contain project logic.

---

*End of guide. If anything here disagrees with the code you're reading, trust the code and tell me —
the most likely drift points are the Google Play paths (still scaffolded) and any TODO-marked stub.
For the demo flow specifically, see [`DEMO_GUIDE.md`](DEMO_GUIDE.md).*
