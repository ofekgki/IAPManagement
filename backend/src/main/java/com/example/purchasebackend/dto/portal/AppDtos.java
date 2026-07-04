package com.example.purchasebackend.dto.portal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/** Developer app DTOs. */
public final class AppDtos {

    private AppDtos() {
    }

    private static final String PACKAGE_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$";

    public record CreateAppRequest(
            @NotBlank String appName,
            @NotBlank @Pattern(regexp = PACKAGE_REGEX, message = "Package name must look like com.example.app")
            String packageName,
            String defaultBillingMode,
            String description) {
    }

    public record UpdateAppRequest(
            String appName,
            @Pattern(regexp = PACKAGE_REGEX, message = "Package name must look like com.example.app")
            String packageName,
            String defaultBillingMode,
            String description,
            Boolean isActive) {
    }

    public record AppDto(
            String id,
            String appName,
            String packageName,
            String description,
            String defaultBillingMode,
            boolean isActive,
            Instant createdAt,
            Instant updatedAt) {
    }
}
