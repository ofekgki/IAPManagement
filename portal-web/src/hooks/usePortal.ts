import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import type {
  AnalyticsEventRow,
  ApiKey,
  App,
  AuthResponse,
  CreatedApiKey,
  Entitlement,
  Funnel,
  Item,
  Overview,
  PagedPurchases,
  PurchaseDetail,
  PurchaseStatusRow,
  RevenueByProduct,
  RevenueSummary,
  RevenueByTimePoint,
  User,
} from "../types";

const base = (appId: string) => `/api/v1/portal/apps/${appId}`;

type Filters = Record<string, string | undefined>;
const clean = (f: Filters) => Object.fromEntries(Object.entries(f).filter(([, v]) => v));

// --- Apps ----------------------------------------------------------------------------------
export const useApps = () => useQuery({ queryKey: ["apps"], queryFn: () => api.get<App[]>("/api/v1/portal/apps") });

export const useApp = (appId: string) =>
  useQuery({ queryKey: ["app", appId], queryFn: () => api.get<App>(`${base(appId)}`), enabled: !!appId });

export function useCreateApp() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Partial<App> & { appName: string; packageName: string }) =>
      api.post<App>("/api/v1/portal/apps", body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["apps"] }),
  });
}

export function useUpdateApp(appId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Partial<App>) => api.patch<App>(`${base(appId)}`, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["app", appId] });
      qc.invalidateQueries({ queryKey: ["apps"] });
    },
  });
}

// --- Users ---------------------------------------------------------------------------------
export const useUsers = () =>
  useQuery({ queryKey: ["users"], queryFn: () => api.get<User[]>("/api/v1/portal/users") });

export function useCreateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { email: string; password: string; displayName?: string }) =>
      api.post<User>("/api/v1/portal/users", body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
}

