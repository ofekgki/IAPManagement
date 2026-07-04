package com.example.purchasebackend.web.portal;

import com.example.purchasebackend.common.ApiResponse;
import com.example.purchasebackend.common.RequestContext;
import com.example.purchasebackend.dto.portal.AuthDtos.CreateUserRequest;
import com.example.purchasebackend.dto.portal.AuthDtos.UserDto;
import com.example.purchasebackend.security.PortalContext;
import com.example.purchasebackend.service.portal.PortalUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Portal user management (JWT): list, add, and delete users. Any authenticated user may do all. */
@RestController
@RequestMapping("/api/v1/portal/users")
public class PortalUserController {

    private final PortalUserService userService;

    public PortalUserController(PortalUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserDto>> list() {
        PortalContext.requireUser();
        return ApiResponse.ok(userService.list(), RequestContext.getRequestId());
    }

    @PostMapping
    public ApiResponse<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(PortalContext.requireUser(), request),
                RequestContext.getRequestId());
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable String userId) {
        userService.delete(PortalContext.requireUser(), userId);
        return ApiResponse.ok(Map.of("deleted", true), RequestContext.getRequestId());
    }
}
