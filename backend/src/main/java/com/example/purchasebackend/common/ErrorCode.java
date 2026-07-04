package com.example.purchasebackend.common;

import org.springframework.http.HttpStatus;

/**
 * The backend's stable, SDK-friendly error codes. Each maps to an HTTP status and a default message.
 * The {@code code} string is what clients switch on; messages are human-readable.
 */
public enum ErrorCode {

    NOT_INITIALIZED(HttpStatus.BAD_REQUEST, "The SDK session is not initialized."),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "The provided SDK API key is missing or invalid."),
    APP_DISABLED(HttpStatus.FORBIDDEN, "This developer app is disabled."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "The request was invalid."),
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested item was not found."),
    PURCHASE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested purchase was not found."),
    PURCHASE_ALREADY_PROCESSED(HttpStatus.CONFLICT, "This purchase was already processed."),
    PURCHASE_CANCELLED(HttpStatus.CONFLICT, "The purchase was cancelled."),
    PURCHASE_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "The purchase could not be completed."),
    BILLING_MODE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "The requested billing mode is not supported."),
    GOOGLE_PLAY_NOT_CONFIGURED(HttpStatus.NOT_IMPLEMENTED, "Google Play purchase verification is not configured yet."),
    VERIFICATION_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "Purchase verification is required before granting access."),
    PURCHASE_VERIFICATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Purchase verification failed."),
    ENTITLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested entitlement was not found."),
    // Internal admin endpoints (not part of the SDK contract, but kept consistent).
    ADMIN_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "The internal admin token is missing or invalid."),
    // Portal (JWT) errors.
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to perform this action."),
    APP_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested app was not found."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested user was not found."),
    API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested API key was not found."),
    ITEM_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "An item with this ID already exists for this app."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
