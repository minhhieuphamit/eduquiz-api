package com.eduquiz.feature.auth.controller;

import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.feature.auth.dto.*;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, xác thực OTP, refresh token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("[AuthController.register] START - email={}, role={}", request.getEmail(), request.getRole());
        ApiResponse<Void> response = authService.register(request);
        log.info("[AuthController.register] SUCCESS - email={}, code={}", request.getEmail(), response.getCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xác thực email bằng mã OTP")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("[AuthController.verifyOtp] START - email={}", request.getEmail());
        ApiResponse<Void> response = authService.verifyOtp(request);
        log.info("[AuthController.verifyOtp] SUCCESS - email={}, code={}", request.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Gửi lại mã OTP")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@RequestParam String email) {
        log.info("[AuthController.resendOtp] START - email={}", email);
        ApiResponse<Void> response = authService.resendOtp(email);
        log.info("[AuthController.resendOtp] SUCCESS - email={}, code={}", email, response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("[AuthController.login] START - email={}", request.getEmail());
        ApiResponse<AuthResponse> response = authService.login(request);
        log.info("[AuthController.login] SUCCESS - email={}, code={}", request.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token bằng refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("[AuthController.refreshToken] START");
        ApiResponse<AuthResponse> response = authService.refreshToken(request);
        log.info("[AuthController.refreshToken] SUCCESS - code={}", response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất (thu hồi tất cả refresh token)")
    public ResponseEntity<ApiResponse<Void>> logout(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        log.info("[AuthController.logout] START - email={}", user.getEmail());
        ApiResponse<Void> response = authService.logout(user);
        log.info("[AuthController.logout] SUCCESS - email={}, code={}", user.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin user hiện tại")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        log.info("[AuthController.getCurrentUser] START - email={}", user.getEmail());
        ApiResponse<UserInfoResponse> response = authService.getCurrentUser(user);
        log.info("[AuthController.getCurrentUser] SUCCESS - email={}, code={}", user.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu đặt lại mật khẩu (quên mật khẩu)")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("[AuthController.forgotPassword] START - email={}", request.getEmail());
        ApiResponse<Void> response = authService.forgotPassword(request);
        log.info("[AuthController.forgotPassword] SUCCESS - email={}, code={}", request.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Xác nhận đặt lại mật khẩu mới")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("[AuthController.resetPassword] START - email={}", request.getEmail());
        ApiResponse<Void> response = authService.resetPassword(request);
        log.info("[AuthController.resetPassword] SUCCESS - email={}, code={}", request.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-change/request")
    @Operation(summary = "Yêu cầu thay đổi mật khẩu (khi đã đăng nhập)")
    public ResponseEntity<ApiResponse<Void>> requestPasswordChange(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordInitRequest request
    ) {
        log.info("[AuthController.requestPasswordChange] START - email={}", user.getEmail());
        ApiResponse<Void> response = authService.requestPasswordChange(user, request);
        log.info("[AuthController.requestPasswordChange] SUCCESS - email={}, code={}", user.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/password-change/confirm")
    @Operation(summary = "Xác nhận thay đổi mật khẩu (khi đã đăng nhập)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("[AuthController.changePassword] START - email={}", user.getEmail());
        ApiResponse<Void> response = authService.changePassword(user, request);
        log.info("[AuthController.changePassword] SUCCESS - email={}, code={}", user.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile")
    @Operation(summary = "Cập nhật thông tin cá nhân")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        log.info("[AuthController.updateProfile] START - email={}", user.getEmail());
        ApiResponse<UserInfoResponse> response = authService.updateProfile(user, request);
        log.info("[AuthController.updateProfile] SUCCESS - email={}, code={}", user.getEmail(), response.getCode());
        return ResponseEntity.ok(response);
    }
}
