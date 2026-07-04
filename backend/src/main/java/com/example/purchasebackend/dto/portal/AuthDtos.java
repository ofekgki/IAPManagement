package com.example.purchasebackend.dto.portal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Portal authentication DTOs (register/login/me). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
            String displayName) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password) {
    }

    public record UserDto(String id, String email, String displayName, String role) {
    }

    public record AuthResponse(String token, UserDto user) {
    }

    /** Create another portal user (any authenticated user may do this — roles were removed). */
    public record CreateUserRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
            String displayName) {
    }

    /**
     * Edit the signed-in user's own profile. All fields optional; a blank/absent field is left
     * unchanged. A new password (when present) must be at least 8 characters.
     */
    public record UpdateProfileRequest(
            @Email String email,
            String displayName,
            @Size(min = 8, message = "Password must be at least 8 characters") String password) {
    }
}
