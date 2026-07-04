package com.example.purchasebackend.service.portal;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.DeveloperApp;
import com.example.purchasebackend.domain.DeveloperUser;
import com.example.purchasebackend.dto.portal.AppDtos.AppDto;
import com.example.purchasebackend.dto.portal.AppDtos.CreateAppRequest;
import com.example.purchasebackend.dto.portal.AppDtos.UpdateAppRequest;
import com.example.purchasebackend.domain.enums.BillingMode;
import com.example.purchasebackend.repository.DeveloperAppRepository;
import com.example.purchasebackend.service.support.BillingModes;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Developer app management for the portal, with per-user ownership enforcement.
 *
 * <p>// TODO: In production, validate package name ownership.
 * <p>// TODO: Add app signing certificate fingerprint validation.
 */
@Service
public class PortalAppService {

    private final DeveloperAppRepository appRepository;

    public PortalAppService(DeveloperAppRepository appRepository) {
        this.appRepository = appRepository;
    }

    /** Loads an app and verifies the user owns it (legacy apps with no owner are accessible). */
    public DeveloperApp requireOwnedApp(String appId, DeveloperUser user) {
        DeveloperApp app = appRepository.findById(appId)
                .orElseThrow(() -> new ApiException(ErrorCode.APP_NOT_FOUND));
        if (app.getOwnerUserId() != null && !app.getOwnerUserId().equals(user.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return app;
    }

    public List<AppDto> listApps(DeveloperUser user) {
        return appRepository.findByOwnerUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(PortalAppService::toDto)
                .toList();
    }

    public AppDto createApp(DeveloperUser user, CreateAppRequest request) {
        DeveloperApp app = new DeveloperApp();
        app.setId(Ids.newId("app"));
        app.setOwnerUserId(user.getId());
        app.setAppName(request.appName());
        app.setPackageName(request.packageName());
        app.setDescription(request.description());
        app.setBillingModeDefault(BillingModes.parseOrDefault(request.defaultBillingMode(), BillingMode.MOCK));
        app.setActive(true);
        return toDto(appRepository.save(app));
    }

    public AppDto getApp(String appId, DeveloperUser user) {
        return toDto(requireOwnedApp(appId, user));
    }

    public AppDto updateApp(String appId, DeveloperUser user, UpdateAppRequest request) {
        DeveloperApp app = requireOwnedApp(appId, user);
        if (request.appName() != null) {
            app.setAppName(request.appName());
        }
        if (request.packageName() != null) {
            app.setPackageName(request.packageName());
        }
        if (request.description() != null) {
            app.setDescription(request.description());
        }
        if (request.defaultBillingMode() != null) {
            app.setBillingModeDefault(
                    BillingModes.parseOrDefault(request.defaultBillingMode(), app.getBillingModeDefault()));
        }
        if (request.isActive() != null) {
            app.setActive(request.isActive());
        }
        return toDto(appRepository.save(app));
    }

    /** Soft delete: deactivate rather than removing data (preserves purchase history). */
    public void deactivateApp(String appId, DeveloperUser user) {
        DeveloperApp app = requireOwnedApp(appId, user);
        app.setActive(false);
        appRepository.save(app);
    }

    static AppDto toDto(DeveloperApp app) {
        return new AppDto(
                app.getId(),
                app.getAppName(),
                app.getPackageName(),
                app.getDescription(),
                app.getBillingModeDefault().name(),
                app.isActive(),
                app.getCreatedAt(),
                app.getUpdatedAt());
    }
}
