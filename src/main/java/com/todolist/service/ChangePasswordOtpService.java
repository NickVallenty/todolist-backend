package com.todolist.service;

import com.todolist.dto.request.ChangePasswordWithOtpRequest;
import com.todolist.dto.response.OtpRequestedResponse;
import com.todolist.entity.AppUser;
import com.todolist.entity.PasswordOtp;
import com.todolist.exception.OtpException;
import com.todolist.exception.ResourceNotFoundException;
import com.todolist.repository.AppUserRepository;
import com.todolist.repository.PasswordOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Đổi mật khẩu bằng OTP cho user đang đăng nhập (userId luôn lấy từ JWT, không đọc từ request).
 * Bước 1 requestOtp: tạo OTP, lưu hash, gửi email.
 * Bước 2 confirm: kiểm tra OTP rồi đổi mật khẩu.
 */
@Service
@RequiredArgsConstructor
public class ChangePasswordOtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final PasswordOtpRepository passwordOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpMailService otpMailService;

    @Value("${app.otp.expiration-minutes:5}")
    private long expirationMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Transactional
    public OtpRequestedResponse requestOtp(Long userId) {
        AppUser user = findUserOrThrow(userId);
        LocalDateTime now = LocalDateTime.now();

        // Chống spam email: chỉ cho xin OTP mới sau khoảng cooldown
        passwordOtpRepository.findFirstByUserIdOrderByOtpIdDesc(userId).ifPresent(latest -> {
            LocalDateTime nextAllowed = latest.getCreatedDate().plusSeconds(resendCooldownSeconds);
            if (nextAllowed.isAfter(now)) {
                long wait = Math.max(1, Duration.between(now, nextAllowed).toSeconds());
                throw OtpException.tooManyRequests(wait);
            }
        });

        // OTP mới vô hiệu hóa OTP cũ
        passwordOtpRepository.deleteByUserId(userId);

        String otp = generateOtp();
        passwordOtpRepository.save(PasswordOtp.builder()
                .userId(userId)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(now.plusMinutes(expirationMinutes))
                .build());

        // Gửi mail lỗi -> exception -> rollback, không để lại OTP "mồ côi"
        otpMailService.sendOtp(user.getEmail(), user.getUsername(), otp, expirationMinutes);

        return new OtpRequestedResponse("Đã gửi OTP tới email của bạn",
                maskEmail(user.getEmail()), expirationMinutes * 60);
    }

    /**
     * noRollbackFor = OtpException: khi OTP sai vẫn phải lưu lại bộ đếm attempts.
     */
    @Transactional(noRollbackFor = OtpException.class)
    public void confirm(Long userId, ChangePasswordWithOtpRequest request) {
        AppUser user = findUserOrThrow(userId);

        PasswordOtp otp = passwordOtpRepository.findFirstByUserIdOrderByOtpIdDesc(userId)
                .orElseThrow(OtpException::invalidOrExpired);

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw OtpException.invalidOrExpired();
        }
        if (otp.getAttempts() >= maxAttempts) {
            throw OtpException.attemptsExceeded();
        }
        if (!passwordEncoder.matches(request.getOtp(), otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            passwordOtpRepository.save(otp);
            throw OtpException.wrongOtp(maxAttempts - otp.getAttempts());
        }

        // Chỉ kiểm tra sau khi OTP đúng, để không biến endpoint thành "oracle" dò password hiện tại
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw OtpException.samePassword();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        appUserRepository.save(user);
        passwordOtpRepository.delete(otp); // OTP chỉ dùng được một lần
    }

    private AppUser findUserOrThrow(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User với id: " + userId));
    }

    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return (local.length() <= 1 ? "*" : local.charAt(0) + "***") + domain;
    }
}
