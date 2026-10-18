package com.todolist.controller;

import com.todolist.dto.request.CreateUserRequest;
import com.todolist.dto.request.ResetPasswordRequest;
import com.todolist.dto.request.UpdateUserRequest;
import com.todolist.dto.response.ApiError;
import com.todolist.dto.response.UserResponse;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import com.todolist.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Chỉ Admin được truy cập (đã enforce ở SecurityConfig: /api/admin/** -> hasRole("ADMIN")).
 * Tên đường dẫn thể hiện rõ hành động: list / create / detail / update / reset-password / delete.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Admin quản lý tài khoản người dùng (role ADMIN)")
public class UserController {

    private final AppUserService appUserService;

    @Operation(summary = "Admin tìm kiếm user theo username hoặc email",
            description = "Nhập một đoạn chữ vào keyword. Trả về các user có username hoặc email CHỨA đoạn chữ đó "
                    + "(không phân biệt hoa thường, tự bỏ khoảng trắng đầu/cuối). "
                    + "Để trống keyword để xem tất cả. Kết quả sắp xếp user mới nhất lên đầu.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công (danh sách rỗng nếu không có user khớp)"),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/list")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @Parameter(description = "Đoạn chữ cần tìm trong username hoặc email", example = "nguyenvan")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Số trang, bắt đầu từ 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số user mỗi trang (1 đến 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "userId"));
        return ResponseEntity.ok(appUserService.getUsers(keyword, pageable));
    }

    @Operation(summary = "Admin tạo user mới")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Username/email đã tồn tại",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appUserService.createUser(request));
    }

    @Operation(summary = "Admin xem chi tiết một user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/detail/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(appUserService.getUserById(userId));
    }

    @Operation(summary = "Admin sửa thông tin user (gồm đổi role, status)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Username/email đã tồn tại",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/update/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId,
                                                   @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(appUserService.updateUser(userId, request));
    }

    @Operation(summary = "Admin đặt lại mật khẩu cho user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã đặt lại mật khẩu"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/reset-password/{userId}")
    public ResponseEntity<Void> resetPassword(@PathVariable Long userId,
                                              @Valid @RequestBody ResetPasswordRequest request) {
        appUserService.resetPassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Admin xóa user (xóa luôn Todo của user đó)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã xóa user"),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy user",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        appUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}