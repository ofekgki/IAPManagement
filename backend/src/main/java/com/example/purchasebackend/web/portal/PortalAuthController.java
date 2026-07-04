package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.dto.portal.AuthDtos.AuthResponse;
import com.example.purchasebackend.dto.portal.AuthDtos.LoginRequest;
import com.example.purchasebackend.dto.portal.AuthDtos.RegisterRequest;
import com.example.purchasebackend.dto.portal.AuthDtos.UpdateProfileRequest;
import com.example.purchasebackend.dto.portal.AuthDtos.UserDto;
import com.example.purchasebackend.security.PortalContext;
import com.example.purchasebackend.service.portal.PortalAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Portal authentication (JWT). register/login are public; logout/me require a token. */
@RestController
@RequestMapping("/api/v1/portal/auth")
public class PortalAuthController {

    private final PortalAuthService authService;

    public PortalAuthController(PortalAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), RequestContext.getRequestId());
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), RequestContext.getRequestId());
    }

    /** Stateless JWT: the client simply discards the token. (No server-side session to clear yet.) */
    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout() {
        return ApiResponse.ok(Map.of("loggedOut", true), RequestContext.getRequestId());
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> me() {
        return ApiResponse.ok(authService.toUserDto(PortalContext.requireUser()), RequestContext.getRequestId());
    }

    /** Update the signed-in user's own profile (email / display name / password). Re-issues the JWT. */
    @PatchMapping("/me")
    public ApiResponse<AuthResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(
                authService.updateProfile(PortalContext.requireUser(), request), RequestContext.getRequestId());
    }
}
