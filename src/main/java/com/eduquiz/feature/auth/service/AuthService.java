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
import com.eduquiz.feature.auth.repository.RoleRepository;
import com.eduquiz.feature.auth.repository.UserRepository;
import com.eduquiz.feature.email.service.EmailService;
import com.eduquiz.security.jwt.JwtUtil;
import com.eduquiz.feature.auth.entity.Role;
import org.springframework.security.authentication.AuthenticationManager;
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

    // ══════════════════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> register(RegisterRequest request) {
        // Fetch role entity
        Role role = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ: " + request.getRole()));

        // Check if registration is allowed for this role
        if (!role.isAllowRegistration()) {
            throw new BadRequestException("Không thể tự đăng ký với vai trò " + role.getName() + ".");
        }

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã được đăng ký: " + request.getEmail());
        }

        // Create inactive user
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

        // Generate & save OTP
        sendAndSaveOtp(user, null,
                "EduQuiz - Xác thực đăng ký tài khoản",
                "Mã Xác Minh Đăng Ký Tài Khoản EduQuiz",
                "/auth/verify-otp?email=" + user.getEmail());

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

        // Copy pending password if resending for a password change request
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
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dob(user.getDob())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().getName())
                .build();
        return ApiResponse.ok(userInfo);
    }

    // ══════════════════════════════════════════════════════
    // FORGOT PASSWORD
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với email: " + request.getEmail()));

        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản chưa được kích hoạt.");
        }

        sendAndSaveOtp(user, null,
                "EduQuiz - Yêu cầu đặt lại mật khẩu",
                "Mã Xác Minh Đặt Lại Mật Khẩu EduQuiz",
                "/auth/reset-password?email=" + user.getEmail());
        log.info("Password reset OTP sent to: {}", request.getEmail());
        return ApiResponse.ok("Mã OTP đặt lại mật khẩu đã được gửi đến email của bạn.");
    }

    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user với email: " + request.getEmail()));

        verifyOtpInternal(user, request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenInvalidatedAt(LocalDateTime.now()); // Logout all sessions
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);

        log.info("Password reset successful for: {}", request.getEmail());
        return ApiResponse.ok("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");
    }

    // ══════════════════════════════════════════════════════
    // CHANGE PASSWORD (LOGGED IN)
    // ══════════════════════════════════════════════════════

    @Transactional
    public ApiResponse<Void> requestPasswordChange(User user, ChangePasswordInitRequest request) {
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu cũ không chính xác.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu cũ.");
        }

        sendAndSaveOtp(user, passwordEncoder.encode(request.getNewPassword()),
                "EduQuiz - Yêu cầu thay đổi mật khẩu",
                "Mã Xác Minh Thay Đổi Mật Khẩu EduQuiz",
                "/auth/password-change/confirm");
        log.info("Password change OTP sent to: {}", user.getEmail());
        return ApiResponse.ok("Mã OTP thay đổi mật khẩu đã được gửi đến email của bạn.");
    }

    @Transactional
    public ApiResponse<Void> changePassword(User user, ChangePasswordRequest request) {
        verifyOtpInternal(user, request.getOtp());

        // Lấy OTP mới nhất đã được verify (verifyOtpInternal đã mark verified = true)
        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy yêu cầu thay đổi mật khẩu."));

        if (verification.getNewPassword() == null) {
            throw new BadRequestException("Yêu cầu thay đổi mật khẩu không hợp lệ.");
        }

        user.setPassword(verification.getNewPassword());
        user.setTokenInvalidatedAt(LocalDateTime.now());
        refreshTokenRepository.deleteByUser(user);
        userRepository.save(user);

        log.info("Password changed successfully for: {}", user.getEmail());
        return ApiResponse.ok("Thay đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
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
        return getCurrentUser(user);
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

        // Construct full URL with OTP appended for auto-fill in frontend
        String separator = verifyPath.contains("?") ? "&" : "?";
        String fullUrl = "http://localhost:3000" + verifyPath + separator + "otp=" + otp;

        // Send email
        emailService.sendOtpEmail(user.getEmail(), subject, title, otp, user.getFullName(), fullUrl);
    }

    private void verifyOtpInternal(User user, String otpCode) {
        EmailVerification verification = emailVerificationRepository.findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new OtpVerificationException("Không tìm thấy mã OTP. Vui lòng yêu cầu gửi lại."));

        if (verification.getVerified()) {
            throw new BadRequestException("Mã OTP này đã được sử dụng.");
        }

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpVerificationException("Mã OTP đã hết hạn.");
        }

        if (verification.getAttempts() >= otpMaxAttempts) {
            throw new OtpVerificationException("Bạn đã nhập sai quá " + otpMaxAttempts + " lần.");
        }

        verification.setAttempts(verification.getAttempts() + 1);

        if (!verification.getOtpCode().equals(otpCode)) {
            emailVerificationRepository.save(verification);
            throw new OtpVerificationException("Mã OTP không đúng.");
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
