package com.example.purchasebackend.common;

import com.example.purchasebackend.domain.DeveloperApp;

/**
 * Per-request, thread-local context populated by the servlet filters:
 * <ul>
 *   <li>{@code requestId} — from the {@code X-Request-Id} header (or generated).</li>
 *   <li>{@code developerApp} — the app resolved from the {@code X-SDK-API-Key} header.</li>
 * </ul>
 * Controllers and services read from here instead of re-resolving. {@code RequestIdFilter} clears it
 * at the end of each request to avoid leaking state across pooled threads.
 */
public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<DeveloperApp> DEVELOPER_APP = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String getRequestId() {
        return REQUEST_ID.get();
    }

    public static void setDeveloperApp(DeveloperApp app) {
        DEVELOPER_APP.set(app);
    }

    /** The resolved app, or throws if a protected endpoint was reached without one (defensive). */
    public static DeveloperApp requireDeveloperApp() {
        DeveloperApp app = DEVELOPER_APP.get();
        if (app == null) {
            throw new ApiException(ErrorCode.INVALID_API_KEY);
        }
        return app;
    }

    public static void clear() {
        REQUEST_ID.remove();
        DEVELOPER_APP.remove();
    }
}
