package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.dto.portal.AppDtos.AppDto;
import com.example.purchasebackend.dto.portal.AppDtos.CreateAppRequest;
import com.example.purchasebackend.dto.portal.AppDtos.UpdateAppRequest;
import com.example.purchasebackend.security.PortalContext;
import com.example.purchasebackend.service.portal.PortalAppService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Developer app CRUD (JWT). Delete is a soft-deactivate. */
@RestController
@RequestMapping("/api/v1/portal/apps")
public class PortalAppController {

    private final PortalAppService appService;

    public PortalAppController(PortalAppService appService) {
        this.appService = appService;
    }

    @GetMapping
    public ApiResponse<List<AppDto>> list() {
        return ApiResponse.ok(appService.listApps(PortalContext.requireUser()), RequestContext.getRequestId());
    }

    @PostMapping
    public ApiResponse<AppDto> create(@Valid @RequestBody CreateAppRequest request) {
        return ApiResponse.ok(appService.createApp(PortalContext.requireUser(), request),
                RequestContext.getRequestId());
    }

    @GetMapping("/{appId}")
    public ApiResponse<AppDto> get(@PathVariable String appId) {
        return ApiResponse.ok(appService.getApp(appId, PortalContext.requireUser()), RequestContext.getRequestId());
    }

    @PatchMapping("/{appId}")
    public ApiResponse<AppDto> update(@PathVariable String appId, @RequestBody UpdateAppRequest request) {
        return ApiResponse.ok(appService.updateApp(appId, PortalContext.requireUser(), request),
                RequestContext.getRequestId());
    }

    @DeleteMapping("/{appId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String appId) {
        appService.deactivateApp(appId, PortalContext.requireUser());
        return ApiResponse.ok(Map.of("deactivated", true), RequestContext.getRequestId());
    }
}
