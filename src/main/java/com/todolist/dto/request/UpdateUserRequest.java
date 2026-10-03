package com.todolist.dto.request;

import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 50)
    private String username;

    @Email(message = "Email không hợp lệ")
    @Size(max = 100)
    private String email;

    private Role role;

    private UserStatus status;
}
