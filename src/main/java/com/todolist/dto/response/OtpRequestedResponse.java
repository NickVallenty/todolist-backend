package com.todolist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpRequestedResponse {
    private String message;
    private String maskedEmail;
    private long expiresInSeconds;
}
