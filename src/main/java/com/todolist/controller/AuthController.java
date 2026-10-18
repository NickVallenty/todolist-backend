package com.todolist.controller;

import com.todolist.dto.request.ChangePasswordWithOtpRequest;
import com.todolist.dto.request.LoginRequest;
import com.todolist.dto.request.RegisterRequest;
import com.todolist.dto.request.RequestOtpRequest;
import com.todolist.dto.response.ApiError;
import com.todolist.dto.response.LoginResponse;
import com.todolist.dto.response.OtpRequestedResponse;
import com.todolist.dto.response.UserResponse;
import com.todolist.security.CustomUserDetails;
import com.todolist.service.AuthService;
import com.todolist.service.ChangePasswordOtpService;
import com.todolist.service.DeleteAccountOtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Nhóm Authentication, gồm 2 loại endpoint:
 * - Công khai (chưa đăng nhập): register, login (SecurityConfig chỉ permitAll 2 đường dẫn này).
 * - Cần đăng nhập (Bearer JWT, USER hoặc ADMIN): request-otp, change-password, delete-account.
 *   Người thực hiện luôn lấy từ JWT, không bao giờ từ body/param.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập và các thao tác cần xác thực OTP (đổi mật khẩu, xóa tài khoản)")
public class AuthController {

    private final AuthService authService;
    private final ChangePasswordOtpService changePasswordOtpService;
    private final DeleteAccountOtpService deleteAccountOtpService;

    // ------------------------------------------------------------------ Công khai

    @Operation(summary = "Đăng ký tài khoản USER mới", description = "Công khai, không cần token.")
    @SecurityRequirements   // endpoint công khai: gỡ yêu cầu JWT trên Swagger
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Username/email đã tồn tại",
                    content = @Content(schema = @Schema(implementation = ApiError.class)    ))
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Đăng nhập, nhận JWT", description = "Công khai, không cần token.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Sai username hoặc password",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ------------------------------------------------------------------ Cần đăng nhập + OTP

    @Operation(summary = "Xin OTP qua email (đổi mật khẩu hoặc xóa tài khoản)",
            description = "Gửi OTP 6 số tới email của tài khoản đang đăng nhập (lấy từ JWT). "
                    + "Chọn mục đích bằng `purpose`: CHANGE_PASSWORD hoặc DELETE_ACCOUNT. "
                    + "OTP hết hạn sau 5 phút, dùng một lần, và chỉ dùng được cho đúng mục đích đã xin. "
                    + "Cách nhau tối thiểu 60 giây giữa 2 lần xin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã gửi OTP tới email"),
            @ApiResponse(responseCode = "400", description = "purpose thiếu hoặc không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Xin OTP quá nhanh (kèm header Retry-After)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "503", description = "Không gửi được email OTP",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/request-otp")
    public ResponseEntity<OtpRequestedResponse> requestOtp(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody RequestOtpRequest request) {
        OtpRequestedResponse response = switch (request.getPurpose()) {
            case CHANGE_PASSWORD -> changePasswordOtpService.requestOtp(principal.getUserId());
            case DELETE_ACCOUNT -> deleteAccountOtpService.requestOtp(principal.getUserId());
        };
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đổi mật khẩu của chính mình bằng OTP",
            description = "Cần OTP đã xin với purpose = CHANGE_PASSWORD. OTP đúng thì đổi mật khẩu.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã đổi mật khẩu"),
            @ApiResponse(responseCode = "400", description = "OTP sai/hết hạn/sai quá số lần hoặc dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordWithOtpRequest request) {
        changePasswordOtpService.confirm(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }

}