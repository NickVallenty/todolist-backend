package com.todolist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @Schema(description = "Tên đăng nhập", example = "admin")
    @NotBlank(message = "Username không được để trống")
    private String username;

    @Schema(description = "Mật khẩu", example = "your-password")
    @NotBlank(message = "Password không được để trống")
    private String password;
}