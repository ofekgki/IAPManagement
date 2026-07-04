package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.config.SeedDataLoader;
import com.example.purchasebackend.security.PortalContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demo-only maintenance endpoints (JWT-protected, under {@code /api/v1/portal/**}).
 *
 * <p>Powers the portal's "danger zone": wipe all demo data and optionally regenerate it from scratch.
 * The whole controller only exists when {@code app.seed.enabled=true} (dev/docker), so it is absent
 * in production — a deliberate fail-safe: there is no "delete everything" button on a real deployment.
 *
 * <p>// TODO: Before any non-demo use, scope this to the caller's own apps and gate it behind an
 *      explicit admin role; today it resets the entire demo dataset (all apps).
 */
@RestController
@RequestMapping("/api/v1/portal/maintenance")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class PortalMaintenanceController {

    private final SeedDataLoader seedDataLoader;

    public PortalMaintenanceController(SeedDataLoader seedDataLoader) {
        this.seedDataLoader = seedDataLoader;
    }

    /**
     * POST /api/v1/portal/maintenance/reset-demo-data?reseed=true|false
     *
     * <ul>
     *   <li>{@code reseed=true} (default) — wipe everything, then regenerate the full demo dataset.</li>
     *   <li>{@code reseed=false} — wipe everything and leave the database empty.</li>
     * </ul>
     *
     * @return a summary of the resulting row counts.
     */
    @PostMapping("/reset-demo-data")
    public ApiResponse<Map<String, Object>> resetDemoData(
            @RequestParam(name = "reseed", defaultValue = "true") boolean reseed) {
        PortalContext.requireUser(); // must be an authenticated portal user
        Map<String, Object> result = seedDataLoader.reset(reseed);
        return ApiResponse.ok(result, RequestContext.getRequestId());
    }
}
