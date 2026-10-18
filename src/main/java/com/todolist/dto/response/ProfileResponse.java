package com.todolist.dto.response;

import com.todolist.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    @Schema(example = "1")                      private Long userId;
    @Schema(example = "nguyenvana")             private String username;
    @Schema(example = "nguyenvana@example.com") private String email;
    @Schema(example = "USER")                   private String role;
    @Schema(example = "Nguyễn Văn A")           private String fullName;
    @Schema(example = "0912345678")             private String phoneNumber;
    @Schema(example = "123 Lê Lợi, Quận 1")     private String address;
    @Schema(example = "2000-01-31")             private LocalDate dateOfBirth;
    @Schema(example = "MALE")                   private Gender gender;
    @Schema(example = "Xin chào!")              private String bio;
    @Schema(example = "https://example.com/avatar.png") private String avatarUrl;
    @Schema(example = "2026-10-17T10:15:30")    private LocalDateTime updatedDate;
}