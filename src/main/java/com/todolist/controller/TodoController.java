package com.todolist.controller;

import com.todolist.dto.request.CreateTodoRequest;
import com.todolist.dto.request.UpdateTodoRequest;
import com.todolist.dto.response.ApiError;
import com.todolist.dto.response.TodoResponse;
import com.todolist.enums.TodoStatus;
import com.todolist.security.CustomUserDetails;
import com.todolist.service.TodoService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Chỉ User được truy cập (đã enforce ở SecurityConfig: /api/user/todos/** -> hasRole("USER")).
 * userId luôn được lấy từ JWT/SecurityContext của người đang đăng nhập,
 * KHÔNG bao giờ lấy từ request body/param, để đảm bảo User chỉ thao tác Todo của chính mình.
 * Tên đường dẫn thể hiện rõ hành động: list / create / detail / update / delete.
 */
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
@Tag(name = "Todo", description = "User quản lý Todo của chính mình (role USER)")
public class TodoController {

    private final TodoService todoService;

    @Operation(summary = "Xem danh sách Todo của tôi (tìm kiếm, lọc, phân trang)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải USER",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/list")
    public ResponseEntity<Page<TodoResponse>> getTodos(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Parameter(description = "Tìm theo từ khóa trong title/description") @RequestParam(required = false) String keyword,
            @Parameter(description = "Lọc theo trạng thái") @RequestParam(required = false) TodoStatus status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(todoService.getTodos(principal.getUserId(), keyword, status, pageable));
    }

    @Operation(summary = "Tạo Todo mới")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải USER",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<TodoResponse> createTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateTodoRequest request) {
        TodoResponse response = todoService.createTodo(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Xem chi tiết một Todo của tôi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thành công"),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải USER",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Todo (hoặc Todo không thuộc về tôi)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/detail/{todoId}")
    public ResponseEntity<TodoResponse> getTodoById(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long todoId) {
        return ResponseEntity.ok(todoService.getTodoById(principal.getUserId(), todoId));
    }

    @Operation(summary = "Sửa Todo của tôi")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải USER",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Todo (hoặc Todo không thuộc về tôi)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/update/{todoId}")
    public ResponseEntity<TodoResponse> updateTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long todoId,
            @Valid @RequestBody UpdateTodoRequest request) {
        return ResponseEntity.ok(todoService.updateTodo(principal.getUserId(), todoId, request));
    }

    @Operation(summary = "Xóa Todo của tôi")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Đã xóa Todo"),
            @ApiResponse(responseCode = "401", description = "Thiếu/sai/hết hạn token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Không phải USER",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy Todo (hoặc Todo không thuộc về tôi)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/delete/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long todoId) {
        todoService.deleteTodo(principal.getUserId(), todoId);
        return ResponseEntity.noContent().build();
    }
}