package com.todolist.controller;

import com.todolist.dto.request.DeleteAccountRequest;
import com.todolist.dto.request.UpdateProfileRequest;
import com.todolist.dto.response.ApiError;
import com.todolist.dto.response.ProfileResponse;
import com.todolist.security.CustomUserDetails;
import com.todolist.service.DeleteAccountOtpService;
import com.todolist.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * USER và ADMIN đều dùng được, chỉ thao tác hồ sơ/tài khoản của chính mình.
 * userId luôn lấy từ JWT, KHÔNG nhận từ body/param.
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Hồ sơ cá nhân và tài khoản của chính mình (USER và ADMIN)")
public class ProfileController {

    private final ProfileService profileService;
    private final DeleteAccountOtpService deleteAccountOtpService;

    @Operation(summary = "Xem hồ sơ của tôi",
            description = "Trả thông tin tài khoản (chỉ đọc) và hồ sơ. Chưa cập nhật hồ sơ lần nào thì các trường hồ sơ là null.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/detail")
    public ResponseEntity<ProfileResponse> detail(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(profileService.getProfile(principal.getUserId()));
    }

    @Operation(summary = "Cập nhật hồ sơ của tôi",
            description = "Cập nhật từng phần: trường không gửi (null) giữ nguyên, trường gửi lên ghi đè, chuỗi rỗng \"\" xóa giá trị. Chưa có hồ sơ thì tạo mới.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/update-info")
    public ResponseEntity<ProfileResponse> updateInfo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(principal.getUserId(), request));
    }

    @Operation(summary = "Xóa tài khoản của chính mình bằng OTP",
            description = "Xin OTP trước bằng POST /api/auth/request-otp với purpose = DELETE_ACCOUNT. "
                    + "Tài khoản bị xóa là tài khoản trong JWT (không nhận userId từ client). "
                    + "OTP đúng thì xóa tài khoản, hồ sơ và toàn bộ Todo (không thể khôi phục).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã xóa tài khoản"),
            @ApiResponse(responseCode = "400", description = "OTP sai/hết hạn/sai quá số lần hoặc dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/delete-account")
    public ResponseEntity<Void> deleteAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody DeleteAccountRequest request) {
        Long userId = principal.getUserId();
        deleteAccountOtpService.confirmDelete(userId, userId, request);
        return ResponseEntity.noContent().build();
    }
}