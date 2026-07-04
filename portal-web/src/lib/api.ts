import axios, { AxiosError } from "axios";
import type { ApiResponse } from "../types";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const TOKEN_KEY = "psdk_portal_token";

export const http = axios.create({ baseURL });

// Attach the JWT on every request.
http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** A friendly error carrying the backend error code so the UI can react/display it. */
export class PortalApiError extends Error {
  code: string;
  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

// Redirect to login on auth failures (except while already on auth pages).
http.interceptors.response.use(
  (res) => res,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status;
    const path = window.location.pathname;
    if (status === 401 && !path.startsWith("/login") && !path.startsWith("/register")) {
      localStorage.removeItem(TOKEN_KEY);
      window.location.assign("/login");
    }
    return Promise.reject(error);
  },
);

function toError(error: unknown): PortalApiError {
  const ax = error as AxiosError<ApiResponse<unknown>>;
  const apiError = ax.response?.data?.error;
  if (apiError) {
    return new PortalApiError(apiError.code, apiError.message);
  }
  return new PortalApiError("NETWORK_ERROR", ax.message || "Network error. Is the backend running?");
}

/** Unwraps the { success, data, error } envelope, throwing a PortalApiError on failure. */
async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const res = await promise;
    if (!res.data.success || res.data.data === null) {
      const e = res.data.error;
      throw new PortalApiError(e?.code ?? "UNKNOWN", e?.message ?? "Request failed");
    }
    return res.data.data;
  } catch (err) {
    if (err instanceof PortalApiError) throw err;
    throw toError(err);
  }
}

export const api = {
  get: <T>(url: string, params?: Record<string, unknown>) =>
    unwrap<T>(http.get(url, { params })),
  post: <T>(url: string, body?: unknown) => unwrap<T>(http.post(url, body)),
  patch: <T>(url: string, body?: unknown) => unwrap<T>(http.patch(url, body)),
  del: <T>(url: string) => unwrap<T>(http.delete(url)),
};
