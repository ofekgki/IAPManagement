# Purchase SDK Backend

Backend server for the Android In-App Purchase SDK. It validates SDK clients, manages items, runs the
purchase flow (start → confirm → entitlement), supports restore and entitlement checks, and collects
analytics — for two billing modes: **MOCK** (fully working) and **GOOGLE_PLAY** (scaffolded,
fails safely until real verification is configured).

> Scope: **server side only.** No developer portal UI, no Android code, no demo app, no Google Play
> Billing UI, no admin panel/dashboard, and no real payment processing.

```
The backend supports MOCK mode for educational/demo purchases and GOOGLE_PLAY mode for future real
billing integration.

MOCK mode does not prove real payment and should not be used in production.

GOOGLE_PLAY mode must verify purchases server-side using the Google Play Developer API before
granting entitlements.

The SDK may show a custom pre-purchase popup, but the final real purchase confirmation UI is always
controlled by Google Play Billing.
```

---

## 1. Server overview

- **Stack:** Java 17, Spring Boot 3.3 (Web, Data JPA, Validation), H2 (in-memory for dev).
- **Base path:** `/api/v1`. SDK endpoints under `/api/v1/sdk`, internal/admin under `/api/v1/internal`.
- **Architecture:** thin controllers → services (business logic) → repositories (JPA). DTOs (records)
  are the only thing exposed over HTTP; entities never leak out.
- **Consistent envelope** for every response:
  ```json
  { "success": true, "data": { }, "error": null, "requestId": "req_abc123" }
  ```
  Errors:
  ```json
  { "success": false, "data": null,
    "error": { "code": "ITEM_NOT_FOUND", "message": "The requested item was not found.", "details": {} },
    "requestId": "req_abc123" }
  ```

### Key classes

| Layer | Classes |
|---|---|
| Controllers | `HealthController`, `SdkInitController`, `ItemController`, `PurchaseController`, `EntitlementController`, `AnalyticsController`, `InternalItemController`, `InternalPurchaseController`, `InternalEntitlementController`, `InternalAnalyticsController` |
| Services | `ApiKeyService`, `DeveloperAppService`, `ItemService`, `PurchaseService`, `EntitlementService`, `AnalyticsService`, `IdempotencyService`, `GooglePlayVerificationService`, `MockBillingService`, `ErrorMapper` |
| Repositories | `DeveloperAppRepository`, `PurchaseItemRepository`, `PurchaseRepository`, `EntitlementRepository`, `AnalyticsEventRepository`, `IdempotencyRecordRepository` |
| Filters | `RequestIdFilter`, `SdkApiKeyFilter`, `InternalAdminTokenFilter` |

> Error normalization (the role of an "ErrorMapper") is realized by `ApiException` + `ErrorCode` +
> `ErrorMapper` + `GlobalExceptionHandler`, so every failure becomes the same stable envelope.

---

## 2. Billing modes

| | `MOCK` | `GOOGLE_PLAY` |
|---|---|---|
| Purpose | Demo / tests / education | Future real billing |
| Works today | yes, end-to-end | scaffold only |
| Google Play Console | not needed | required (later) |
| Purchase verification | simulated success | **must** verify token server-side (TODO) |
| On unconfigured call | n/a | fails with `GOOGLE_PLAY_NOT_CONFIGURED` |

**MOCK never proves real payment.** **GOOGLE_PLAY never fakes success** — it returns a clear error
until `GooglePlayVerificationService` is implemented.

---

## 3. API authentication

| Header | Used by | Purpose |
|---|---|---|
| `X-SDK-API-Key` | all `/sdk/**` | Identifies the developer app (validated as a SHA-256 hash; raw key never stored). |
| `X-SDK-Version` | `/sdk/**` | SDK version, for logging/analytics. |
| `X-Request-Id` | all | Correlation id; echoed back in the response and the envelope. Generated if absent. |
| `Idempotency-Key` | `/sdk/purchases/confirm` | Makes confirmation safe to retry (unique per attempt). |
| `Authorization: Bearer <JWT>` | all `/portal/**` (except register/login) | Developer-portal auth. The backend also serves the **portal API** (`/api/v1/portal/**`) consumed by `portal-web`. |
| `X-Internal-Admin-Token` | all `/internal/**` | Shared admin token. **Fails closed** if unset. |

> The full portal endpoint list (apps, API keys, items, purchases, entitlements, analytics, users,
> maintenance) lives in [`../docs/API_ENDPOINTS.md`](../docs/API_ENDPOINTS.md). Roles were flattened —
> every authenticated portal user has full access.

