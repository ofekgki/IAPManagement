package com.example.purchasebackend.service.portal;

import com.example.purchasebackend.domain.DeveloperUser;
import com.example.purchasebackend.domain.Entitlement;
import com.example.purchasebackend.domain.enums.EntitlementStatus;
import com.example.purchasebackend.dto.internal.GrantEntitlementRequest;
import com.example.purchasebackend.dto.internal.RevokeEntitlementRequest;
import com.example.purchasebackend.dto.portal.EntitlementDtos.GrantRequest;
import com.example.purchasebackend.dto.portal.EntitlementDtos.PortalEntitlementDto;
import com.example.purchasebackend.dto.portal.EntitlementDtos.RevokeRequest;
import com.example.purchasebackend.repository.EntitlementRepository;
import com.example.purchasebackend.repository.PurchaseItemRepository;
import com.example.purchasebackend.service.EntitlementService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Portal entitlements: list with filters, and manual grant/revoke (any authenticated portal user).
 *
 * <p>// TODO: Add audit log for all manual entitlement changes.
 */
@Service
public class PortalEntitlementService {

    private final EntitlementRepository entitlementRepository;
    private final PurchaseItemRepository itemRepository;
    private final EntitlementService entitlementService;

    public PortalEntitlementService(EntitlementRepository entitlementRepository,
                                    PurchaseItemRepository itemRepository,
                                    EntitlementService entitlementService) {
        this.entitlementRepository = entitlementRepository;
        this.itemRepository = itemRepository;
        this.entitlementService = entitlementService;
    }

    public List<PortalEntitlementDto> list(String appId, String userId, String entitlementId,
                                           EntitlementStatus status, String itemId) {
        // Resolve itemId filter → entitlementId (items declare their entitlement).
        String itemEntitlementId = itemId == null ? null : itemRepository
                .findByDeveloperAppIdAndItemId(appId, itemId)
                .map(i -> i.getEntitlementId()).orElse("__none__");

        // userId/entitlementId are typed by hand, so match case-insensitive "contains" (a partial or
        // differently-cased id still matches) rather than exact equality.
        String userNeedle = normalize(userId);
        String entNeedle = normalize(entitlementId);
        return entitlementRepository.findByDeveloperAppId(appId).stream()
                .filter(e -> userNeedle == null || contains(e.getUserId(), userNeedle))
                .filter(e -> entNeedle == null || contains(e.getEntitlementId(), entNeedle))
                .filter(e -> status == null || e.getStatus() == status)
                .filter(e -> itemEntitlementId == null || itemEntitlementId.equals(e.getEntitlementId()))
                // Most-recent first: order by start date, falling back to created date.
                .sorted(Comparator.comparing(
                        (Entitlement e) -> e.getStartsAt() != null ? e.getStartsAt()
                                : (e.getCreatedAt() != null ? e.getCreatedAt() : Instant.EPOCH),
                        Comparator.reverseOrder()))
                .map(PortalEntitlementService::toDto)
                .toList();
    }

    public PortalEntitlementDto grant(String appId, DeveloperUser user, GrantRequest req) {
        Entitlement entitlement = entitlementService.grantManual(new GrantEntitlementRequest(
                appId, req.userId(), req.entitlementId(), req.sourceItemId(), req.expiresAt(), req.reason()));
        return toDto(entitlement);
    }

    public PortalEntitlementDto revoke(String appId, DeveloperUser user, RevokeRequest req) {
        Entitlement entitlement = entitlementService.revoke(new RevokeEntitlementRequest(
                appId, req.userId(), req.entitlementId(), req.reason()));
        return toDto(entitlement);
    }

    /** Lower-cased, trimmed filter needle, or null when blank. */
    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    /** Case-insensitive "contains" against an already-lower-cased [needle]. */
    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle);
    }

    static PortalEntitlementDto toDto(Entitlement e) {
        return new PortalEntitlementDto(
                e.getId(), e.getUserId(), e.getEntitlementId(), e.getSourceItemId(),
                e.getStatus().name(), e.getStartsAt(), e.getExpiresAt(), e.getPurchaseId());
    }
}
