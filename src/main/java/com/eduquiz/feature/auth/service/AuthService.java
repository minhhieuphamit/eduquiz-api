package com.eduquiz.feature.auth.service;

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
import com.eduquiz.feature.auth.repository.UserRepository;
import com.eduquiz.feature.email.service.EmailService;
import com.eduquiz.security.jwt.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final OtpGenerator otpGenerator;
    private final AuthenticationManager authenticationManager;

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

    // ══════════════════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> register(RegisterRequest request) {
        // Block ADMIN role – chỉ admin hiện tại mới được tạo admin khác
        if (request.getRole() == com.eduquiz.feature.auth.entity.Role.ADMIN) {
            throw new BadRequestException("Không thể tự đăng ký với vai trò ADMIN.");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã được đăng ký: " + request.getEmail());
        }

        // Create inactive user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .isActive(false)
                .emailVerified(false)
                .build();
        userRepository.save(user);

        // Generate & save OTP
        sendAndSaveOtp(user);

        log.info("User registered: {}", request.getEmail());
        return ApiResponse.ok("Đăng ký thành công! Vui lòng kiểm tra email để xác thực OTP.");
    }

    // ══════════════════════════════════════════════════════
    // VERIFY OTP
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với email: " + request.getEmail()));

        if (user.getEmailVerified()) {
            throw new BadRequestException("Email đã được xác thực trước đó.");
        }

        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new OtpVerificationException("Không tìm thấy mã OTP. Vui lòng yêu cầu gửi lại."));

        // Check expired
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpVerificationException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        // Check max attempts
        if (verification.getAttempts() >= otpMaxAttempts) {
            throw new OtpVerificationException("Bạn đã nhập sai quá " + otpMaxAttempts + " lần. Vui lòng yêu cầu gửi lại OTP.");
        }

        // Increment attempts
        verification.setAttempts(verification.getAttempts() + 1);

        // Verify OTP
        if (!verification.getOtpCode().equals(request.getOtp())) {
            emailVerificationRepository.save(verification);
            throw new OtpVerificationException("Mã OTP không đúng. Còn " + (otpMaxAttempts - verification.getAttempts()) + " lần thử.");
        }

        // Mark verified
        verification.setVerified(true);
        emailVerificationRepository.save(verification);

        // Activate user
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Email verified: {}", request.getEmail());
        return ApiResponse.ok("Xác thực email thành công! Bạn có thể đăng nhập.");
    }

    // ══════════════════════════════════════════════════════
    // RESEND OTP
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với email: " + email));

        if (user.getEmailVerified()) {
            throw new BadRequestException("Email đã được xác thực trước đó.");
        }

        // Check resend limit (max N times within cooldown window)
        LocalDateTime since = LocalDateTime.now().minusMinutes(otpResendCooldownMinutes);
        int resentCount = emailVerificationRepository.countByUserAndCreatedAtAfter(user, since);

        if (resentCount >= otpMaxResend) {
            throw new BadRequestException(
                    "Bạn đã gửi lại OTP " + otpMaxResend + " lần trong " + otpResendCooldownMinutes + " phút. Vui lòng đợi.");
        }

        sendAndSaveOtp(user);

        log.info("OTP resent to: {}", email);
        return ApiResponse.ok("Mã OTP mới đã được gửi đến email của bạn.");
    }

    // ══════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<AuthResponse> login(LoginRequest request) {
        // Authenticate qua Spring Security pipeline
        // Tự động check: password, isEnabled, isAccountNonLocked, isCredentialsNonExpired
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new BadRequestException("Email chưa được xác thực. Vui lòng kiểm tra email và nhập mã OTP.");
        } catch (org.springframework.security.authentication.LockedException e) {
            throw new BadRequestException("Tài khoản đã bị khóa. Vui lòng liên hệ admin.");
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new BadRequestException("Email hoặc mật khẩu không đúng.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không đúng."));

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = createRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .build();

        log.info("User logged in: {}", request.getEmail());
        return ApiResponse.ok("Đăng nhập thành công!", authResponse);
    }

    // ══════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<AuthResponse> refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ."));

        // Check revoked
        if (refreshToken.getRevoked()) {
            throw new BadRequestException("Refresh token đã bị thu hồi.");
        }

        // Check expired
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BadRequestException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User user = refreshToken.getUser();

        // Generate new access token
        String newAccessToken = jwtUtil.generateToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken()) // giữ nguyên refresh token
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .build();

        return ApiResponse.ok("Token đã được làm mới.", authResponse);
    }

    // ══════════════════════════════════════════════════════
    // LOGOUT
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> logout(User user) {
        // Invalidate tất cả access token đã phát hành trước thời điểm này
        user.setTokenInvalidatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Xóa tất cả refresh token
        refreshTokenRepository.deleteByUser(user);

        log.info("User logged out: {}", user.getEmail());
        return ApiResponse.ok("Đăng xuất thành công.");
    }

    // ══════════════════════════════════════════════════════
    // GET CURRENT USER
    // ══════════════════════════════════════════════════════

    public ApiResponse<UserInfoResponse> getCurrentUser(User user) {
        UserInfoResponse userInfo = UserInfoResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
        return ApiResponse.ok(userInfo);
    }

    // ══════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════

    private void sendAndSaveOtp(User user) {
        String otp = otpGenerator.generateOtp();

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .verified(false)
                .attempts(0)
                .build();
        emailVerificationRepository.save(verification);

        // Send email (async nếu cần tối ưu sau)
        emailService.sendOtpEmail(user.getEmail(), otp, user.getFullName());
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
