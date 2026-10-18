package com.todolist.service;

import com.todolist.dto.request.DeleteAccountRequest;
import com.todolist.dto.response.OtpRequestedResponse;
import com.todolist.entity.AccountDeletionOtp;
import com.todolist.entity.AppUser;
import com.todolist.exception.OtpException;
import com.todolist.exception.ResourceNotFoundException;
import com.todolist.repository.AccountDeletionOtpRepository;
import com.todolist.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeleteAccountOtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final AccountDeletionOtpRepository accountDeletionOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpMailService otpMailService;

    @Value("${app.otp.expiration-minutes:5}")
    private long expirationMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Transactional
    public OtpRequestedResponse requestOtp(Long currentUserId) {
        AppUser user = findUserOrThrow(currentUserId);
        LocalDateTime now = LocalDateTime.now();

        accountDeletionOtpRepository.findFirstByUserIdOrderByOtpIdDesc(currentUserId).ifPresent(latest -> {
            LocalDateTime nextAllowed = latest.getCreatedDate().plusSeconds(resendCooldownSeconds);
            if (nextAllowed.isAfter(now)) {
                long wait = Math.max(1, Duration.between(now, nextAllowed).toSeconds());
                throw OtpException.tooManyRequests(wait);
            }
        });

        accountDeletionOtpRepository.deleteByUserId(currentUserId);

        String otp = generateOtp();
        accountDeletionOtpRepository.save(AccountDeletionOtp.builder()
                .userId(currentUserId)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(now.plusMinutes(expirationMinutes))
                .build());

        otpMailService.sendDeleteAccountOtp(user.getEmail(), user.getUsername(), otp, expirationMinutes);

        return new OtpRequestedResponse("Đã gửi OTP xác nhận xóa tài khoản tới email của bạn",
                ChangePasswordOtpService.maskEmail(user.getEmail()), expirationMinutes * 60);
    }

    @Transactional(noRollbackFor = OtpException.class)
    public void confirmDelete(Long currentUserId, Long targetUserId, DeleteAccountRequest request) {
        // Chỉ được xóa tài khoản của chính mình (kiểm tra trước khi đụng tới OTP)
        if (currentUserId == null || !currentUserId.equals(targetUserId)) {
            throw new AccessDeniedException("Chỉ được xóa tài khoản của chính mình");
        }

        AppUser user = findUserOrThrow(currentUserId);

        AccountDeletionOtp otp = accountDeletionOtpRepository.findFirstByUserIdOrderByOtpIdDesc(currentUserId)
                .orElseThrow(OtpException::invalidOrExpired);

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw OtpException.invalidOrExpired();
        }
        if (otp.getAttempts() >= maxAttempts) {
            throw OtpException.attemptsExceeded();
        }
        if (!passwordEncoder.matches(request.getOtp(), otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            accountDeletionOtpRepository.save(otp);
            throw OtpException.wrongOtp(maxAttempts - otp.getAttempts());
        }

        accountDeletionOtpRepository.delete(otp); // OTP chỉ dùng một lần
        appUserRepository.delete(user);           // cascade xóa luôn Todo của user
    }

    private AppUser findUserOrThrow(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User với id: " + userId));
    }

    private String generateOtp() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}