# IAP Purchase SDK (`:iap-sdk`)

A small, self-contained **client-side** In-App Purchase SDK for Android, written in Kotlin. It gives
an app a clean public API for fetching items, showing a purchase popup, making purchases, and
checking entitlements — with all networking, storage, analytics, and UI hidden behind `internal`
implementation classes.

> **Learning project / scope.** This module is the **client SDK library layer only**. There is *no*
> real backend yet: the network layer (`ApiClient`) is a self-contained mock that serves a sample
> catalog and records purchases in memory. Every seam where a real server call belongs is marked
> with a `TODO(backend)` comment.

---

## Table of contents

1. [What this SDK includes](#what-this-sdk-includes)
2. [What it does NOT include](#what-it-does-not-include)
3. [Installation](#installation)
4. [Initialization](#initialization)
5. [Billing modes](#billing-modes)
6. [Public client API](#public-client-api)
7. [Public models](#public-models)
8. [Internal architecture](#internal-architecture)
9. [Public vs internal — at a glance](#public-vs-internal--at-a-glance)
10. [Usage examples](#usage-examples)
11. [Error handling](#error-handling)
12. [Notes for future backend integration](#notes-for-future-backend-integration)
13. [Notes for future Google Play Billing integration](#notes-for-future-google-play-billing-integration)
14. [Current limitations](#current-limitations)

---

## What this SDK includes

- A public singleton entry point: **`PurchaseSdk`**.
- Two **billing modes** — **`BillingMode.MOCK`** (default; fully local simulation) and
  **`BillingMode.GOOGLE_PLAY`** (scaffolded real Play Billing) — selectable at `init`.
- Public **models** (`PurchaseItem`, `PurchaseResult`, `UserEntitlement`, `PurchaseSdkError`, …) and a
  **`PurchaseListener`** callback interface.
- A **custom purchase popup** (bottom-sheet UI), shown via `PurchaseSdk.showPurchasePopup`.
- **Internal managers**: purchase flow, entitlements, local cache, analytics, error mapping, and a
  mock API client — all `internal`, none reachable by the host app.
- **Coroutines-based** async API (`suspend` functions).
- This **README** documenting every public and internal piece.

## What it does NOT include

- ❌ Backend server, database, or developer portal / admin dashboard.
- ❌ A real payment provider or **Google Play Billing** integration.
- ❌ Server authentication, product-creation UI, or an analytics dashboard.
- ❌ A full demo app screen (a minimal demo lives in the sibling `:app` module).

The SDK is structured so each of these can be added later **behind the existing internal seams**
without changing the public API.

---

## Installation

This is a local Gradle module. The host app depends on it directly:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":iap-sdk"))
}
```

```kotlin
// settings.gradle.kts
include(":iap-sdk")
```

> Placeholder for the future published artifact:
> ```kotlin
> implementation("com.example.iapsdk:iap-sdk:<version>")
> ```

**Requirements:** `minSdk 24`, `compileSdk 37`, Material Components theme is **not** required in the
host app — the popup themes itself.

---

## Initialization

Call `init` once before anything else, e.g. in `Application.onCreate`:

```kotlin
// Simplest: mock billing (the default), no user id required.
PurchaseSdk.init(
    context = applicationContext,
    apiKey = "TODO_API_KEY",
    billingMode = BillingMode.MOCK,
)

// Real Google Play billing (scaffolded — see "Billing modes").
PurchaseSdk.init(
    context = applicationContext,
    apiKey = "TODO_API_KEY",
    billingMode = BillingMode.GOOGLE_PLAY,
    userId = "user-123",
    config = PurchaseSdkConfig(
        environment = SdkEnvironment.SANDBOX,
        enableAnalytics = true,
        enableLocalCache = true,
    ),
)
```

`billingMode` defaults to `BillingMode.MOCK`. Only `context.applicationContext` is retained, so
passing an `Activity` does not leak it. **Do not hardcode a real API key** — the `apiKey` above is a
placeholder; real validation against a backend is a `TODO`.

---

## Billing modes

The SDK runs in one of two modes, chosen at `init`. Internally each is a `BillingProvider`
implementation; `PurchaseManager` delegates to whichever was selected, so the public API is identical
in both modes.

### `BillingMode.MOCK`  *(default)*

Used for:
- Demo apps
- Tests
- Educational / project mode
- **No Google Play setup required**

Behavior: `makePurchase(...)` simulates a successful purchase, grants the entitlement locally (so
`hasEntitlement` / `listEntitlements` / `restorePurchases` work end-to-end), returns a fake
`PurchaseResult`, and never opens any Google UI. Sample items: `remove_ads`, `premium_lifetime`,
`coins_100`, `monthly_subscription_demo` (demo prices like `$1.99` / `$4.99` — **demo values only**).

### `BillingMode.GOOGLE_PLAY`

Used for:
- Real Google Play Billing integration
- Requires Google Play Console products
- Requires `BillingClient` setup
- Requires backend purchase verification before granting entitlements

Behavior: the structure exists (`GooglePlayBillingProvider`) but real billing is **not implemented
yet**. Until the provider's `TODO`s are completed, its calls fail gracefully with
`PurchaseSdkError.GooglePlayNotConfigured` — the SDK never pretends a real purchase succeeded.

> **The SDK popup is a *pre-purchase* UI only.** For real Google Play Billing purchases, the final
> payment confirmation screen is always controlled by Google Play and **cannot be customized or
> replaced** by this SDK. The SDK popup is shown *before* the Google flow.

---

## Public client API

All public functions live on the `PurchaseSdk` object. Data methods are `suspend` (call them from a
coroutine).

### `init(context, apiKey, billingMode = BillingMode.MOCK, userId = null, config = PurchaseSdkConfig())`
Initializes the SDK: validates the API key, persists key / user id / config to local storage, builds
the internal managers for the chosen `billingMode`, and emits an `sdk_initialized` analytics event.
Calling it again replaces the previous session (handy when switching users or modes). `userId` is
optional (MOCK mode); a real deployment should pass a stable id. Throws `PurchaseException` if
`apiKey` is blank.

### `isInitialized(): Boolean`
Returns whether `init` has run and has not been torn down by `logout`.

### `getItem(itemId): PurchaseItem` *(suspend)*
Returns one item by id. Checks the local cache first (when caching is enabled), then the API. Throws
`PurchaseException` (`ITEM_NOT_FOUND`) when the item doesn't exist.

### `showPurchasePopup(activity, itemId, listener)`
Loads the item and presents the SDK's purchase popup over `activity`. Outcomes are delivered to the
`PurchaseListener` on the main thread. If the item can't be loaded, `onPurchaseFailed` is called and
no popup is shown. This is the recommended, batteries-included flow.

### `makePurchase(itemId, paymentMethodId = null): PurchaseResult` *(suspend)*
Runs the purchase flow **without** a popup — for apps with their own UI. Returns a `PurchaseResult`
on success; throws `PurchaseException` on any failure.

### `restorePurchases(): List<PurchaseResult>` *(suspend)*
Restores the user's prior purchases and refreshes the local entitlement cache.

### `hasEntitlement(itemId): Boolean` *(suspend)*
Whether the current user has active access to `itemId`. Checks the cache first, then refreshes from
the API if needed.

### `listEntitlements(): List<UserEntitlement>` *(suspend)*
All active entitlements for the current user.

### `trackEvent(eventName, properties = emptyMap())`
Sends a custom analytics event through the SDK. No-op if not initialized or if analytics are disabled.

### `logout()`
Clears the user's local data (user id + entitlement cache), stops background work, and resets the SDK
to uninitialized. The cached item catalog is preserved. Safe to call when not initialized.

---

## Public models

| Model | Summary |
|---|---|
| **`BillingMode`** | `MOCK` / `GOOGLE_PLAY`. Selects the billing strategy at `init`. |
| **`PurchaseSdkConfig`** | Optional init config: `environment`, `baseUrl?`, `enableAnalytics`, `enableLogs`, `enableLocalCache`. All defaulted. |
| **`SdkEnvironment`** | `SANDBOX` / `PRODUCTION`. Chooses the (mock) base URL. |
| **`PurchaseItem`** | A purchasable product: `id`, `name`, `description`, `price`, `currency`, `type`. |
| **`PurchaseItemType`** | `LIFETIME` / `CONSUMABLE` / `SUBSCRIPTION`. |
| **`PurchaseResult`** | Outcome of a purchase: `purchaseId`, `itemId`, `userId`, `status`, `purchasedAt`, `message?`. |
| **`PurchaseStatus`** | `SUCCESS` / `PENDING` / `FAILED` / `CANCELLED`. |
| **`PurchaseSdkError`** | Sealed set of client-facing errors (each with a stable `code` + `message`). See [Error handling](#error-handling). |
| **`UserEntitlement`** | Access record: `itemId`, `userId`, `type`, `isActive`, `grantedAt`, `expiresAt?`. |
| **`PurchaseListener`** | Popup callback: `onPurchaseSuccess` / `onPurchaseCancelled` / `onPurchaseFailed(PurchaseSdkError)`. |
| **`PurchaseException`** | Thrown by suspend APIs; carries a `PurchaseSdkError` in `.error`. |

There is also a lower-level UI pair kept from the original popup component:
**`PurchasePopupData`** + **`PurchaseDialog`** — a data-only way to show the same sheet without the
full SDK flow (no `init` required). The high-level `PurchaseSdk.showPurchasePopup` is preferred.

---

## Internal architecture

Everything below is `internal` — compiled into the SDK but **not visible** to the host app. The host
only ever touches `PurchaseSdk` and the public models.

```
PurchaseSdk (public facade)
        │  holds one
        ▼
   SdkRuntime ──────────────────────────────────────────────┐  (wires deps, owns a coroutine scope)
        │                                                    │
        ├── ApiClient            mock network seam (catalog, purchases, entitlements, analytics)
        ├── LocalStorage         SharedPreferences + Gson cache
        ├── AnalyticsTracker     fire-and-forget events (tagged with billingMode) → ApiClient
        ├── EntitlementManager   cache-first access checks → ApiClient + LocalStorage
        ├── BillingProvider      selected by BillingMode:
        │      ├── MockBillingProvider        local simulation (+ grants entitlement)
        │      └── GooglePlayBillingProvider  real Play Billing (scaffold + TODOs)
        ├── PurchaseManager      delegates to BillingProvider; adds analytics + error mapping
        └── PurchasePopupController  hosts PurchasePopupView in a bottom-sheet Dialog
                                     └── PurchasePopupView  the actual UI
Supporting: ErrorMapper, PurchaseValidator, DTO↔model Mappers, id helpers.
```

- **`BillingProvider`** (interface) — the billing strategy seam: `getItem`, `makePurchase`,
  `restorePurchases`. One implementation per `BillingMode`.
  - **`MockBillingProvider`** — local simulation over the mock `ApiClient`; grants entitlements
    locally so the whole flow works without Google Play.
  - **`GooglePlayBillingProvider`** — real Play Billing scaffold. Every method throws
    `PurchaseSdkError.GooglePlayNotConfigured` and is annotated with the exact `TODO`s needed
    (BillingClient setup, ProductDetails, `launchBillingFlow`, token verification, acknowledge/consume,
    response-code mapping, `setObfuscatedAccountId`).
- **`ApiClient`** — the single seam to "the network". Suspend functions: `fetchItem`, `fetchItems`,
  `createPurchase`, `confirmPurchase`, `cancelPurchase`, `restorePurchases`, `fetchEntitlements`,
  `sendAnalyticsEvent`. Currently mocked with a sample catalog, an in-memory purchase store, and
  simulated latency. Honors the request idempotency key. Marked with `TODO(backend)`.
- **`PurchaseManager`** — **selects the provider from the active `BillingMode`** and delegates
  `getItem` / `makePurchase` / `restorePurchases` to it, wrapping each with start/success/failure
  analytics and `ErrorMapper` normalization.
- **`PurchasePopupView`** — the self-contained bottom-sheet UI (`FrameLayout`). `bindItem` /
  `setLoadingState` / `showError` / `clearError`. Themes itself via a Material `ContextThemeWrapper`,
  so the host needs no Material theme.
- **`PurchasePopupController`** — builds, shows, and dismisses the popup in a `Dialog`; wires Confirm
  to `PurchaseManager` and routes results to the `PurchaseListener`. `showPopup` / `dismissPopup` /
  `isPopupShowing`.
- **`EntitlementManager`** — cache-first entitlement reads, refresh from API, and upsert after a
  successful purchase. Backed by an in-memory mirror + `LocalStorage`.
- **`LocalStorage`** — the only thing that touches `SharedPreferences`. Stores credentials, config,
  the item catalog cache, and the entitlement cache as Gson JSON.
- **`ErrorMapper`** — converts any throwable into a stable `PurchaseSdkError`; the place to map future
  Google Play `BillingResponseCode`s.
- **`AnalyticsTracker`** — structured `track*` events, each tagged with `billingMode`; fire-and-forget
  to `ApiClient`; no-op when analytics are disabled.
- **`PurchaseValidator`** — precondition checks (initialized, api key, item id, price).
- **`SdkRuntime`** — immutable container for one initialized session (managers + scope). `PurchaseSdk`
  holding one nullable `SdkRuntime` is what makes "initialized" a simple null check and guarantees no
  Activity is retained.

> **Package note:** Kotlin's `internal` is a *visibility modifier*, not a package. Internal classes
> live in topical packages (`api`, `billing`, `purchase`, `entitlement`, `storage`, `analytics`,
> `error`, `util`, `core`, `ui`) and are enforced `internal` per-class, rather than under an
> `internal/` folder (which would collide with the keyword).

---

## Public vs internal — at a glance

| Component | Visibility | Used By | Purpose |
|---|---|---|---|
| `PurchaseSdk` | public | Client app | Main SDK entry point |
| `BillingMode` | public | Client app | Selects MOCK vs GOOGLE_PLAY |
| `PurchaseItem` / `PurchaseResult` / `UserEntitlement` | public | Client app | Client-facing models |
| `PurchaseSdkError` / `PurchaseException` | public | Client app | Structured errors |
| `PurchaseListener` | public | Client app | Receives purchase callbacks |
| `PurchaseSdkConfig` / `SdkEnvironment` | public | Client app | Init configuration |
| `PurchasePopupData` / `PurchaseDialog` | public | Client app | Low-level data-only popup |
| `BillingProvider` | internal | SDK only | Billing strategy seam |
| `MockBillingProvider` | internal | SDK only | Local simulated billing |
| `GooglePlayBillingProvider` | internal | SDK only | Real Play Billing (scaffold + TODOs) |
| `ApiClient` | internal | SDK only | Network seam (mocked) |
| `PurchaseManager` | internal | SDK only | Orchestrates purchase flow over a provider |
| `PurchasePopupView` | internal | SDK only | Popup UI |
| `PurchasePopupController` | internal | SDK only | Shows/dismisses the popup |
| `EntitlementManager` | internal | SDK only | Manages access rights |
| `LocalStorage` | internal | SDK only | Local cache / prefs |
| `AnalyticsTracker` | internal | SDK only | Tracks SDK events |
| `ErrorMapper` | internal | SDK only | Converts errors |
| `PurchaseValidator` | internal | SDK only | Precondition checks |
| `SdkRuntime` | internal | SDK only | Session/dependency container |

---

## Usage examples

### Popup purchase flow

```kotlin
PurchaseSdk.showPurchasePopup(
    activity = this,
    itemId = "remove_ads",
    listener = object : PurchaseListener {
        override fun onPurchaseSuccess(result: PurchaseResult) {
            // unlock the feature, result.status == SUCCESS
        }
        override fun onPurchaseCancelled() { /* user backed out */ }
        override fun onPurchaseFailed(error: PurchaseSdkError) {
            // e.g. error is PurchaseSdkError.GooglePlayNotConfigured in GOOGLE_PLAY mode
        }
    },
)
```

### Direct purchase flow (your own UI)

```kotlin
lifecycleScope.launch {
    try {
        val result = PurchaseSdk.makePurchase("remove_ads")
        // result.purchaseId, result.status == SUCCESS
    } catch (e: PurchaseException) {
        // e.error.code / e.error.message
    }
}
```

### Entitlement check

```kotlin
lifecycleScope.launch {
    val hasAccess = PurchaseSdk.hasEntitlement("remove_ads")
    val all = PurchaseSdk.listEntitlements()
    val restored = PurchaseSdk.restorePurchases()
}
```

### Custom analytics

```kotlin
PurchaseSdk.trackEvent("paywall_viewed", mapOf("source" to "settings"))
```

---

## Error handling

Every failure is normalized to the sealed **`PurchaseSdkError`** (each case has a stable `code` and a
human `message`):

| Case (`code`) | When |
|---|---|
| `NotInitialized` (`SDK_NOT_INITIALIZED`) | A method was called before `init`. |
| `ItemNotFound` (`ITEM_NOT_FOUND`) | No item exists for the given id. |
| `BillingUnavailable` (`BILLING_UNAVAILABLE`) | Billing service unreachable / not ready (e.g. I/O failure). |
| `PurchaseCancelled` (`PURCHASE_CANCELLED`) | The user cancelled the purchase. |
| `PurchaseFailed` (`PURCHASE_FAILED`) | The purchase attempt failed. |
| `GooglePlayNotConfigured` (`GOOGLE_PLAY_NOT_CONFIGURED`) | `GOOGLE_PLAY` mode selected but real billing isn't wired up yet. |
| `VerificationRequired` (`VERIFICATION_REQUIRED`) | A purchase token needs server-side verification before granting. |
| `Unknown(message)` (`UNKNOWN_ERROR`) | Anything else. |

`suspend` APIs throw `PurchaseException` (which wraps a `PurchaseSdkError`); the popup delivers the
same `PurchaseSdkError` to `PurchaseListener.onPurchaseFailed`. `ErrorMapper` is the single place that
performs this normalization (and where future Google Play `BillingResponseCode`s get mapped).

```kotlin
try {
    PurchaseSdk.getItem("does_not_exist")
} catch (e: PurchaseException) {
    when (e.error) {
        is PurchaseSdkError.ItemNotFound -> showMissingItemMessage()
        is PurchaseSdkError.NotInitialized -> initThenRetry()
        is PurchaseSdkError.GooglePlayNotConfigured -> fallBackToMockOrToast()
        else -> showGenericError(e.error.message)
    }
}
```

---

## Notes for future backend integration

- All network calls already funnel through **`ApiClient`**. Replace each mocked function body with a
  real HTTP call (Retrofit/Ktor) to `baseUrl`, sending the `apiKey` as an auth header. The DTOs
  (`PurchaseItemDto`, `PurchaseDto`, `UserEntitlementDto`, `AnalyticsEventDto`) and request bodies
  (`CreatePurchaseRequest`, `AnalyticsEventRequest`) are already the shapes such an API would use.
- The `CreatePurchaseRequest.idempotencyKey` is ready to send as an `Idempotency-Key` header so
  retried / double-tapped Confirms collapse into a single server-side purchase.
- DTO↔model mapping lives in `api/Mappers.kt`; enum parsing is lenient so a new server enum value
  won't crash older clients.
- No other layer needs to change — managers depend on `ApiClient`, not on transport details.

## Notes for future Google Play Billing integration

- The integration point already exists: **`GooglePlayBillingProvider`** (in the `billing` package).
  Its class-level and per-method `TODO`s enumerate every real step — add the `billing-ktx` dependency,
  initialize/connect `BillingClient`, query `ProductDetails` from Play Console product ids, call
  `launchBillingFlow(activity, …)`, handle `PurchasesUpdatedListener`, **verify the purchase token
  server-side before granting**, acknowledge/consume, and map `BillingResponseCode` → `PurchaseSdkError`.
- Switching is just `init(billingMode = BillingMode.GOOGLE_PLAY)`. The public `PurchaseSdk` API and the
  `PurchaseListener` contract are identical across modes.
- `PurchaseItemType` already distinguishes `CONSUMABLE` / `SUBSCRIPTION` / `LIFETIME`, which maps onto
  Play's consumable vs. non-consumable vs. subscription product types.
- Use `setObfuscatedAccountId(...)` with a **hashed** user id — never raw email/user data.
- Reminder: the SDK popup is only a *pre-purchase* screen. Google Play's payment sheet is the final,
  non-replaceable confirmation UI.

## Current limitations

- **`GOOGLE_PLAY` mode is a scaffold** — it intentionally fails with `GooglePlayNotConfigured` until
  the `GooglePlayBillingProvider` TODOs are implemented. Only `MOCK` mode is functional today.
- **No real backend or payments** — `ApiClient` is an in-memory mock; purchases and entitlements do
  not persist server-side and reset when the process is killed (only the local caches persist).
- The popup's payment method is a static placeholder; there is no payment-method selection yet.
- Analytics events are logged/forwarded to the mock client only; there is no real ingestion.
- Local cache uses `SharedPreferences` + Gson (fine for v1; consider DataStore/Room later).
- Single active user/session at a time (switching users = call `init` again or `logout` first).
