# API Endpoints Reference

Every HTTP endpoint the backend exposes, grouped by authentication surface. All responses use the
envelope `{ success, data, error, requestId }` (see `ApiResponse`); the tables below describe what
goes in `data`. Money is always in **minor units** (cents). Enums are serialized as their names.

Base URL (dev): `http://localhost:8080`.

## Authentication surfaces

| Surface | Path prefix | Auth header | Notes |
|---------|-------------|-------------|-------|
| **Public** | `/api/v1/health`, `/api/v1/portal/auth/register`, `/api/v1/portal/auth/login` | none | Open. |
| **SDK** | `/api/v1/sdk/**` | `X-SDK-API-Key: <raw key>` | Resolves the developer app by `SHA-256(pepper+key)`. Rejected with `INVALID_API_KEY` if missing/invalid, `APP_DISABLED` if the app is off. |
| **Portal** | `/api/v1/portal/**` (except register/login) | `Authorization: Bearer <JWT>` | JWT issued by login/register. Roles were removed — every authenticated user has full access. |
| **Internal** | `/api/v1/internal/**` | `X-Internal-Admin-Token: <token>` | Server-to-server/admin. Dev token `dev_internal_admin_token`; unset in docker (fails closed). |

---

## Public

| Method | Path | Purpose | Response `data` |
|--------|------|---------|-----------------|
| GET | `/api/v1/health` | Liveness probe. | `{ status, time }` |

---

## SDK API (`X-SDK-API-Key`)

### Init & catalog
| Method | Path | Purpose | Request | Response `data` |
|--------|------|---------|---------|-----------------|
| POST | `/api/v1/sdk/init` | Handshake; returns app config. | `{ userId?, billingMode? }` | `InitResponse` (app id, default billing mode, …) |
| GET | `/api/v1/sdk/items` | Active catalog. Optional `?billingMode=`. | — | `{ items: ItemDto[] }` |
| GET | `/api/v1/sdk/items/{itemId}` | One catalog item. | — | `ItemDto` |

### Purchases
| Method | Path | Purpose | Request | Response `data` |
|--------|------|---------|---------|-----------------|
| POST | `/api/v1/sdk/purchases/start` | Create a `CREATED` purchase + snapshot price/payment method. | `{ userId, itemId, billingMode?, paymentMethod? }` | `StartPurchaseResponse` (`purchaseId`, `status`, …) |
| POST | `/api/v1/sdk/purchases/confirm` | Complete a started purchase (grants entitlement). Idempotent via optional `Idempotency-Key` header. | `{ purchaseId, userId, itemId, billingMode?, googlePlay? }` | `ConfirmPurchaseResponse` (final `status`, entitlement) |
| POST | `/api/v1/sdk/purchases/restore` | **Return/refund**: releases owned items and drops revenue. Optional `itemId` = per-item return. | `{ userId, billingMode?, itemId? }` | `RestoreResponse` (`restoredPurchases[]`, active `entitlements[]`) |

> `paymentMethod` is one of `APPLE_PAY` / `GOOGLE_PLAY` / `PAYPAL` / `CREDIT_CARD` (defaults to
> `CREDIT_CARD`). `Idempotency-Key` must be **unique per purchase attempt** — reusing a key replays
> the first response (see the idempotency note at the bottom).

### Entitlements & analytics
| Method | Path | Purpose | Request | Response `data` |
|--------|------|---------|---------|-----------------|
| GET | `/api/v1/sdk/entitlements/check?userId=&itemId=` | Does the user have active access to an item? | — | `CheckEntitlementResponse` (`{ active }`) |
| GET | `/api/v1/sdk/entitlements?userId=` | A user's entitlements. | — | `{ entitlements: [{ entitlementId, sourceItemId, status, expiresAt }] }` |
| POST | `/api/v1/sdk/analytics/events` | Record a client analytics event. | `TrackEventRequest` (`{ userId?, eventName, itemId?, billingMode?, purchaseId?, metadata? }`) | `TrackEventResponse` |

---

## Portal API (`Authorization: Bearer <JWT>`)

### Auth & profile
| Method | Path | Purpose | Request | Response `data` |
|--------|------|---------|---------|-----------------|
| POST | `/api/v1/portal/auth/register` | Create the first account (becomes OWNER). *(public)* | `{ email, password(≥8), displayName? }` | `AuthResponse` (`{ token, user }`) |
| POST | `/api/v1/portal/auth/login` | Log in. *(public)* | `{ email, password }` | `AuthResponse` |
| POST | `/api/v1/portal/auth/logout` | Stateless (client discards token). | — | `{ loggedOut }` |
| GET | `/api/v1/portal/auth/me` | Current user. | — | `UserDto` |
| PATCH | `/api/v1/portal/auth/me` | Edit own email / display name / password. Re-issues the JWT. | `{ email?, displayName?, password?(≥8) }` | `AuthResponse` |

### Users
| Method | Path | Purpose | Request | Response `data` |
|--------|------|---------|---------|-----------------|
| GET | `/api/v1/portal/users` | List all portal users. | — | `UserDto[]` |
| POST | `/api/v1/portal/users` | Add a user (defaults to OWNER). | `{ email, password(≥8), displayName? }` | `UserDto` |
| DELETE | `/api/v1/portal/users/{userId}` | Delete a user (not yourself). | — | `{ deleted }` |

