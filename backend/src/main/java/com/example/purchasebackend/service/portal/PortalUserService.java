package com.example.purchasebackend.service.portal;

import com.example.purchasebackend.common.ApiException;
import com.example.purchasebackend.common.ErrorCode;
import com.example.purchasebackend.common.Ids;
import com.example.purchasebackend.domain.DeveloperUser;
import com.example.purchasebackend.domain.enums.DeveloperUserRole;
import com.example.purchasebackend.dto.portal.AuthDtos.CreateUserRequest;
import com.example.purchasebackend.dto.portal.AuthDtos.UserDto;
import com.example.purchasebackend.repository.DeveloperUserRepository;
import com.example.purchasebackend.security.PasswordHasher;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Portal user management: list, add, and delete developer users. Roles were removed — every portal
 * user has full access, so there is no permission gating here.
 *
 * <p>// TODO: Add email invitations instead of setting a password directly.
 * <p>// TODO: Scope users to teams/orgs; today every portal user can see every app's data.
 */
@Service
public class PortalUserService {

    private final DeveloperUserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public PortalUserService(DeveloperUserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    public List<UserDto> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(DeveloperUser::getEmail, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toDto)
                .toList();
    }

    public UserDto create(DeveloperUser actor, CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        DeveloperUser user = new DeveloperUser();
        user.setId(Ids.newId("usr"));
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(req.password()));
        user.setDisplayName(req.displayName());
        // Roles were removed: every portal user has full access (OWNER).
        user.setRole(DeveloperUserRole.OWNER);
        userRepository.save(user);
        return toDto(user);
    }

    /** Deletes a portal user. You cannot delete your own account (that would lock you out). */
    public void delete(DeveloperUser actor, String userId) {
        if (actor.getId().equals(userId)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "You cannot delete your own account.");
        }
        DeveloperUser target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(target);
    }

    private UserDto toDto(DeveloperUser u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole().name());
    }
}