> The SDK API key ships inside a client app, so it is **not a strong secret** — it identifies, it does
> not authorize sensitive actions. Future hardening (all marked `TODO` in code): package-name
> validation, signing-certificate fingerprint, Play Integrity API, rate limiting, and **never trust
> client-side entitlement claims**.

---

## 4. SDK endpoints

| Method | Path | Description |
|---|---|---|
| GET  | `/api/v1/health` | Health check (no auth). |
| POST | `/api/v1/sdk/init` | Validate app, return feature flags, record `sdk_initialized`. |
| GET  | `/api/v1/sdk/items` | List active items. |
| GET  | `/api/v1/sdk/items/{itemId}` | Get one item (`ITEM_NOT_FOUND` if missing/inactive). |
| POST | `/api/v1/sdk/purchases/start` | Create a `CREATED` purchase (snapshots price + `paymentMethod`). |
| POST | `/api/v1/sdk/purchases/confirm` | Complete a purchase (idempotent). |
| POST | `/api/v1/sdk/purchases/restore` | **Return/refund**: releases owned items (revokes entitlement, drops revenue). Optional `itemId` = per-item return. |
| GET  | `/api/v1/sdk/entitlements/check` | `hasEntitlement` by `entitlementId` or `itemId`. |
| GET  | `/api/v1/sdk/entitlements` | `listEntitlements` for a user. |
| POST | `/api/v1/sdk/analytics/events` | Store a custom analytics event. |

### Internal/admin endpoints (require `X-Internal-Admin-Token`)

| Method | Path | Description |
|---|---|---|
| POST  | `/api/v1/internal/items` | Create an item. |
| PATCH | `/api/v1/internal/items/{itemId}?developerAppId=...` | Update mutable item fields. |
| GET   | `/api/v1/internal/purchases?developerAppId=&userId=&status=` | List purchases (filters). |
| POST  | `/api/v1/internal/entitlements/grant` | Manually grant an entitlement. |
| POST  | `/api/v1/internal/entitlements/revoke` | Revoke an entitlement (kept as `REVOKED`). |
| GET   | `/api/v1/internal/analytics/summary?developerAppId=&from=&to=` | Basic event counts. |

---

## 5. Mock purchase flow (end-to-end)

```
SDK                         Backend
 │  POST /sdk/purchases/start ───────────►  create Purchase(status=CREATED), track purchase_started
 │  ◄─ { purchaseId, status: CREATED }
 │
 │  POST /sdk/purchases/confirm ─────────►  mark SUCCESS, grant entitlement, track purchase_success
 │     (Idempotency-Key: k1)               store idempotency(k1 → response)
 │  ◄─ { status: SUCCESS, entitlementGranted: true, entitlement {...} }
 │
 │  (retry with same Idempotency-Key) ───►  returns the original response (no double processing)
```

Entitlement rules (in `EntitlementService`):
- **NON_CONSUMABLE** → active forever; existing active entitlement is reused (no duplicates).
- **SUBSCRIPTION** → active with expiry (MOCK: `app.mock-subscription-days`, default 30; GOOGLE_PLAY:
  from verified Google data — TODO).
- **CONSUMABLE** → no long-lived entitlement unless the item declares an `entitlementId`
  (TODO: real consumable balances, e.g. coins).

---

## 6. Google Play — future integration (all TODO)

Real billing is intentionally **not** implemented. `GooglePlayVerificationService.verifyPurchase(...)`
returns `notConfigured()`, so confirm/restore in `GOOGLE_PLAY` mode return `GOOGLE_PLAY_NOT_CONFIGURED`.
The code marks every required step:

- Add the Google Play Developer API client + service-account credentials.
- Verify the purchase **token**, **package name**, **product id**, and **purchase state**.
- Ensure the token was not already used (per user); handle refunds/cancellations/revocations.
- Acknowledge non-consumables/subscriptions; consume consumables (if supported).
- Read subscription expiry/auto-renewal from verified Google data.
- Connect Real-time Developer Notifications via Google Pub/Sub.
- **Only grant entitlements after verification succeeds.** Log only a shortened/hashed token.

The client SDK launches `BillingClient.launchBillingFlow(...)`; the **final payment UI is Google's**
and cannot be replaced by this backend or the SDK.

---

## 7. Example requests and responses