### Apps
| Method | Path | Purpose | Request | Response `data` |
|--------|------|---------|---------|-----------------|
| GET | `/api/v1/portal/apps` | Your apps. | — | `App[]` |
| POST | `/api/v1/portal/apps` | Create an app. | `{ appName, packageName, description?, defaultBillingMode? }` | `App` |
| GET | `/api/v1/portal/apps/{appId}` | One app. | — | `App` |
| PATCH | `/api/v1/portal/apps/{appId}` | Update (e.g. `{ isActive }` to enable/disable). | partial `App` | `App` |
| DELETE | `/api/v1/portal/apps/{appId}` | Delete an app. | — | `{ deleted }` |

### API keys (`/api/v1/portal/apps/{appId}/api-keys`)
| Method | Path | Purpose | Response `data` |
|--------|------|---------|-----------------|
| GET | `` | List keys. | `ApiKeyDto[]` |
| POST | `` | Create key (raw key shown **once**). Rejects a duplicate active name. | `CreatedApiKeyResponse` |
| POST | `/{keyId}/revoke` | Revoke a key. | `ApiKeyDto` |
| POST | `/{keyId}/rotate` | Revoke + issue a replacement. | `CreatedApiKeyResponse` |

### Items (`/api/v1/portal/apps/{appId}/items`)
| Method | Path | Purpose | Response `data` |
|--------|------|---------|-----------------|
| GET | `` | List items. | `PortalItemDto[]` |
| POST | `` | Create an item (unique `itemId` per app). | `PortalItemDto` |
| GET | `/{itemId}` | One item. | `PortalItemDto` |
| PATCH | `/{itemId}` | Update (name, description, **price**, currency, entitlement, …). | `PortalItemDto` |
| POST | `/{itemId}/enable` · `/{itemId}/disable` | Toggle active. | `PortalItemDto` |

### Purchases (`/api/v1/portal/apps/{appId}/purchases`)
| Method | Path | Purpose | Response `data` |
|--------|------|---------|-----------------|
| GET | `` | Paginated list. Query: `from,to,status,itemId,paymentMethod,userId,page,size(≤200)`. `userId`/`itemId` match case-insensitive **substring**. | `PagedPurchasesDto` (`items[]`, `page`, `size`, `totalItems`, `totalPages`) |
| GET | `/{purchaseId}` | Detail + linked entitlement + event log. | `PurchaseDetailDto` |

### Analytics (`/api/v1/portal/apps/{appId}/analytics`)
Common query params: `from`, `to` (default 1 Jan 2026 → now on the Revenue page), `itemId`,
`paymentMethod`, and `groupBy=day|week|month` where noted.

| Method | Path | Purpose | Response `data` |
|--------|------|---------|-----------------|
| GET | `/overview` | KPI summary (revenue net of restores, conversions, active entitlements). | `OverviewResponse` |
| GET | `/funnel` | Popup → success funnel (from events; ignores `paymentMethod`). | `FunnelResponse` |
| GET | `/revenue` | Net revenue + **by payment method** + zero-filled time series. | `RevenueSummaryResponse` (`byPaymentMethod[]`, `overTime[]`, `restoredValueMinor`, …) |
| GET | `/revenue/by-product` | Net units + revenue per product. | `RevenueByProductRow[]` |
| GET | `/revenue/by-time` | Zero-filled revenue buckets. | `RevenueByTimePoint[]` |
| GET | `/purchases-by-status` | Purchase counts per status in the window. | `PurchaseStatusRow[]` |
| GET | `/events` | Recent events (capped 200). | `EventDto[]` |

### Maintenance
| Method | Path | Purpose | Response `data` |
|--------|------|---------|-----------------|
| POST | `/api/v1/portal/maintenance/reset-demo-data?reseed=true|false` | Wipe (and optionally regenerate) the demo dataset. Dev/docker only. | `{ reseeded, items, purchases, entitlements, events }` |

---

## Internal API (`X-Internal-Admin-Token`)

| Method | Path | Purpose | Response `data` |
|--------|------|---------|-----------------|
| POST | `/api/v1/internal/items` | Create an item (admin). | `AdminItemDto` |
| PATCH | `/api/v1/internal/items/{itemId}` | Update an item (admin). | `AdminItemDto` |
| GET | `/api/v1/internal/purchases` | List purchases (admin, cross-cutting filters). | `AdminPurchaseDto[]` |
| GET | `/api/v1/internal/analytics/summary` | Aggregate analytics summary. | `AnalyticsSummaryResponse` |
| POST | `/api/v1/internal/entitlements/grant` | Grant an entitlement. | `EntitlementActionResponse` |
| POST | `/api/v1/internal/entitlements/revoke` | Revoke an entitlement. | `EntitlementActionResponse` |

---

## Cross-cutting notes

- **Envelope & errors:** failures return `success:false` with `error:{ code, message }` and an HTTP
  status from `ErrorCode` (e.g. `INVALID_API_KEY` 401, `APP_DISABLED` 403, `ITEM_NOT_FOUND` 404,
  `EMAIL_ALREADY_EXISTS` 409, `GOOGLE_PLAY_NOT_CONFIGURED` 400).
- **Idempotency (confirm):** `POST /purchases/confirm` dedupes on `(appId, Idempotency-Key)`. The key
  **must be unique per purchase attempt** — the SDK derives it per `createPurchase` call. A stable key
  reused across attempts (e.g. buying an item again after a restore) makes the backend replay the
  first response, leaving the new purchase `CREATED` and un-granted.
- **Restore = return/refund:** it revokes ownership and subtracts the item's snapshot price from
  revenue (see `restore-semantics`). Not a re-download of entitlements.
- **Security:** raw API keys are only shown once at creation; only hashes are stored. Google Play
  verification fails safe (`REQUIRES_VERIFICATION`) until configured. Provider purchase tokens are
  never returned by the portal.

See also [DATABASE_QUERIES.md](DATABASE_QUERIES.md) for the persistence layer behind these endpoints.
