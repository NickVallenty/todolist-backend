package com.todolist.exception;

import com.todolist.controller.AuthController;
import com.todolist.controller.ProfileController;
import com.todolist.dto.response.ApiError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Áp dụng cho AuthController (request-otp, change-password) và ProfileController (delete-account, update-info).
 * - OtpException: 400 / 429 (kèm Retry-After) / 503.
 * - HttpMessageNotReadableException: body thiếu hoặc sai kiểu/định dạng trả 400 thay vì rơi vào handler chung (500).
 * Mọi exception khác (validation, 403, 404, 409...) vẫn do GlobalExceptionHandler xử lý như cũ.
 */
@RestControllerAdvice(assignableTypes = {AuthController.class, ProfileController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OtpExceptionHandler {

    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ApiError> handleOtp(OtpException ex) {
        HttpStatus status = ex.getStatus();
        ApiError error = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                ex.getMessage(), null);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (ex.getRetryAfterSeconds() != null) {
            builder.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        }
        return builder.body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiError error = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                "Dữ liệu gửi lên không hợp lệ (thiếu body hoặc sai định dạng/giá trị)", null);
        return ResponseEntity.status(status).body(error);
    }
}