Init:
```bash
curl -s -X POST localhost:8080/api/v1/sdk/init \
  -H "X-SDK-API-Key: demo_api_key_123" -H "Content-Type: application/json" \
  -d '{"sdkVersion":"1.0.0","packageName":"com.example.demo","userId":"user_123","billingMode":"MOCK"}'
```
```json
{ "success": true,
  "data": { "appId": "app_demo", "billingMode": "MOCK", "serverTime": "2026-06-29T12:00:00Z",
            "features": { "mockBillingEnabled": true, "googlePlayBillingEnabled": false, "analyticsEnabled": true } },
  "error": null, "requestId": "req_..." }
```

Start + confirm (MOCK):
```bash
curl -s -X POST localhost:8080/api/v1/sdk/purchases/start \
  -H "X-SDK-API-Key: demo_api_key_123" -H "Content-Type: application/json" \
  -d '{"userId":"user_123","itemId":"remove_ads","billingMode":"MOCK"}'

curl -s -X POST localhost:8080/api/v1/sdk/purchases/confirm \
  -H "X-SDK-API-Key: demo_api_key_123" -H "Idempotency-Key: key-001" -H "Content-Type: application/json" \
  -d '{"purchaseId":"pur_xxx","userId":"user_123","itemId":"remove_ads","billingMode":"MOCK"}'
```
```json
{ "success": true,
  "data": { "purchaseId": "pur_xxx", "status": "SUCCESS", "entitlementGranted": true,
            "entitlement": { "entitlementId": "ent_remove_ads", "status": "ACTIVE", "expiresAt": null } },
  "error": null, "requestId": "req_..." }
```

Check entitlement:
```bash
curl -s "localhost:8080/api/v1/sdk/entitlements/check?userId=user_123&itemId=remove_ads" \
  -H "X-SDK-API-Key: demo_api_key_123"
```

GOOGLE_PLAY confirm (fails safely):
```json
{ "success": false, "data": null,
  "error": { "code": "GOOGLE_PLAY_NOT_CONFIGURED",
             "message": "Google Play purchase verification is not configured...", "details": null },
  "requestId": "req_..." }
```

---

## 8. Error codes

`NOT_INITIALIZED`, `INVALID_API_KEY`, `APP_DISABLED`, `INVALID_REQUEST`, `ITEM_NOT_FOUND`,
`PURCHASE_NOT_FOUND`, `PURCHASE_ALREADY_PROCESSED`, `PURCHASE_CANCELLED`, `PURCHASE_FAILED`,
`BILLING_MODE_NOT_SUPPORTED`, `GOOGLE_PLAY_NOT_CONFIGURED`, `VERIFICATION_REQUIRED`,
`PURCHASE_VERIFICATION_FAILED`, `ENTITLEMENT_NOT_FOUND`, `INTERNAL_ERROR` (plus `ADMIN_TOKEN_INVALID`
for internal endpoints). Each maps to a fixed HTTP status in `ErrorCode`.

---

## 9. Local development setup

Requirements: JDK 17+ and Maven (or an IDE with Spring Boot support) plus internet access for
dependencies.

```bash
cd backend
mvn spring-boot:run          # starts on http://localhost:8080 with the dev profile
```

The `dev` profile (active by default in `application.yml`) enables the H2 console at `/h2-console`
and demo seed data. Configure the internal admin token via `INTERNAL_ADMIN_TOKEN` (dev default:
`dev_internal_admin_token`).

> This is a standalone Spring Boot project and is intentionally **not** part of the Android Gradle
> build (different toolchains). Build it with Maven/your IDE, not with the project's `gradlew`.

---

## 10. Seed data (dev only)

On startup with `app.seed.enabled=true` (dev + docker profiles), `SeedDataLoader` creates:

- **Portal user:** `demo@example.com` / `password123` (owner of the demo app).
- **Developer app:** `app_demo` ("Demo Game"), API key `demo_api_key_123`, default mode `MOCK`.
- **Catalog:** a **~21-item** catalog — lifetime unlocks (e.g. `remove_ads`, `premium_lifetime`),
  monthly/yearly subscriptions (e.g. `pro_monthly`), and consumable coin packs (`coins_100/500/…`).
  All prices are **USD**, stored in **minor units**.
- **History** (`app.seed.sample-data=true`): a realistic, deterministic stream of purchases,
  entitlements, and analytics from `app.seed.start-date` (default `2026-01-01`) to
  `app.seed.end-date` (blank ⇒ today). Each purchase gets a **price snapshot** and a random
  **payment method** (Apple Pay / Google Pay / PayPal / Credit Card) for the portal's breakdowns.

Seeding is idempotent, auto-reseeds a stale/smaller catalog, and **never runs in production**.
See [`../docs/DEMO_GUIDE.md`](../docs/DEMO_GUIDE.md).
```
