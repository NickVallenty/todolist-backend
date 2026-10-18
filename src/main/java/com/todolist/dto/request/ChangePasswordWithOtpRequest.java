package com.todolist.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request bước 2 của luồng đổi mật khẩu bằng OTP.
 * Quy tắc password giữ nguyên như RegisterRequest / ResetPasswordRequest (6–100 ký tự).
 */
@Data
public class ChangePasswordWithOtpRequest {

    @NotBlank(message = "OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "OTP phải gồm đúng 6 chữ số")
    private String otp;

    @NotBlank(message = "Password mới không được để trống")
    @Size(min = 6, max = 100, message = "Password phải có ít nhất 6 ký tự")
    private String newPassword;

    @NotBlank(message = "Xác nhận password không được để trống")
    private String confirmPassword;

    @JsonIgnore
    @AssertTrue(message = "Password mới và xác nhận password không khớp")
    public boolean isPasswordMatching() {
        return newPassword == null || confirmPassword == null || newPassword.equals(confirmPassword);
    }
}
