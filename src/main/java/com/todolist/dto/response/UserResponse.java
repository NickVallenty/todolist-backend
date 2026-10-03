package com.todolist.dto.response;

import com.todolist.entity.AppUser;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdDate;

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedDate()
        );
        // Password field is intentionally never included.
    }
}
