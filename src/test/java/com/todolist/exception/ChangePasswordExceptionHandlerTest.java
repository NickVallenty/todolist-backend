package com.todolist.exception;

import com.todolist.dto.response.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordExceptionHandlerTest {

    private final OtpExceptionHandler handler = new OtpExceptionHandler();

    @Test
    void tooManyRequests_returns429WithRetryAfterHeader() {
        ResponseEntity<ApiError> response = handler.handleOtp(OtpException.tooManyRequests(42));

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("42");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(429);
    }

    @Test
    void invalidOtp_returns400WithoutRetryAfter() {
        ResponseEntity<ApiError> response = handler.handleOtp(OtpException.invalidOrExpired());

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getHeaders().containsKey("Retry-After")).isFalse();
    }

    @Test
    void mailUnavailable_returns503() {
        ResponseEntity<ApiError> response = handler.handleOtp(OtpException.mailUnavailable());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
    }
}
