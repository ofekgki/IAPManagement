package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.domain.ApiKey;
import com.example.purchasebackend.dto.portal.ApiKeyDtos.ApiKeyDto;
import com.example.purchasebackend.dto.portal.ApiKeyDtos.CreateApiKeyRequest;
import com.example.purchasebackend.dto.portal.ApiKeyDtos.CreatedApiKeyResponse;
import com.example.purchasebackend.security.PortalContext;
import com.example.purchasebackend.service.ApiKeyService;
import com.example.purchasebackend.service.portal.PortalAppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API-key management for an app (JWT). The raw key is returned only on create/rotate. */
@RestController
@RequestMapping("/api/v1/portal/apps/{appId}/api-keys")
public class PortalApiKeyController {

    private final ApiKeyService apiKeyService;
    private final PortalAppService appService;

    public PortalApiKeyController(ApiKeyService apiKeyService, PortalAppService appService) {
        this.apiKeyService = apiKeyService;
        this.appService = appService;
    }

    @GetMapping
    public ApiResponse<List<ApiKeyDto>> list(@PathVariable String appId) {
        authorize(appId);
        List<ApiKeyDto> keys = apiKeyService.list(appId).stream().map(PortalApiKeyController::toDto).toList();
        return ApiResponse.ok(keys, RequestContext.getRequestId());
    }

    @PostMapping
    public ApiResponse<CreatedApiKeyResponse> create(@PathVariable String appId,
                                                     @Valid @RequestBody CreateApiKeyRequest request) {
        authorize(appId);
        ApiKeyService.CreatedApiKey created = apiKeyService.create(appId, request.name());
        return ApiResponse.ok(toCreatedResponse(created), RequestContext.getRequestId());
    }

    @PostMapping("/{keyId}/revoke")
    public ApiResponse<ApiKeyDto> revoke(@PathVariable String appId, @PathVariable String keyId) {
        authorize(appId);
        return ApiResponse.ok(toDto(apiKeyService.revoke(appId, keyId)), RequestContext.getRequestId());
    }

    @PostMapping("/{keyId}/rotate")
    public ApiResponse<CreatedApiKeyResponse> rotate(@PathVariable String appId, @PathVariable String keyId) {
        authorize(appId);
        return ApiResponse.ok(toCreatedResponse(apiKeyService.rotate(appId, keyId)), RequestContext.getRequestId());
    }

    private void authorize(String appId) {
        appService.requireOwnedApp(appId, PortalContext.requireUser());
    }

    private static CreatedApiKeyResponse toCreatedResponse(ApiKeyService.CreatedApiKey created) {
        ApiKey key = created.apiKey();
        return new CreatedApiKeyResponse(created.rawApiKey(), key.getKeyPrefix(), toDto(key));
    }

    private static ApiKeyDto toDto(ApiKey key) {
        return new ApiKeyDto(key.getId(), key.getName(), key.getKeyPrefix(), key.getStatus().name(),
                key.getCreatedAt(), key.getRevokedAt(), key.getLastUsedAt());
    }
}
