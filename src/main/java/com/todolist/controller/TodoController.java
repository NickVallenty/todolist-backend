package com.todolist.controller;

import com.todolist.dto.request.CreateTodoRequest;
import com.todolist.dto.request.UpdateTodoRequest;
import com.todolist.dto.response.TodoResponse;
import com.todolist.enums.TodoStatus;
import com.todolist.security.CustomUserDetails;
import com.todolist.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Chỉ User được truy cập (đã enforce ở SecurityConfig: /api/todos/** -> hasRole("USER")).
 * userId luôn được lấy từ JWT/SecurityContext của người đang đăng nhập,
 * KHÔNG bao giờ lấy từ request body/param, để đảm bảo User chỉ thao tác Todo của chính mình.
 */
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public ResponseEntity<TodoResponse> createTodo(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @Valid @RequestBody CreateTodoRequest request) {
        TodoResponse response = todoService.createTodo(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TodoResponse>> getTodos(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) TodoStatus status,
                                                        Pageable pageable) {
        return ResponseEntity.ok(todoService.getTodos(principal.getUserId(), keyword, status, pageable));
    }

    @GetMapping("/{todoId}")
    public ResponseEntity<TodoResponse> getTodoById(@AuthenticationPrincipal CustomUserDetails principal,
                                                     @PathVariable Long todoId) {
        return ResponseEntity.ok(todoService.getTodoById(principal.getUserId(), todoId));
    }

    @PutMapping("/{todoId}")
    public ResponseEntity<TodoResponse> updateTodo(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @PathVariable Long todoId,
                                                    @Valid @RequestBody UpdateTodoRequest request) {
        return ResponseEntity.ok(todoService.updateTodo(principal.getUserId(), todoId, request));
    }

    @DeleteMapping("/{todoId}")
    public ResponseEntity<Void> deleteTodo(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable Long todoId) {
        todoService.deleteTodo(principal.getUserId(), todoId);
        return ResponseEntity.noContent().build();
    }
}
