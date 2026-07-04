// Shared types mirroring the backend DTOs. See also ../../shared for cross-project definitions.

export type BillingMode = "MOCK" | "GOOGLE_PLAY";
export type PaymentMethod = "APPLE_PAY" | "GOOGLE_PLAY" | "PAYPAL" | "CREDIT_CARD";

/** Display labels for payment methods (backend stores the enum name). */
export const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: "CREDIT_CARD", label: "Credit Card" },
  { value: "APPLE_PAY", label: "Apple Pay" },
  { value: "GOOGLE_PLAY", label: "Google Pay" },
  { value: "PAYPAL", label: "PayPal" },
];

export const paymentMethodLabel = (m: string | null | undefined): string =>
  PAYMENT_METHODS.find((p) => p.value === m)?.label ?? (m ?? "—");
export type ItemType = "NON_CONSUMABLE" | "CONSUMABLE" | "SUBSCRIPTION";
export type PurchaseStatus =
  | "CREATED"
  | "PENDING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED"
  | "REQUIRES_VERIFICATION"
  | "RESTORED";
export type EntitlementStatus = "ACTIVE" | "EXPIRED" | "REVOKED";
export type UserRole = "OWNER" | "ADMIN" | "VIEWER";

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown> | null;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: ApiError | null;
  requestId: string;
}

export interface User {
  id: string;
  email: string;
  displayName: string | null;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface App {
  id: string;
  appName: string;
  packageName: string;
  description: string | null;
  defaultBillingMode: BillingMode;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ApiKey {
  id: string;
  name: string;
  keyPrefix: string;
  status: "ACTIVE" | "REVOKED";
  createdAt: string;
  revokedAt: string | null;
  lastUsedAt: string | null;
}

export interface CreatedApiKey {
  apiKey: string;
  keyPrefix: string;
  key: ApiKey;
}

export interface Item {
  id: string;
  itemId: string;
  name: string;
  description: string | null;
  type: ItemType;
  entitlementId: string | null;
  priceAmountMinor: number | null;
  currency: string | null;
  priceDisplay: string | null;
  googlePlayProductId: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Purchase {
  purchaseId: string;
  userId: string;
  itemId: string;
  billingMode: BillingMode;
  paymentMethod: PaymentMethod | null;
  status: PurchaseStatus;
  revenueMinor: number | null;
  originalPriceMinor: number | null;
  currency: string | null;
  provider: string;
  failureCode: string | null;
  createdAt: string;
  completedAt: string | null;
}

/** One page of the purchases listing (the API never returns the full history at once). */
export interface PagedPurchases {
  items: Purchase[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface PurchaseDetail {
  purchase: Purchase;
  itemName: string | null;
  itemType: ItemType | null;
  failureMessage: string | null;
  providerOrderId: string | null;
  entitlement: { entitlementId: string; status: EntitlementStatus; expiresAt: string | null } | null;
  events: { eventName: string; billingMode: BillingMode | null; createdAt: string }[];
}

export interface Entitlement {
  id: string;
  userId: string;
  entitlementId: string;
  sourceItemId: string | null;
  status: EntitlementStatus;
  startsAt: string | null;
  expiresAt: string | null;
  purchaseId: string | null;
}

export interface Overview {
  popupShown: number;
  cancelClicked: number;
  confirmClicked: number;
  purchaseStarted: number;
  purchaseSuccess: number;
  purchaseFailed: number;
  restoreStarted: number;
  restoreSuccess: number;
  restoreFailed: number;
  entitlementChecked: number;
  popupToSuccessConversion: number;
  confirmToSuccessConversion: number;
  cancelRate: number;
  totalRevenueMinor: number;
  averageRevenueMinor: number;
  currency: string;
  activeEntitlements: number;
  restorePurchasesCount: number;
}

export interface FunnelStep {
  step: string;
  count: number;
  percentOfTop: number;
  percentOfPrev: number;
}

export interface Funnel {
  mainFunnel: FunnelStep[];
  cancelBranch: FunnelStep;
  popupShown: number;
}

export interface RevenueByPaymentMethod {
  paymentMethod: string;
  revenueMinor: number;
  purchases: number;
}

export interface RevenueByTimePoint {
  bucket: string;
  revenueMinor: number;
  purchases: number;
}

export interface RevenueSummary {
  totalRevenueMinor: number;
  currency: string;
  byPaymentMethod: RevenueByPaymentMethod[];
  overTime: RevenueByTimePoint[];
  restoredValueMinor: number;
  restoredCount: number;
}

export interface RevenueByProduct {
  itemId: string;
  name: string;
  successfulPurchases: number;
  totalRevenueMinor: number;
  averageRevenueMinor: number;
  popupViews: number;
  conversionRate: number;
  currency: string;
}

export interface PurchaseStatusRow {
  status: string;
  count: number;
}

export interface AnalyticsEventRow {
  id: string;
  userId: string | null;
  eventName: string;
  billingMode: BillingMode | null;
  itemId: string | null;
  purchaseId: string | null;
  createdAt: string;
  metadataJson: string | null;
}
