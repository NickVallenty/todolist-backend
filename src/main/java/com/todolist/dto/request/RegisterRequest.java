package com.todolist.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request đăng ký tài khoản công khai.
 * Cố ý KHÔNG có field role/status: người dùng tự đăng ký luôn là USER + ACTIVE,
 * tránh việc ai đó tự đăng ký quyền ADMIN.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "Username chỉ được chứa chữ cái, số, dấu chấm, gạch dưới và gạch ngang")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, max = 100, message = "Password phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận password không được để trống")
    private String confirmPassword;

    @JsonIgnore
    @AssertTrue(message = "Password và xác nhận password không khớp")
    public boolean isPasswordMatching() {
        // Nếu một trong hai null thì để @NotBlank báo lỗi, tránh báo trùng lỗi
        return password == null || confirmPassword == null || password.equals(confirmPassword);
    }
}
