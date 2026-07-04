package com.example.purchasebackend.web;

import com.example.purchasebackend.dto.common.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness/health endpoint. Public — not behind the API-key filter. */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "purchase-sdk-backend");
    }
}
