package com.example.purchasebackend.service.portal;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.DeveloperUser;
import com.example.purchasebackend.domain.enums.DeveloperUserRole;
import com.example.purchasebackend.dto.portal.AuthDtos.AuthResponse;
import com.example.purchasebackend.dto.portal.AuthDtos.LoginRequest;
import com.example.purchasebackend.dto.portal.AuthDtos.RegisterRequest;
import com.example.purchasebackend.dto.portal.AuthDtos.UpdateProfileRequest;
import com.example.purchasebackend.dto.portal.AuthDtos.UserDto;
import com.example.purchasebackend.repository.DeveloperUserRepository;
import com.example.purchasebackend.security.JwtService;
import com.example.purchasebackend.security.PasswordHasher;
import org.springframework.stereotype.Service;

/**
 * Portal authentication: register, login, and current-user mapping. Issues JWTs for the portal.
 *
 * <p>// TODO: Add email verification.
 * <p>// TODO: Add password reset.
 * <p>// TODO: Add OAuth login later.
 */
@Service
public class PortalAuthService {

    private final DeveloperUserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    public PortalAuthService(DeveloperUserRepository userRepository, PasswordHasher passwordHasher,
                             JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        DeveloperUser user = new DeveloperUser();
        user.setId(Ids.newId("usr"));
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(request.password()));
        user.setDisplayName(request.displayName());
        user.setRole(DeveloperUserRole.OWNER);
        userRepository.save(user);
        return authResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        DeveloperUser user = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordHasher.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        return authResponse(user);
    }

    /**
     * Updates the signed-in user's own email / display name / password. Returns a fresh
     * {@link AuthResponse} because changing the email re-issues the JWT (the token embeds the email).
     */
    public AuthResponse updateProfile(DeveloperUser user, UpdateProfileRequest req) {
        if (req.email() != null && !req.email().isBlank()) {
            String email = req.email().trim().toLowerCase();
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
                throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(email);
        }
        if (req.displayName() != null) {
            user.setDisplayName(req.displayName().isBlank() ? null : req.displayName().trim());
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordHasher.hash(req.password()));
        }
        userRepository.save(user);
        return authResponse(user);
    }

    public UserDto toUserDto(DeveloperUser user) {
        return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name());
    }

    private AuthResponse authResponse(DeveloperUser user) {
        String token = jwtService.issue(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, toUserDto(user));
    }
}
