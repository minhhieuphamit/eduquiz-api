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
        Role role = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new BadRequestException(ResponseCode.ROLE_NOT_FOUND));

        if (!role.isAllowRegistration()) {
            throw new BadRequestException(ResponseCode.INSUFFICIENT_ROLE,
                    "Không thể tự đăng ký với vai trò " + role.getName());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
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

        sendAndSaveOtp(user, null,
                "EduQuiz - Xác thực đăng ký tài khoản",
                "Mã Xác Minh Đăng Ký Tài Khoản EduQuiz",
                "/auth/verify-otp?email=" + user.getEmail());

        log.info("User registered: {}", request.getEmail());
        return ApiResponse.ok(ResponseCode.AUTH_REGISTER_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // VERIFY OTP
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND));

        if (user.getEmailVerified()) {
            throw new DuplicateResourceException(ResponseCode.EMAIL_ALREADY_VERIFIED);
        }

        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new OtpVerificationException(ResponseCode.OTP_NOT_FOUND));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpVerificationException(ResponseCode.OTP_EXPIRED);
        }

        if (verification.getAttempts() >= otpMaxAttempts) {
            throw new OtpVerificationException(ResponseCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (!verification.getOtpCode().equals(request.getOtp())) {
            emailVerificationRepository.save(verification);
            throw new OtpVerificationException(ResponseCode.OTP_INVALID,
                    "Mã OTP không đúng. Còn " + (otpMaxAttempts - verification.getAttempts()) + " lần thử.");
        }

        verification.setVerified(true);
        emailVerificationRepository.save(verification);

        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Email verified: {}", request.getEmail());
        return ApiResponse.ok(ResponseCode.OTP_VERIFIED_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // RESEND OTP
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND));

        if (user.getEmailVerified()) {
            throw new DuplicateResourceException(ResponseCode.EMAIL_ALREADY_VERIFIED);
        }

        LocalDateTime since = LocalDateTime.now().minusMinutes(otpResendCooldownMinutes);
        int resentCount = emailVerificationRepository.countByUserAndCreatedAtAfter(user, since);

        if (resentCount >= otpMaxResend) {
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

        log.info("OTP resent to: {}", email);
        return ApiResponse.ok(ResponseCode.OTP_RESENT_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            throw new BadRequestException(ResponseCode.AUTH_ACCOUNT_NOT_VERIFIED);
        } catch (LockedException e) {
            throw new BadRequestException(ResponseCode.AUTH_ACCOUNT_INACTIVE);
        } catch (BadCredentialsException e) {
            throw new BadRequestException(ResponseCode.AUTH_INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException(ResponseCode.AUTH_INVALID_CREDENTIALS));

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = createRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .build();

        log.info("User logged in: {}", request.getEmail());
        return ApiResponse.ok(ResponseCode.AUTH_LOGIN_SUCCESS, authResponse);
    }

    // ══════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException(ResponseCode.AUTH_REFRESH_TOKEN_INVALID));

        if (refreshToken.getRevoked()) {
            throw new BadRequestException(ResponseCode.AUTH_REFRESH_TOKEN_INVALID);
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
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

        return ApiResponse.ok(ResponseCode.AUTH_REFRESH_SUCCESS, authResponse);
    }

    // ══════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> logout(User user) {
        user.setTokenInvalidatedAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);

        log.info("User logged out: {}", user.getEmail());
        return ApiResponse.ok(ResponseCode.AUTH_LOGOUT_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // GET CURRENT USER
    // ══════════════════════════════════════════════════════

    public ApiResponse<UserInfoResponse> getCurrentUser(User user) {
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .build();
        return ApiResponse.ok(ResponseCode.AUTH_ME_SUCCESS, userInfo);
    }

    // ══════════════════════════════════════════════════════
    // FORGOT PASSWORD
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND));

        if (!user.getIsActive()) {
            throw new BadRequestException(ResponseCode.AUTH_ACCOUNT_INACTIVE);
        }

        sendAndSaveOtp(user, null,
                "EduQuiz - Yêu cầu đặt lại mật khẩu",
                "Mã Xác Minh Đặt Lại Mật Khẩu EduQuiz",
                "/auth/reset-password?email=" + user.getEmail());
        log.info("Password reset OTP sent to: {}", request.getEmail());
        return ApiResponse.ok(ResponseCode.OTP_SENT_SUCCESS);
    }

    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(ResponseCode.USER_NOT_FOUND));

        verifyOtpInternal(user, request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenInvalidatedAt(LocalDateTime.now());
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);

        log.info("Password reset successful for: {}", request.getEmail());
        return ApiResponse.ok(ResponseCode.AUTH_PASSWORD_RESET_SUCCESS);
    }

    // ══════════════════════════════════════════════════════
    // CHANGE PASSWORD (LOGGED IN)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> requestPasswordChange(User user, ChangePasswordInitRequest request) {
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException(ResponseCode.AUTH_PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException(ResponseCode.AUTH_NEW_PASSWORD_SAME_AS_OLD);
        }

        sendAndSaveOtp(user, passwordEncoder.encode(request.getNewPassword()),
                "EduQuiz - Yêu cầu thay đổi mật khẩu",
                "Mã Xác Minh Thay Đổi Mật Khẩu EduQuiz",
                "/auth/password-change/confirm");
        log.info("Password change OTP sent to: {}", user.getEmail());
        return ApiResponse.ok(ResponseCode.OTP_SENT_SUCCESS);
    }

    @Transactional
    public ApiResponse<Void> changePassword(User user, ChangePasswordRequest request) {
        verifyOtpInternal(user, request.getOtp());

        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new BadRequestException(ResponseCode.OTP_NOT_FOUND));

        if (verification.getNewPassword() == null) {
            throw new BadRequestException(ResponseCode.BAD_REQUEST, "Yêu cầu thay đổi mật khẩu không hợp lệ.");
        }

        user.setPassword(verification.getNewPassword());
        user.setTokenInvalidatedAt(LocalDateTime.now());
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);

        log.info("Password changed successfully for: {}", user.getEmail());
        return ApiResponse.ok(ResponseCode.AUTH_PASSWORD_CHANGED);
    }

    // ══════════════════════════════════════════════════════
    // UPDATE PROFILE
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<UserInfoResponse> updateProfile(User user, UpdateProfileRequest request) {
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Profile updated for: {}", user.getEmail());

        UserInfoResponse userInfo = UserInfoResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .build();
        return ApiResponse.ok(ResponseCode.AUTH_PROFILE_UPDATED, userInfo);
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private void sendAndSaveOtp(User user, String pendingPassword, String subject, String title, String verifyPath) {
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
    }

    private void verifyOtpInternal(User user, String otpCode) {
        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new OtpVerificationException(ResponseCode.OTP_NOT_FOUND));

        if (verification.getVerified()) {
            throw new OtpVerificationException(ResponseCode.OTP_ALREADY_USED);
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpVerificationException(ResponseCode.OTP_EXPIRED);
        }

        if (verification.getAttempts() >= otpMaxAttempts) {
            throw new OtpVerificationException(ResponseCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (!verification.getOtpCode().equals(otpCode)) {
            emailVerificationRepository.save(verification);
            throw new OtpVerificationException(ResponseCode.OTP_INVALID);
        }

        verification.setVerified(true);
        emailVerificationRepository.save(verification);
    }

    private String createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }
}
