# shared/

Cross-project type definitions and contracts shared between the backend, the portal web app, and
(conceptually) the Android SDK.

- [`types.ts`](./types.ts) — the canonical enums (`BillingMode`, `ItemType`, `PurchaseStatus`,
  `EntitlementStatus`, `UserRole`, `ApiKeyStatus`) and the `ApiResponse<T>` envelope.

These mirror:
- **Backend (Java):** `backend/src/main/java/com/example/purchasebackend/domain/enums/*` and the
  `common/ApiResponse` envelope.
- **Portal (TS):** `portal-web/src/types/index.ts`.

Keep all three in sync when changing an enum or the response shape.
