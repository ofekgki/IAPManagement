# shared/

Cross-project type definitions and contracts shared between the backend, the portal web app, and
(conceptually) the Android SDK.

- [`types.ts`](./types.ts) — the canonical enums (`BillingMode`, `ItemType`, `PurchaseStatus`
  including `RESTORED`, `EntitlementStatus`, `PaymentMethod`, `UserRole`, `ApiKeyStatus`) and the
  `ApiResponse<T>` envelope.

> Notes: `PaymentMethod` (Apple Pay / Google Pay / PayPal / Credit Card) is the portal's revenue
> breakdown dimension. `UserRole` still exists on users but is **no longer enforced** — roles were
> flattened, so every portal user has full access.

These mirror:
- **Backend (Java):** `backend/src/main/java/com/example/purchasebackend/domain/enums/*` and the
  `common/ApiResponse` envelope.
- **Portal (TS):** `portal-web/src/types/index.ts`.

Keep all three in sync when changing an enum or the response shape.
