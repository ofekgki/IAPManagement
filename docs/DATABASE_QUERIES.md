# Database Queries Reference

Every database query in the backend goes through **Spring Data JPA / Hibernate**. There is **no
hand-written SQL and no `@Query` annotation anywhere** — all custom queries are *derived query
methods* (Spring generates the SQL from the method name). This document lists every repository
method, what it does, and the SQL Hibernate generates for it.

- **ORM:** Spring Data JPA (Hibernate 6)
- **Database:** PostgreSQL (run via Docker Compose). Column/table names below use the
  physical names Hibernate derives from the entity fields (camelCase → the same identifier, quoted).
- **Schema management:** `spring.jpa.hibernate.ddl-auto: update` (tables are created/updated from the
  entities; see each `@Entity`).
- **Enums** are stored as `varchar` (`@Enumerated(EnumType.STRING)`).

> SQL below is illustrative (Postgres-style). Actual generated SQL selects every mapped column and
> may quote identifiers; parameters are bound as `?`.

---

## Tables

| Table | Entity | Key columns | Indexes |
|-------|--------|-------------|---------|
| `developer_user` | `DeveloperUser` | id (PK), email (unique), passwordHash, displayName, role, createdAt, updatedAt | `uq_user_email` (email, unique) |
| `developer_app` | `DeveloperApp` | id (PK), ownerUserId, appName, packageName, description, billingModeDefault, isActive, createdAt, updatedAt | `idx_app_owner` (ownerUserId) |
| `api_key` | `ApiKey` | id (PK), developerAppId, name, keyPrefix, keyHash (unique), status, createdAt, revokedAt, lastUsedAt | `idx_apikey_hash` (keyHash, unique), `idx_apikey_app` (developerAppId) |
| `purchase_item` | `PurchaseItem` | id (PK), developerAppId, itemId, name, description, type, priceAmountMinor, currency, priceDisplay, googlePlayProductId, entitlementId, isActive, createdAt, updatedAt | (see entity) |
| `purchase` | `Purchase` | id (PK), developerAppId, userId, itemId, billingMode, **paymentMethod** (APPLE_PAY / GOOGLE_PLAY / PAYPAL / CREDIT_CARD — the revenue-breakdown dimension), status, provider, providerPurchaseToken, providerOrderId, idempotencyKey, failureCode, failureMessage, **priceAmountMinor, priceCurrency** (price snapshot at purchase time), createdAt, updatedAt, completedAt | `idx_purchase_app_user` (developerAppId, userId), `idx_purchase_app_status_completed` (developerAppId, status, completedAt), `idx_purchase_app_created` (developerAppId, createdAt) |
| `entitlement` | `Entitlement` | id (PK), developerAppId, userId, entitlementId, sourceItemId, purchaseId, status, startsAt, expiresAt, createdAt, updatedAt | `idx_ent_app_user` (developerAppId, userId), `idx_ent_app_user_ent` (developerAppId, userId, entitlementId), `idx_ent_app_status` (developerAppId, status) |
| `analytics_event` | `AnalyticsEvent` | id (PK), developerAppId, userId, eventName, billingMode, itemId, purchaseId, metadataJson, createdAt | `idx_evt_app_name_time` (developerAppId, eventName, createdAt), `idx_evt_app_time` (developerAppId, createdAt), `idx_evt_purchase` (purchaseId) |
| `idempotency_record` | `IdempotencyRecord` | id (PK), developerAppId, idempotencyKey, purchaseId, responseJson, createdAt | (unique on developerAppId + idempotencyKey) |

---

## Built-in `JpaRepository` methods (available on every repository)

These come from Spring Data and are used throughout the services:

| Method | SQL |
|--------|-----|
| `save(e)` | `INSERT` (new) or `UPDATE` (existing, by `@Id`). |
| `saveAll(list)` | Batched `INSERT`/`UPDATE`. |
| `findById(id)` | `SELECT * FROM <table> WHERE id = ?` |
| `findAll()` | `SELECT * FROM <table>` |
| `existsById(id)` | `SELECT COUNT(*) ... WHERE id = ?` |
| `count()` | `SELECT COUNT(*) FROM <table>` |
| `delete(e)` / `deleteById(id)` | `DELETE FROM <table> WHERE id = ?` |
| `deleteAll()` | `DELETE FROM <table>` (row-by-row) |
| `deleteAllInBatch()` | `DELETE FROM <table>` (single statement — used by the demo-reset flow) |

