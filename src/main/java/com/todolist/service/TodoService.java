package com.todolist.service;

import com.todolist.dto.request.CreateTodoRequest;
import com.todolist.dto.request.UpdateTodoRequest;
import com.todolist.dto.response.TodoResponse;
import com.todolist.enums.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Nghiệp vụ CRUD Todo, luôn giới hạn thao tác trong phạm vi Todo thuộc về
 * userId được truyền vào (lấy từ JWT của người đang đăng nhập) — user không
 * bao giờ được đọc/sửa/xóa Todo của user khác.
 */
public interface TodoService {

    TodoResponse createTodo(Long userId, CreateTodoRequest request);

    Page<TodoResponse> getTodos(Long userId, String keyword, TodoStatus status, Pageable pageable);

    TodoResponse getTodoById(Long userId, Long todoId);

    TodoResponse updateTodo(Long userId, Long todoId, UpdateTodoRequest request);

    void deleteTodo(Long userId, Long todoId);
}
