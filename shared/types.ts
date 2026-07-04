/**
 * Shared type definitions across the platform. These mirror the backend enums/DTOs and the
 * portal-web TypeScript types. Treat this file as the cross-project contract reference.
 *
 * The portal app re-declares these in portal-web/src/types for bundling convenience; keep them in
 * sync with this file and with the Java enums in
 * backend/src/main/java/com/example/purchasebackend/domain/enums.
 */

export type BillingMode = "MOCK" | "GOOGLE_PLAY";
export type ItemType = "NON_CONSUMABLE" | "CONSUMABLE" | "SUBSCRIPTION";
export type PurchaseStatus =
  | "CREATED"
  | "PENDING"
  | "SUCCESS"
  | "FAILED"
  | "CANCELLED"
  | "REQUIRES_VERIFICATION";
export type EntitlementStatus = "ACTIVE" | "EXPIRED" | "REVOKED";
export type UserRole = "OWNER" | "ADMIN" | "VIEWER";
export type ApiKeyStatus = "ACTIVE" | "REVOKED";

/** The consistent response envelope returned by every backend endpoint. */
export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  error: { code: string; message: string; details?: Record<string, unknown> | null } | null;
  requestId: string;
}