export function useDeleteUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => api.del<{ deleted: boolean }>(`/api/v1/portal/users/${userId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["users"] }),
  });
}

// --- Profile (self) ------------------------------------------------------------------------
export function useUpdateProfile() {
  return useMutation({
    mutationFn: (body: { email?: string; displayName?: string; password?: string }) =>
      api.patch<AuthResponse>("/api/v1/portal/auth/me", body),
  });
}

// --- API keys ------------------------------------------------------------------------------
export const useApiKeys = (appId: string) =>
  useQuery({ queryKey: ["apiKeys", appId], queryFn: () => api.get<ApiKey[]>(`${base(appId)}/api-keys`) });

export function useCreateApiKey(appId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => api.post<CreatedApiKey>(`${base(appId)}/api-keys`, { name }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["apiKeys", appId] }),
  });
}

export function useRevokeApiKey(appId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (keyId: string) => api.post<ApiKey>(`${base(appId)}/api-keys/${keyId}/revoke`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["apiKeys", appId] }),
  });
}

export function useRotateApiKey(appId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (keyId: string) => api.post<CreatedApiKey>(`${base(appId)}/api-keys/${keyId}/rotate`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["apiKeys", appId] }),
  });
}

// --- Items ---------------------------------------------------------------------------------
export const useItems = (appId: string) =>
  useQuery({ queryKey: ["items", appId], queryFn: () => api.get<Item[]>(`${base(appId)}/items`) });

export const useItem = (appId: string, itemId: string) =>
  useQuery({ queryKey: ["item", appId, itemId], queryFn: () => api.get<Item>(`${base(appId)}/items/${itemId}`), enabled: !!itemId });

export function useCreateItem(appId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Record<string, unknown>) => api.post<Item>(`${base(appId)}/items`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["items", appId] }),
  });
}

export function useUpdateItem(appId: string, itemId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Record<string, unknown>) => api.patch<Item>(`${base(appId)}/items/${itemId}`, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["items", appId] });
      qc.invalidateQueries({ queryKey: ["item", appId, itemId] });
    },
  });
}

export function useSetItemActive(appId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ itemId, active }: { itemId: string; active: boolean }) =>
      api.post<Item>(`${base(appId)}/items/${itemId}/${active ? "enable" : "disable"}`),
    onSuccess: (_data, { itemId }) => {
      qc.invalidateQueries({ queryKey: ["items", appId] });
      // Also refresh the item detail query so the Active/Disabled badge updates immediately.
      qc.invalidateQueries({ queryKey: ["item", appId, itemId] });
    },
  });
}

// --- Purchases -----------------------------------------------------------------------------
export const usePurchases = (appId: string, filters: Filters, page: number, size = 50) =>
  useQuery({
    queryKey: ["purchases", appId, filters, page, size],
    queryFn: () =>
      api.get<PagedPurchases>(`${base(appId)}/purchases`, {
        ...clean(filters),
        page: String(page),
        size: String(size),
      }),
  });

export const usePurchase = (appId: string, purchaseId: string) =>
  useQuery({
    queryKey: ["purchase", appId, purchaseId],
    queryFn: () => api.get<PurchaseDetail>(`${base(appId)}/purchases/${purchaseId}`),
    enabled: !!purchaseId,
  });

// --- Entitlements --------------------------------------------------------------------------
export const useEntitlements = (appId: string, filters: Filters) =>
  useQuery({
    queryKey: ["entitlements", appId, filters],
    queryFn: () => api.get<Entitlement[]>(`${base(appId)}/entitlements`, clean(filters)),
  });

export function useGrantEntitlement(appId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Record<string, unknown>) => api.post<Entitlement>(`${base(appId)}/entitlements/grant`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["entitlements", appId] }),
  });
}

export function useRevokeEntitlement(appId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: Record<string, unknown>) => api.post<Entitlement>(`${base(appId)}/entitlements/revoke`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["entitlements", appId] }),
  });
}

// --- Analytics -----------------------------------------------------------------------------
export const useOverview = (appId: string, filters: Filters) =>
  useQuery({
    queryKey: ["overview", appId, filters],
    queryFn: () => api.get<Overview>(`${base(appId)}/analytics/overview`, clean(filters)),
  });

export const useFunnel = (appId: string, filters: Filters) =>
  useQuery({
    queryKey: ["funnel", appId, filters],
    queryFn: () => api.get<Funnel>(`${base(appId)}/analytics/funnel`, clean(filters)),
  });

export const useRevenue = (appId: string, filters: Filters) =>
  useQuery({
    queryKey: ["revenue", appId, filters],
    queryFn: () => api.get<RevenueSummary>(`${base(appId)}/analytics/revenue`, clean(filters)),
  });

export const useRevenueByProduct = (appId: string, filters: Filters) =>
  useQuery({
    queryKey: ["revenueByProduct", appId, filters],
    queryFn: () => api.get<RevenueByProduct[]>(`${base(appId)}/analytics/revenue/by-product`, clean(filters)),
  });

export const useRevenueByTime = (appId: string, filters: Filters) =>
  useQuery({
    queryKey: ["revenueByTime", appId, filters],
    queryFn: () => api.get<RevenueByTimePoint[]>(`${base(appId)}/analytics/revenue/by-time`, clean(filters)),
  });

export const useEvents = (appId: string, filters: Filters) =>
  useQuery({
    queryKey: ["events", appId, filters],
    queryFn: () => api.get<AnalyticsEventRow[]>(`${base(appId)}/analytics/events`, clean(filters)),
  });

export const usePurchasesByStatus = (appId: string, filters: Filters) =>
  useQuery({
    queryKey: ["purchasesByStatus", appId, filters],
    queryFn: () =>
      api.get<PurchaseStatusRow[]>(`${base(appId)}/analytics/purchases-by-status`, clean(filters)),
  });

// --- Demo maintenance (dev/demo only) ------------------------------------------------------
export type ResetResult = {
  reseeded: boolean;
  items: number;
  purchases: number;
  entitlements: number;
  events: number;
};

/**
 * Wipes all demo data and (optionally) regenerates it from scratch via the backend
 * /portal/maintenance/reset-demo-data endpoint. On success every cached query is invalidated so the
 * whole portal reflects the fresh dataset.
 */
export function useResetDemoData() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reseed: boolean) =>
      api.post<ResetResult>(`/api/v1/portal/maintenance/reset-demo-data?reseed=${reseed}`),
    onSuccess: () => qc.invalidateQueries(),
  });
}
