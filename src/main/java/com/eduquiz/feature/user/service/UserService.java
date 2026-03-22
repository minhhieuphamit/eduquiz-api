package com.eduquiz.feature.user.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.dto.PageResponse;
import com.eduquiz.common.exception.BadRequestException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.feature.auth.entity.Role;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.auth.repository.RefreshTokenRepository;
import com.eduquiz.feature.auth.repository.RoleRepository;
import com.eduquiz.feature.auth.repository.UserRepository;
import com.eduquiz.feature.user.dto.UpdateUserRoleRequest;
import com.eduquiz.feature.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // ══════════════════════════════════════════════════════
    // GET ALL USERS (search, filter, paginated) - ADMIN only
    // ══════════════════════════════════════════════════════

    public ApiResponse<PageResponse<UserResponse>> getAllUsers(String keyword, String roleName, Boolean isActive, Pageable pageable) {
        log.info("[UserService.getAllUsers] START - keyword={}, role={}, isActive={}, page={}, size={}",
                keyword, roleName, isActive, pageable.getPageNumber(), pageable.getPageSize());

        Page<UserResponse> page = userRepository.searchUsers(keyword, roleName, isActive, pageable)
                .map(this::toResponse);

        log.info("[UserService.getAllUsers] SUCCESS - totalElements={}, totalPages={}", page.getTotalElements(), page.getTotalPages());
        return ApiResponse.ok(ResponseCode.USER_LIST_SUCCESS, PageResponse.of(page));
    }

    // ══════════════════════════════════════════════════════
    // GET USER BY ID - ADMIN only
    // ══════════════════════════════════════════════════════

    public ApiResponse<UserResponse> getUserById(UUID id) {
        log.info("[UserService.getUserById] START - id={}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[UserService.getUserById] FAILED - user not found: {}", id);
                    return new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND);
                });

        log.info("[UserService.getUserById] SUCCESS - id={}, email={}", id, user.getEmail());
        return ApiResponse.ok(ResponseCode.USER_FETCH_SUCCESS, toResponse(user));
    }

    // ══════════════════════════════════════════════════════
    // UPDATE USER ROLE - ADMIN only
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<UserResponse> updateUserRole(UUID id, UpdateUserRoleRequest request, User currentAdmin) {
        log.info("[UserService.updateUserRole] START - targetId={}, newRole={}, adminEmail={}", id, request.getRole(), currentAdmin.getEmail());

        if (currentAdmin.getId().equals(id)) {
            log.warn("[UserService.updateUserRole] FAILED - admin tried to change own role: {}", currentAdmin.getEmail());
            throw new BadRequestException(ResponseCode.USER_CANNOT_CHANGE_OWN_ROLE);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[UserService.updateUserRole] FAILED - user not found: {}", id);
                    return new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND);
                });

        Role newRole = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> {
                    log.warn("[UserService.updateUserRole] FAILED - role not found: {}", request.getRole());
                    return new ResourceNotFoundException(ResponseCode.ROLE_NOT_FOUND);
                });

        String oldRole = user.getRole().getName();
        user.setRole(newRole);
        user.setUpdatedAt(LocalDateTime.now());

        // Invalidate tokens khi đổi role (quyền thay đổi)
        user.setTokenInvalidatedAt(LocalDateTime.now());
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);

        log.info("[UserService.updateUserRole] SUCCESS - id={}, email={}, oldRole={}, newRole={}, tokensInvalidated=true",
                id, user.getEmail(), oldRole, newRole.getName());
        return ApiResponse.ok(ResponseCode.USER_ROLE_UPDATED, toResponse(user));
    }

    // ══════════════════════════════════════════════════════
    // ACTIVATE USER - ADMIN only
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<UserResponse> activateUser(UUID id, User currentAdmin) {
        log.info("[UserService.activateUser] START - targetId={}, adminEmail={}", id, currentAdmin.getEmail());

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[UserService.activateUser] FAILED - user not found: {}", id);
                    return new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND);
                });

        if (user.getIsActive()) {
            log.warn("[UserService.activateUser] FAILED - user already active: {}", user.getEmail());
            throw new BadRequestException(ResponseCode.USER_ALREADY_ACTIVE);
        }

        user.setIsActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("[UserService.activateUser] SUCCESS - id={}, email={}", id, user.getEmail());
        return ApiResponse.ok(ResponseCode.USER_ACTIVATED, toResponse(user));
    }

    // ══════════════════════════════════════════════════════
    // DEACTIVATE USER - ADMIN only
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<UserResponse> deactivateUser(UUID id, User currentAdmin) {
        log.info("[UserService.deactivateUser] START - targetId={}, adminEmail={}", id, currentAdmin.getEmail());

        if (currentAdmin.getId().equals(id)) {
            log.warn("[UserService.deactivateUser] FAILED - admin tried to deactivate self: {}", currentAdmin.getEmail());
            throw new BadRequestException(ResponseCode.USER_CANNOT_DEACTIVATE_SELF);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[UserService.deactivateUser] FAILED - user not found: {}", id);
                    return new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND);
                });

        if (!user.getIsActive()) {
            log.warn("[UserService.deactivateUser] FAILED - user already inactive: {}", user.getEmail());
            throw new BadRequestException(ResponseCode.USER_ALREADY_INACTIVE);
        }

        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());

        // Invalidate tokens khi deactivate
        user.setTokenInvalidatedAt(LocalDateTime.now());
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);

        log.info("[UserService.deactivateUser] SUCCESS - id={}, email={}, tokensInvalidated=true", id, user.getEmail());
        return ApiResponse.ok(ResponseCode.USER_DEACTIVATED, toResponse(user));
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
