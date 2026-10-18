package com.todolist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** Tất cả trường đều tùy chọn. null = giữ nguyên, "" = xóa. Cố ý KHÔNG có userId/username/email/role/status. */
@Data
public class UpdateProfileRequest {

    @Schema(example = "Nguyễn Văn A")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @Schema(example = "0912345678")
    @Pattern(regexp = "^$|^(\\+84|0)\\d{9,10}$", message = "Số điện thoại không hợp lệ (định dạng Việt Nam)")
    private String phoneNumber;

    @Schema(example = "123 Lê Lợi, Quận 1, TP.HCM")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @Schema(example = "2000-01-31")
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @Schema(example = "MALE", allowableValues = {"MALE", "FEMALE", "OTHER"})
    @Pattern(regexp = "^$|^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
    private String gender;

    @Schema(example = "Xin chào, mình là sinh viên.")
    @Size(max = 500, message = "Giới thiệu tối đa 500 ký tự")
    private String bio;

    @Schema(example = "https://example.com/avatar.png")
    @Size(max = 500, message = "Link ảnh tối đa 500 ký tự")
    @Pattern(regexp = "^$|^https?://.+", message = "Link ảnh phải bắt đầu bằng http:// hoặc https://")
    private String avatarUrl;
}