`findAll()` is used by `PortalUserService.list()` (all portal users). `deleteAllInBatch()` is used by
`SeedDataLoader` when wiping demo data.

---

## `DeveloperUserRepository`

| Method | Purpose | SQL |
|--------|---------|-----|
| `findByEmailIgnoreCase(email)` | Login / lookup by email. | `SELECT * FROM developer_user WHERE lower(email) = lower(?)` |
| `existsByEmailIgnoreCase(email)` | Reject duplicate emails on register / add-user. | `SELECT COUNT(*) FROM developer_user WHERE lower(email) = lower(?)` |

## `DeveloperAppRepository`

| Method | Purpose | SQL |
|--------|---------|-----|
| `findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId)` | List a user's apps, newest first. | `SELECT * FROM developer_app WHERE ownerUserId = ? ORDER BY createdAt DESC` |
| `findByIdAndOwnerUserId(id, ownerUserId)` | Ownership-checked app fetch. | `SELECT * FROM developer_app WHERE id = ? AND ownerUserId = ?` |

## `ApiKeyRepository`

| Method | Purpose | SQL |
|--------|---------|-----|
| `findByKeyHash(keyHash)` | Resolve the app on each SDK request (by `SHA-256(pepper+rawKey)`). | `SELECT * FROM api_key WHERE keyHash = ?` |
| `findByDeveloperAppIdOrderByCreatedAtDesc(developerAppId)` | List an app's keys (also used for the unique-name check). | `SELECT * FROM api_key WHERE developerAppId = ? ORDER BY createdAt DESC` |
| `findByIdAndDeveloperAppId(id, developerAppId)` | Ownership-checked key fetch (revoke/rotate). | `SELECT * FROM api_key WHERE id = ? AND developerAppId = ?` |

## `PurchaseItemRepository`

| Method | Purpose | SQL |
|--------|---------|-----|
| `findByDeveloperAppIdAndItemId(developerAppId, itemId)` | Fetch one catalog item. | `SELECT * FROM purchase_item WHERE developerAppId = ? AND itemId = ?` |
| `findByDeveloperAppIdAndIsActiveTrue(developerAppId)` | Active catalog for the SDK. | `SELECT * FROM purchase_item WHERE developerAppId = ? AND isActive = true` |
| `findByDeveloperAppId(developerAppId)` | Full catalog (portal, analytics joins). | `SELECT * FROM purchase_item WHERE developerAppId = ?` |
| `existsByDeveloperAppIdAndItemId(developerAppId, itemId)` | Reject duplicate itemIds on create. | `SELECT COUNT(*) FROM purchase_item WHERE developerAppId = ? AND itemId = ?` |
| `findFirstByDeveloperAppIdAndEntitlementId(developerAppId, entitlementId)` | Resolve item behind an entitlement. | `SELECT * FROM purchase_item WHERE developerAppId = ? AND entitlementId = ? LIMIT 1` |

## `PurchaseRepository`

