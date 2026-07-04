package com.example.purchasebackend.dto.common;

/** Body of GET /api/v1/health (returned unwrapped, not inside the ApiResponse envelope). */
public record HealthResponse(String status, String service) {
}
