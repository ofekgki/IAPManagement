package com.example.purchasebackend.security;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.domain.DeveloperUser;

/** Thread-local holder for the authenticated portal user (set by {@link PortalAuthFilter}). */
public final class PortalContext {

    private static final ThreadLocal<DeveloperUser> USER = new ThreadLocal<>();

    private PortalContext() {
    }

    public static void setUser(DeveloperUser user) {
        USER.set(user);
    }

    public static DeveloperUser requireUser() {
        DeveloperUser user = USER.get();
        if (user == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static void clear() {
        USER.remove();
    }
}
