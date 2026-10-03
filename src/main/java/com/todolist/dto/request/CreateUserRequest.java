package com.todolist.dto.request;

import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(max = 50)
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, max = 100, message = "Password phải có ít nhất 6 ký tự")
    private String password;

    @NotNull(message = "Role không được để trống")
    private Role role;

    private UserStatus status; // optional, mặc định ACTIVE nếu không truyền
}
