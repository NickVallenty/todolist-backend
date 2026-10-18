package com.todolist.service;

import com.todolist.dto.request.ChangePasswordWithOtpRequest;
import com.todolist.dto.response.OtpRequestedResponse;
import com.todolist.entity.AppUser;
import com.todolist.entity.PasswordOtp;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import com.todolist.exception.OtpException;
import com.todolist.repository.AppUserRepository;
import com.todolist.repository.PasswordOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangePasswordOtpServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private PasswordOtpRepository passwordOtpRepository;
    @Mock
    private OtpMailService otpMailService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private ChangePasswordOtpService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        service = new ChangePasswordOtpService(appUserRepository, passwordOtpRepository, encoder, otpMailService);
        ReflectionTestUtils.setField(service, "expirationMinutes", 5L);
        ReflectionTestUtils.setField(service, "maxAttempts", 3);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);

        user = AppUser.builder()
                .userId(USER_ID)
                .username("alice")
                .email("alice@example.com")
                .password(encoder.encode("OldPass1"))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ------------------------------------------------------------------ requestOtp

    @Test
    void requestOtp_success_savesHashedOtpAndSendsMail() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordOtpRepository.findFirstByUserIdOrderByOtpIdDesc(USER_ID)).thenReturn(Optional.empty());

        OtpRequestedResponse response = service.requestOtp(USER_ID);

        ArgumentCaptor<String> sentOtp = ArgumentCaptor.forClass(String.class);
        verify(otpMailService).sendOtp(eq("alice@example.com"), eq("alice"), sentOtp.capture(), eq(5L));
        ArgumentCaptor<PasswordOtp> saved = ArgumentCaptor.forClass(PasswordOtp.class);
        verify(passwordOtpRepository).save(saved.capture());
        verify(passwordOtpRepository).deleteByUserId(USER_ID);

        assertThat(sentOtp.getValue()).matches("\\d{6}");
        assertThat(saved.getValue().getOtpHash()).isNotEqualTo(sentOtp.getValue());
        assertThat(encoder.matches(sentOtp.getValue(), saved.getValue().getOtpHash())).isTrue();
        assertThat(saved.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getValue().getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(4));
        assertThat(response.getMaskedEmail()).isEqualTo("a***@example.com");
        assertThat(response.getExpiresInSeconds()).isEqualTo(300L);
    }

    @Test
    void requestOtp_withinCooldown_throwsTooManyRequests() {
        PasswordOtp latest = otp("123456", 5, 0);
        latest.setCreatedDate(LocalDateTime.now().minusSeconds(10));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordOtpRepository.findFirstByUserIdOrderByOtpIdDesc(USER_ID)).thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.requestOtp(USER_ID))
                .isInstanceOfSatisfying(OtpException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getRetryAfterSeconds()).isBetween(1L, 60L);
                });

        verify(otpMailService, never()).sendOtp(any(), any(), any(), anyLong());
        verify(passwordOtpRepository, never()).save(any());
    }

    @Test
    void requestOtp_afterCooldown_replacesOldOtp() {
        PasswordOtp latest = otp("123456", 5, 0);
        latest.setCreatedDate(LocalDateTime.now().minusSeconds(61));
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordOtpRepository.findFirstByUserIdOrderByOtpIdDesc(USER_ID)).thenReturn(Optional.of(latest));

        service.requestOtp(USER_ID);

        verify(passwordOtpRepository).deleteByUserId(USER_ID);
        verify(passwordOtpRepository).save(any(PasswordOtp.class));
        verify(otpMailService).sendOtp(any(), any(), any(), anyLong());
    }

    // ------------------------------------------------------------------ confirm

    @Test
    void confirm_success_changesPasswordAndConsumesOtp() {
        PasswordOtp otp = otp("123456", 5, 0);
        stubUserAndOtp(otp);

        service.confirm(USER_ID, request("123456", "NewPass1"));

        assertThat(encoder.matches("NewPass1", user.getPassword())).isTrue();
        verify(appUserRepository).save(user);
        verify(passwordOtpRepository).delete(otp);
    }

    @Test
    void confirm_wrongOtp_incrementsAttemptsAndKeepsPassword() {
        PasswordOtp otp = otp("123456", 5, 0);
        stubUserAndOtp(otp);

        assertThatThrownBy(() -> service.confirm(USER_ID, request("000000", "NewPass1")))
                .isInstanceOfSatisfying(OtpException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).contains("2 lần");
                });

        assertThat(otp.getAttempts()).isEqualTo(1);
        verify(passwordOtpRepository).save(otp);
        verify(appUserRepository, never()).save(any());
        assertThat(encoder.matches("OldPass1", user.getPassword())).isTrue();
    }

    @Test
    void confirm_expiredOtp_rejected() {
        PasswordOtp otp = otp("123456", -1, 0);
        stubUserAndOtp(otp);

        assertThatThrownBy(() -> service.confirm(USER_ID, request("123456", "NewPass1")))
                .isInstanceOfSatisfying(OtpException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(appUserRepository, never()).save(any());
        verify(passwordOtpRepository, never()).delete(any());
    }

    @Test
    void confirm_attemptsExceeded_rejectedEvenWithCorrectOtp() {
        PasswordOtp otp = otp("123456", 5, 3);
        stubUserAndOtp(otp);

        assertThatThrownBy(() -> service.confirm(USER_ID, request("123456", "NewPass1")))
                .isInstanceOfSatisfying(OtpException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(appUserRepository, never()).save(any());
        assertThat(encoder.matches("OldPass1", user.getPassword())).isTrue();
    }

    @Test
    void confirm_noOtpRequested_rejected() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordOtpRepository.findFirstByUserIdOrderByOtpIdDesc(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(USER_ID, request("123456", "NewPass1")))
                .isInstanceOfSatisfying(OtpException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void confirm_newPasswordSameAsCurrent_rejectedAndOtpKept() {
        PasswordOtp otp = otp("123456", 5, 0);
        stubUserAndOtp(otp);

        assertThatThrownBy(() -> service.confirm(USER_ID, request("123456", "OldPass1")))
                .isInstanceOfSatisfying(OtpException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(appUserRepository, never()).save(any());
        verify(passwordOtpRepository, never()).delete(any());
    }

    @Test
    void maskEmail_masksLocalPart() {
        assertThat(ChangePasswordOtpService.maskEmail("alice@example.com")).isEqualTo("a***@example.com");
        assertThat(ChangePasswordOtpService.maskEmail("a@example.com")).isEqualTo("*@example.com");
    }

    // ------------------------------------------------------------------ helpers

    private void stubUserAndOtp(PasswordOtp otp) {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordOtpRepository.findFirstByUserIdOrderByOtpIdDesc(USER_ID)).thenReturn(Optional.of(otp));
    }

    private PasswordOtp otp(String plainOtp, long minutesUntilExpiry, int attempts) {
        return PasswordOtp.builder()
                .otpId(10L)
                .userId(USER_ID)
                .otpHash(encoder.encode(plainOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(minutesUntilExpiry))
                .attempts(attempts)
                .build();
    }

    private ChangePasswordWithOtpRequest request(String otp, String newPassword) {
        ChangePasswordWithOtpRequest request = new ChangePasswordWithOtpRequest();
        request.setOtp(otp);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(newPassword);
        return request;
    }
}
