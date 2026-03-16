package com.eduquiz.feature.auth.service;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import com.eduquiz.common.exception.BadRequestException;
import com.eduquiz.common.exception.DuplicateResourceException;
import com.eduquiz.common.exception.OtpVerificationException;
import com.eduquiz.common.exception.ResourceNotFoundException;
import com.eduquiz.common.util.OtpGenerator;
import com.eduquiz.feature.auth.dto.*;
import com.eduquiz.feature.auth.entity.EmailVerification;
import com.eduquiz.feature.auth.entity.RefreshToken;
import com.eduquiz.feature.auth.entity.User;
import com.eduquiz.feature.auth.repository.EmailVerificationRepository;
import com.eduquiz.feature.auth.repository.RefreshTokenRepository;
import com.eduquiz.feature.auth.repository.RoleRepository;
import com.eduquiz.feature.auth.repository.UserRepository;
import com.eduquiz.feature.email.service.EmailService;
import com.eduquiz.security.jwt.JwtUtil;
import com.eduquiz.feature.auth.entity.Role;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;

    @Autowired
    @Lazy
    private AuthenticationManager authenticationManager;

    @Value("${otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.max-attempts}")
    private int otpMaxAttempts;

    @Value("${otp.max-resend}")
    private int otpMaxResend;

    @Value("${otp.resend-cooldown-minutes}")
    private int otpResendCooldownMinutes;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ══════════════════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> register(RegisterRequest request) {
        log.info("[AuthService.register] START - email={}, role={}", request.getEmail(), request.getRole());

        Role role = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> {
                    log.warn("[AuthService.register] FAILED - role not found: {}", request.getRole());
                    return new BadRequestException(ResponseCode.ROLE_NOT_FOUND);
                });

        if (!role.isAllowRegistration()) {
            log.warn("[AuthService.register] FAILED - registration not allowed for role: {}", role.getName());
            throw new BadRequestException(ResponseCode.INSUFFICIENT_ROLE,
                    "Không thể tự đăng ký với vai trò " + role.getName());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("[AuthService.register] FAILED - email already exists: {}", request.getEmail());
            throw new DuplicateResourceException(ResponseCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dob(request.getDob())
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .isActive(false)
                .emailVerified(false)
                .build();
        userRepository.save(user);
        log.debug("[AuthService.register] User saved - email={}, userId={}", user.getEmail(), user.getId());

        sendAndSaveOtp(user, null,
                "EduQuiz - Xác thực đăng ký tài khoản",
                "Mã Xác Minh Đăng Ký Tài Khoản EduQuiz",
                "/auth/verify-otp?email=" + user.getEmail());

        log.info("[AuthService.register] SUCCESS - email={}, userId={}", user.getEmail(), user.getId());
        return ApiResponse.ok(ResponseCode.AUTH_REGISTER_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // VERIFY OTP
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> verifyOtp(VerifyOtpRequest request) {
        log.info("[AuthService.verifyOtp] START - email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[AuthService.verifyOtp] FAILED - user not found: {}", request.getEmail());
                    return new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND);
                });

        if (user.getEmailVerified()) {
            log.warn("[AuthService.verifyOtp] FAILED - email already verified: {}", request.getEmail());
            throw new DuplicateResourceException(ResponseCode.EMAIL_ALREADY_VERIFIED);
        }

        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> {
                    log.warn("[AuthService.verifyOtp] FAILED - no OTP record found for: {}", request.getEmail());
                    return new OtpVerificationException(ResponseCode.OTP_NOT_FOUND);
                });

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[AuthService.verifyOtp] FAILED - OTP expired for: {}, expiredAt={}", request.getEmail(), verification.getExpiresAt());
            throw new OtpVerificationException(ResponseCode.OTP_EXPIRED);
        }

        if (verification.getAttempts() >= otpMaxAttempts) {
            log.warn("[AuthService.verifyOtp] FAILED - max OTP attempts exceeded for: {}, attempts={}", request.getEmail(), verification.getAttempts());
            throw new OtpVerificationException(ResponseCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (!verification.getOtpCode().equals(request.getOtp())) {
            emailVerificationRepository.save(verification);
            int remaining = otpMaxAttempts - verification.getAttempts();
            log.warn("[AuthService.verifyOtp] FAILED - invalid OTP for: {}, attemptsUsed={}, remaining={}", request.getEmail(), verification.getAttempts(), remaining);
            throw new OtpVerificationException(ResponseCode.OTP_INVALID,
                    "Mã OTP không đúng. Còn " + remaining + " lần thử.");
        }

        verification.setVerified(true);
        emailVerificationRepository.save(verification);

        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("[AuthService.verifyOtp] SUCCESS - email={}, userId={}", request.getEmail(), user.getId());
        return ApiResponse.ok(ResponseCode.OTP_VERIFIED_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // RESEND OTP
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> resendOtp(String email) {
        log.info("[AuthService.resendOtp] START - email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[AuthService.resendOtp] FAILED - user not found: {}", email);
                    return new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND);
                });

        if (user.getEmailVerified()) {
            log.warn("[AuthService.resendOtp] FAILED - email already verified: {}", email);
            throw new DuplicateResourceException(ResponseCode.EMAIL_ALREADY_VERIFIED);
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(otpResendCooldownMinutes);
        int resentCount = emailVerificationRepository.countByUserAndCreatedAtAfter(user, since);

        if (resentCount >= otpMaxResend) {
            log.warn("[AuthService.resendOtp] FAILED - resend limit exceeded for: {}, resentCount={}, maxResend={}", email, resentCount, otpMaxResend);
            throw new OtpVerificationException(ResponseCode.OTP_RESEND_LIMIT_EXCEEDED);
        }

        String pendingPassword = null;
        Optional<EmailVerification> lastVerification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (lastVerification.isPresent()) {
            pendingPassword = lastVerification.get().getNewPassword();
        }

        String subject = "EduQuiz - Xác thực đăng ký tài khoản";
        String title = "Mã Xác Minh Đăng Ký Tài Khoản EduQuiz";
        String verifyPath = "/auth/verify-otp?email=" + user.getEmail();

        if (user.getEmailVerified()) {
            if (pendingPassword != null) {
                subject = "EduQuiz - Yêu cầu thay đổi mật khẩu";
                title = "Mã Xác Minh Thay Đổi Mật Khẩu EduQuiz";
                verifyPath = "/auth/password-change/confirm?email=" + user.getEmail();
            } else {
                subject = "EduQuiz - Yêu cầu đặt lại mật khẩu";
                title = "Mã Xác Minh Đặt Lại Mật Khẩu EduQuiz";
                verifyPath = "/auth/reset-password?email=" + user.getEmail();
            }
        }

        sendAndSaveOtp(user, pendingPassword, subject, title, verifyPath);

        log.info("[AuthService.resendOtp] SUCCESS - email={}, resentCount={}", email, resentCount + 1);
        return ApiResponse.ok(ResponseCode.OTP_RESENT_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        log.info("[AuthService.login] START - email={}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            log.warn("[AuthService.login] FAILED - account not verified: {}", request.getEmail());
            throw new BadRequestException(ResponseCode.AUTH_ACCOUNT_NOT_VERIFIED);
        } catch (LockedException e) {
            log.warn("[AuthService.login] FAILED - account inactive/locked: {}", request.getEmail());
            throw new BadRequestException(ResponseCode.AUTH_ACCOUNT_INACTIVE);
        } catch (BadCredentialsException e) {
            log.warn("[AuthService.login] FAILED - invalid credentials: {}", request.getEmail());
            throw new BadRequestException(ResponseCode.AUTH_INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("[AuthService.login] FAILED - user not found after auth success: {}", request.getEmail());
                    return new BadRequestException(ResponseCode.AUTH_INVALID_CREDENTIALS);
                });

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = createRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .build();

        log.info("[AuthService.login] SUCCESS - email={}, userId={}", request.getEmail(), user.getId());
        return ApiResponse.ok(ResponseCode.AUTH_LOGIN_SUCCESS, authResponse);
    }

    // ══════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request) {
        log.info("[AuthService.refreshToken] START");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    log.warn("[AuthService.refreshToken] FAILED - token not found in DB");
                    return new BadRequestException(ResponseCode.AUTH_REFRESH_TOKEN_INVALID);
                });

        if (refreshToken.getRevoked()) {
            log.warn("[AuthService.refreshToken] FAILED - token already revoked, userId={}", refreshToken.getUser().getId());
            throw new BadRequestException(ResponseCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.warn("[AuthService.refreshToken] FAILED - token expired, userId={}, expiredAt={}", refreshToken.getUser().getId(), refreshToken.getExpiryDate());
            throw new BadRequestException(ResponseCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtUtil.generateToken(user);

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        String newRefreshToken = createRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .build();

        log.info("[AuthService.refreshToken] SUCCESS - email={}, userId={}", user.getEmail(), user.getId());
        return ApiResponse.ok(ResponseCode.AUTH_REFRESH_SUCCESS, authResponse);
    }

    // ══════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> logout(User user) {
        log.info("[AuthService.logout] START - email={}, userId={}", user.getEmail(), user.getId());

        user.setTokenInvalidatedAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);

        log.info("[AuthService.logout] SUCCESS - email={}, userId={}, allTokensRevoked=true", user.getEmail(), user.getId());
        return ApiResponse.ok(ResponseCode.AUTH_LOGOUT_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // GET CURRENT USER
    // ══════════════════════════════════════════════════════

    public ApiResponse<UserInfoResponse> getCurrentUser(User user) {
        log.info("[AuthService.getCurrentUser] START - email={}, userId={}", user.getEmail(), user.getId());

        UserInfoResponse userInfo = UserInfoResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .build();

        log.info("[AuthService.getCurrentUser] SUCCESS - email={}, role={}", user.getEmail(), user.getRole().getName());
        return ApiResponse.ok(ResponseCode.AUTH_ME_SUCCESS, userInfo);
    }

    // ══════════════════════════════════════════════════════
    // FORGOT PASSWORD
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        log.info("[AuthService.forgotPassword] START - email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[AuthService.forgotPassword] FAILED - user not found: {}", request.getEmail());
                    return new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND);
                });

        if (!user.getIsActive()) {
            log.warn("[AuthService.forgotPassword] FAILED - account inactive: {}", request.getEmail());
            throw new BadRequestException(ResponseCode.AUTH_ACCOUNT_INACTIVE);
        }

        sendAndSaveOtp(user, null,
                "EduQuiz - Yêu cầu đặt lại mật khẩu",
                "Mã Xác Minh Đặt Lại Mật Khẩu EduQuiz",
                "/auth/reset-password?email=" + user.getEmail());

        log.info("[AuthService.forgotPassword] SUCCESS - OTP sent to: {}", request.getEmail());
        return ApiResponse.ok(ResponseCode.OTP_SENT_SUCCESS);
    }

    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        log.info("[AuthService.resetPassword] START - email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("[AuthService.resetPassword] FAILED - user not found: {}", request.getEmail());
                    return new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND);
                });

        verifyOtpInternal("resetPassword", user, request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenInvalidatedAt(LocalDateTime.now());
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);

        log.info("[AuthService.resetPassword] SUCCESS - email={}, userId={}, allSessionsInvalidated=true", request.getEmail(), user.getId());
        return ApiResponse.ok(ResponseCode.AUTH_PASSWORD_RESET_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // CHANGE PASSWORD (LOGGED IN)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> requestPasswordChange(User user, ChangePasswordInitRequest request) {
        log.info("[AuthService.requestPasswordChange] START - email={}, userId={}", user.getEmail(), user.getId());

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("[AuthService.requestPasswordChange] FAILED - incorrect current password: {}", user.getEmail());
            throw new BadRequestException(ResponseCode.AUTH_PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("[AuthService.requestPasswordChange] FAILED - new password same as old: {}", user.getEmail());
            throw new BadRequestException(ResponseCode.AUTH_NEW_PASSWORD_SAME_AS_OLD);
        }

        sendAndSaveOtp(user, passwordEncoder.encode(request.getNewPassword()),
                "EduQuiz - Yêu cầu thay đổi mật khẩu",
                "Mã Xác Minh Thay Đổi Mật Khẩu EduQuiz",
                "/auth/password-change/confirm");

        log.info("[AuthService.requestPasswordChange] SUCCESS - OTP sent to: {}", user.getEmail());
        return ApiResponse.ok(ResponseCode.OTP_SENT_SUCCESS);
    }

    @Transactional
    public ApiResponse<Void> changePassword(User user, ChangePasswordRequest request) {
        log.info("[AuthService.changePassword] START - email={}, userId={}", user.getEmail(), user.getId());

        verifyOtpInternal("changePassword", user, request.getOtp());

        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> {
                    log.error("[AuthService.changePassword] FAILED - no OTP record found after verify: {}", user.getEmail());
                    return new BadRequestException(ResponseCode.OTP_NOT_FOUND);
                });

        if (verification.getNewPassword() == null) {
            log.warn("[AuthService.changePassword] FAILED - no pending password in OTP record: {}", user.getEmail());
            throw new BadRequestException(ResponseCode.BAD_REQUEST, "Yêu cầu thay đổi mật khẩu không hợp lệ.");
        }

        user.setPassword(verification.getNewPassword());
        user.setTokenInvalidatedAt(LocalDateTime.now());
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);

        log.info("[AuthService.changePassword] SUCCESS - email={}, userId={}, allSessionsInvalidated=true", user.getEmail(), user.getId());
        return ApiResponse.ok(ResponseCode.AUTH_PASSWORD_CHANGED);
    }

    // ══════════════════════════════════════════════════════
    // UPDATE PROFILE
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<UserInfoResponse> updateProfile(User user, UpdateProfileRequest request) {
        log.info("[AuthService.updateProfile] START - email={}, userId={}", user.getEmail(), user.getId());

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        UserInfoResponse userInfo = UserInfoResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .build();

        log.info("[AuthService.updateProfile] SUCCESS - email={}, userId={}", user.getEmail(), user.getId());
        return ApiResponse.ok(ResponseCode.AUTH_PROFILE_UPDATED, userInfo);
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private void sendAndSaveOtp(User user, String pendingPassword, String subject, String title, String verifyPath) {
        log.debug("[AuthService.sendAndSaveOtp] START - email={}, subject={}", user.getEmail(), subject);

        String otp = otpGenerator.generateOtp();

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .verified(false)
                .attempts(0)
                .newPassword(pendingPassword)
                .build();
        emailVerificationRepository.save(verification);

        String fullUrl = frontendUrl + verifyPath;

        emailService.sendOtpEmail(user.getEmail(), subject, title, otp, user.getFullName(), fullUrl);
        log.info("[AuthService.sendAndSaveOtp] SUCCESS - email={}, expiresAt={}", user.getEmail(), verification.getExpiresAt());
    }

    private void verifyOtpInternal(String callerMethod, User user, String otpCode) {
        log.debug("[AuthService.verifyOtpInternal] START - caller={}, email={}", callerMethod, user.getEmail());

        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> {
                    log.warn("[AuthService.verifyOtpInternal] FAILED - caller={}, no OTP record: {}", callerMethod, user.getEmail());
                    return new OtpVerificationException(ResponseCode.OTP_NOT_FOUND);
                });

        if (verification.getVerified()) {
            log.warn("[AuthService.verifyOtpInternal] FAILED - caller={}, OTP already used: {}", callerMethod, user.getEmail());
            throw new OtpVerificationException(ResponseCode.OTP_ALREADY_USED);
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("[AuthService.verifyOtpInternal] FAILED - caller={}, OTP expired: {}, expiredAt={}", callerMethod, user.getEmail(), verification.getExpiresAt());
            throw new OtpVerificationException(ResponseCode.OTP_EXPIRED);
        }

        if (verification.getAttempts() >= otpMaxAttempts) {
            log.warn("[AuthService.verifyOtpInternal] FAILED - caller={}, max attempts exceeded: {}, attempts={}", callerMethod, user.getEmail(), verification.getAttempts());
            throw new OtpVerificationException(ResponseCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (!verification.getOtpCode().equals(otpCode)) {
            emailVerificationRepository.save(verification);
            log.warn("[AuthService.verifyOtpInternal] FAILED - caller={}, invalid OTP: {}, attemptsUsed={}", callerMethod, user.getEmail(), verification.getAttempts());
            throw new OtpVerificationException(ResponseCode.OTP_INVALID);
        }

        verification.setVerified(true);
        emailVerificationRepository.save(verification);
        log.info("[AuthService.verifyOtpInternal] SUCCESS - caller={}, email={}", callerMethod, user.getEmail());
    }

    private String createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        log.debug("[AuthService.createRefreshToken] Token created - email={}, expiresAt={}", user.getEmail(), refreshToken.getExpiryDate());
        return refreshToken.getToken();
    }
}
