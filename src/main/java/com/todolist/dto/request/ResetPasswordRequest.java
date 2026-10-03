package com.todolist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Size(min = 6, max = 100, message = "Password phải có ít nhất 6 ký tự")
    private String newPassword;
}