| Method | Purpose | SQL |
|--------|---------|-----|
| `findByIdAndDeveloperAppId(id, developerAppId)` | Confirm/detail a single purchase. | `SELECT * FROM purchase WHERE id = ? AND developerAppId = ?` |
| `findByDeveloperAppIdAndUserIdAndStatus(developerAppId, userId, status)` | A user's purchases in a state (e.g. SUCCESS for restore). | `SELECT * FROM purchase WHERE developerAppId = ? AND userId = ? AND status = ?` |
| `findByDeveloperAppIdAndUserId(developerAppId, userId)` | All of a user's purchases. | `SELECT * FROM purchase WHERE developerAppId = ? AND userId = ?` |
| `findByDeveloperAppId(developerAppId)` | All purchases (rarely — most reads are windowed below). | `SELECT * FROM purchase WHERE developerAppId = ?` |
| `findByDeveloperAppIdAndStatus(developerAppId, status)` | Purchases in a state (no window). | `SELECT * FROM purchase WHERE developerAppId = ? AND status = ?` |
| `findByDeveloperAppIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(developerAppId, status, from, to)` | Analytics window: SUCCESS/RESTORED purchases completed in `[from, to)`. Served by `idx_purchase_app_status_completed`. | `SELECT * FROM purchase WHERE developerAppId = ? AND status = ? AND completedAt >= ? AND completedAt < ?` |
| `findByDeveloperAppIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(developerAppId, from, to)` | Portal listing / by-status window, newest first. Served by `idx_purchase_app_created`. | `SELECT * FROM purchase WHERE developerAppId = ? AND createdAt >= ? AND createdAt < ? ORDER BY createdAt DESC` |

## `EntitlementRepository`

| Method | Purpose | SQL |
|--------|---------|-----|
| `findByDeveloperAppIdAndUserId(developerAppId, userId)` | A user's entitlements (SDK `listEntitlements`, restore). | `SELECT * FROM entitlement WHERE developerAppId = ? AND userId = ?` |
| `findByDeveloperAppId(developerAppId)` | All entitlements (portal list/filter). | `SELECT * FROM entitlement WHERE developerAppId = ?` |
| `countByDeveloperAppIdAndStatus(developerAppId, status)` | Active-entitlement KPI. | `SELECT COUNT(*) FROM entitlement WHERE developerAppId = ? AND status = ?` |
| `findByDeveloperAppIdAndUserIdAndEntitlementId(developerAppId, userId, entitlementId)` | All records for one entitlement. | `SELECT * FROM entitlement WHERE developerAppId = ? AND userId = ? AND entitlementId = ?` |
| `findFirstByDeveloperAppIdAndUserIdAndEntitlementIdOrderByCreatedAtDesc(developerAppId, userId, entitlementId)` | The **latest** record for an entitlement (grant/check/revoke). | `SELECT * FROM entitlement WHERE developerAppId = ? AND userId = ? AND entitlementId = ? ORDER BY createdAt DESC LIMIT 1` |

## `AnalyticsEventRepository`

| Method | Purpose | SQL |
|--------|---------|-----|
| `countByDeveloperAppIdAndEventNameAndCreatedAtBetween(developerAppId, eventName, from, to)` | Funnel/overview counts per event in a window. | `SELECT COUNT(*) FROM analytics_event WHERE developerAppId = ? AND eventName = ? AND createdAt BETWEEN ? AND ?` |
| `findByDeveloperAppIdAndCreatedAtBetween(developerAppId, from, to)` | All events in a window (analytics grouping). | `SELECT * FROM analytics_event WHERE developerAppId = ? AND createdAt BETWEEN ? AND ?` |
| `findTop200ByDeveloperAppIdAndCreatedAtBetweenOrderByCreatedAtDesc(developerAppId, from, to)` | Recent events feed (capped at 200). | `SELECT * FROM analytics_event WHERE developerAppId = ? AND createdAt BETWEEN ? AND ? ORDER BY createdAt DESC LIMIT 200` |
| `findByPurchaseId(purchaseId)` | Events linked to one purchase (purchase detail). | `SELECT * FROM analytics_event WHERE purchaseId = ?` |

## `IdempotencyRecordRepository`

| Method | Purpose | SQL |
|--------|---------|-----|
| `findByDeveloperAppIdAndIdempotencyKey(developerAppId, idempotencyKey)` | Replay a previously processed confirm request. | `SELECT * FROM idempotency_record WHERE developerAppId = ? AND idempotencyKey = ?` |

---

## Notes on a few query-heavy flows

