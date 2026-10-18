package com.todolist.dto.request;

import com.todolist.enums.OtpPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body của POST /api/auth/request-otp. Người xin OTP luôn là user trong JWT, không nhận id từ client.
 */
@Data
public class RequestOtpRequest {

    @Schema(description = "Mục đích xin OTP", example = "CHANGE_PASSWORD",
            allowableValues = {"CHANGE_PASSWORD", "DELETE_ACCOUNT"})
    @NotNull(message = "purpose không được để trống (CHANGE_PASSWORD hoặc DELETE_ACCOUNT)")
    private OtpPurpose purpose;
}