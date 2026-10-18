package com.todolist.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Lỗi nghiệp vụ của luồng đổi mật khẩu bằng OTP. Mang sẵn HTTP status để
 * ChangePasswordExceptionHandler trả đúng mã (400 / 429 / 503) mà không phải sửa GlobalExceptionHandler.
 */
@Getter
public class OtpException extends RuntimeException {

    private final HttpStatus status;
    private final Long retryAfterSeconds;

    private OtpException(HttpStatus status, String message, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static OtpException invalidOrExpired() {
        return new OtpException(HttpStatus.BAD_REQUEST,
                "OTP không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu OTP mới", null);
    }

    public static OtpException attemptsExceeded() {
        return new OtpException(HttpStatus.BAD_REQUEST,
                "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng yêu cầu OTP mới", null);
    }

    public static OtpException wrongOtp(int remainingAttempts) {
        String message = remainingAttempts > 0
                ? "OTP không đúng. Bạn còn " + remainingAttempts + " lần thử"
                : "OTP không đúng. Bạn đã hết lượt thử, vui lòng yêu cầu OTP mới";
        return new OtpException(HttpStatus.BAD_REQUEST, message, null);
    }

    public static OtpException samePassword() {
        return new OtpException(HttpStatus.BAD_REQUEST,
                "Password mới phải khác password hiện tại", null);
    }

    public static OtpException tooManyRequests(long retryAfterSeconds) {
        return new OtpException(HttpStatus.TOO_MANY_REQUESTS,
                "Bạn vừa yêu cầu OTP, vui lòng thử lại sau " + retryAfterSeconds + " giây", retryAfterSeconds);
    }

    public static OtpException mailUnavailable() {
        return new OtpException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không thể gửi email OTP lúc này. Vui lòng thử lại sau", null);
    }
}