- **Revenue** (`PortalAnalyticsService`): SUCCESS purchases in the requested window are read with the
  windowed derived query (served by `idx_purchase_app_status_completed`), and each purchase's
  **price snapshot** (`purchase.priceAmountMinor`, captured when the purchase started) is summed —
  so editing an item's list price later never rewrites historical revenue. Rows without a snapshot
  (legacy) fall back to the item's current price. `RESTORED` purchases are **refunds**: the same
  windowed query fetches them and their snapshot price is *subtracted* from every revenue figure
  (total, by mode, by time bucket, by product).
- **Event counters** (`overview`/`funnel`): when no item/billing-mode filter is set (the common
  case), each event name is counted with `countByDeveloperAppIdAndEventNameAndCreatedAtBetween` —
  an index-only `COUNT` on `idx_evt_app_name_time`, no rows materialized. With a filter, the
  window's events are fetched once (`idx_evt_app_time`) and counted in memory.
- **Purchases listing** (`GET /portal/apps/{appId}/purchases`): **paginated** (`page`, `size` ≤ 200,
  default 50). The date window is pushed to the DB (`idx_purchase_app_created`, ordered newest
  first); the optional status/item/mode/user filters are applied in memory and one page of DTOs is
  returned — the client never downloads the full history.
- **Restore = return/refund** (`PurchaseService.restorePurchases`): reads the user's SUCCESS
  purchases (`findByDeveloperAppIdAndUserIdAndStatus`, deduped by item — optionally narrowed to a
  single `itemId` for per-item return) and their current access
  (`EntitlementRepository.findByDeveloperAppIdAndUserId`). Every currently-owned (ACTIVE) item gets a
  `RESTORED` purchase row carrying the *original* price snapshot, and its entitlement is **revoked**
  (ownership released) — idempotent because a returned item is no longer ACTIVE.
- **Purchase detail timeline**: `findByPurchaseId` on `analytics_event` is served by
  `idx_evt_purchase` (added because this was previously a full-table scan on the largest table).
- **Revenue by payment method** (`PortalAnalyticsService.revenue`): purchases are grouped by
  `purchase.paymentMethod` (replacing the old MOCK/GOOGLE_PLAY "billing mode" breakdown). The optional
  `paymentMethod` filter on the analytics/purchases endpoints filters purchase rows in memory; it does
  not touch event-based metrics (analytics events carry no payment method).
- **Typed-id search is substring, case-insensitive:** the portal purchases and entitlements listings
  match `userId` / `itemId` / `entitlementId` with a lower-cased `contains` (not exact `equals`), so a
  partial or differently-cased id typed into the search box still matches.
- **Revenue-over-time is zero-filled:** `bucketByTime` pre-seeds every day/week/month in the window
  with 0, so the chart shows an actual value on days with sales and drops to 0 on days without
  (instead of interpolating a straight line between sparse points).
- **Entitlement "latest wins":** grant/check/revoke use
  `findFirstBy…OrderByCreatedAtDesc` because a user can accumulate multiple records for the same
  `entitlementId` over time; the most recent one is authoritative.
- **No `@Query`, no native SQL, no stored procedures.** If you add one, document it here.

## Index design summary

| Index | Serves |
|-------|--------|
| `idx_purchase_app_user` | SDK restore + user-purchase reads |
| `idx_purchase_app_status_completed` | analytics revenue windows (SUCCESS / RESTORED in `[from, to)`) |
| `idx_purchase_app_created` | portal purchases listing + purchases-by-status windows |
| `idx_evt_app_name_time` | index-only event `COUNT`s (overview / funnel) |
| `idx_evt_app_time` | events-in-window fetch (filtered analytics, recent-events feed) |
| `idx_evt_purchase` | purchase-detail event timeline |
| `idx_ent_app_user` / `idx_ent_app_user_ent` | entitlement reads / latest-wins lookups |
| `idx_ent_app_status` | active-entitlements KPI count |
| `idx_apikey_hash` (unique) | per-request SDK API-key resolution |
| `uq_idem_app_key` (unique) | idempotent confirm replay |

> Migration note: schema is managed by `ddl-auto: update` — new columns/indexes are added on boot.
> A pre-existing Postgres volume may still contain the old `idx_purchase_status` index; it is
> harmless and can be dropped manually.
