package com.eduquiz.feature.user.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.user.dto.UpdateUserRoleRequest;
import com.eduquiz.feature.user.dto.UserResponse;
import com.eduquiz.feature.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Quản lý user (ADMIN)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách user (search, filter, phân trang) - ADMIN")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("[UserController.getAllUsers] START - keyword={}, role={}, isActive={}, page={}, size={}",
                keyword, role, isActive, pageable.getPageNumber(), pageable.getPageSize());
        ApiResponse<PageResponse<UserResponse>> response = userService.getAllUsers(keyword, role, isActive, pageable);
        log.info("[UserController.getAllUsers] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy chi tiết user theo ID - ADMIN")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        log.info("[UserController.getUserById] START - id={}", id);
        ApiResponse<UserResponse> response = userService.getUserById(id);
        log.info("[UserController.getUserById] SUCCESS - id={}, code={}", id, response.getCode());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đổi role cho user - ADMIN")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentAdmin) {
        log.info("[UserController.updateUserRole] START - targetId={}, newRole={}, adminEmail={}",
                id, request.getRole(), currentAdmin.getEmail());
        ApiResponse<UserResponse> response = userService.updateUserRole(id, request, currentAdmin);
        log.info("[UserController.updateUserRole] SUCCESS - id={}, code={}", id, response.getCode());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kích hoạt tài khoản user - ADMIN")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentAdmin) {
        log.info("[UserController.activateUser] START - targetId={}, adminEmail={}", id, currentAdmin.getEmail());
        ApiResponse<UserResponse> response = userService.activateUser(id, currentAdmin);
        log.info("[UserController.activateUser] SUCCESS - id={}, code={}", id, response.getCode());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Vô hiệu hóa tài khoản user - ADMIN")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @PathVariable UUID id,
            @Parameter(hidden = true) @AuthenticationPrincipal User currentAdmin) {
        log.info("[UserController.deactivateUser] START - targetId={}, adminEmail={}", id, currentAdmin.getEmail());
        ApiResponse<UserResponse> response = userService.deactivateUser(id, currentAdmin);
        log.info("[UserController.deactivateUser] SUCCESS - id={}, code={}", id, response.getCode());
        return ResponseEntity.ok(response);
    }
}
