package com.todolist.service;

import com.todolist.dto.request.CreateTodoRequest;
import com.todolist.dto.request.UpdateTodoRequest;
import com.todolist.dto.response.TodoResponse;
import com.todolist.entity.AppUser;
import com.todolist.entity.Todo;
import com.todolist.enums.TodoStatus;
import com.todolist.exception.ResourceNotFoundException;
import com.todolist.repository.AppUserRepository;
import com.todolist.repository.TodoRepository;
import com.todolist.repository.TodoSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public TodoResponse createTodo(Long userId, CreateTodoRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User với id: " + userId));

        Todo todo = Todo.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TodoStatus.PENDING)
                .build();

        return TodoResponse.from(todoRepository.save(todo));
    }

    @Override
    public Page<TodoResponse> getTodos(Long userId, String keyword, TodoStatus status, Pageable pageable) {
        return todoRepository.findAll(TodoSpecifications.forUser(userId, keyword, status), pageable)
                .map(TodoResponse::from);
    }

    @Override
    public TodoResponse getTodoById(Long userId, Long todoId) {
        return TodoResponse.from(findOwnedTodoOrThrow(userId, todoId));
    }

    @Override
    @Transactional
    public TodoResponse updateTodo(Long userId, Long todoId, UpdateTodoRequest request) {
        Todo todo = findOwnedTodoOrThrow(userId, todoId);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            todo.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            todo.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            todo.setStatus(request.getStatus());
        }

        return TodoResponse.from(todoRepository.save(todo));
    }

    @Override
    @Transactional
    public void deleteTodo(Long userId, Long todoId) {
        Todo todo = findOwnedTodoOrThrow(userId, todoId);
        todoRepository.delete(todo);
    }

    /**
     * Luôn kiểm tra Todo.User ID == Session.User ID trước khi cho thao tác,
     * và trả về 404 (thay vì 403) để không lộ thông tin có tồn tại Todo của người khác hay không.
     */
    private Todo findOwnedTodoOrThrow(Long userId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Todo với id: " + todoId));

        if (!todo.getUser().getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy Todo với id: " + todoId);
        }
        return todo;
    }
}
