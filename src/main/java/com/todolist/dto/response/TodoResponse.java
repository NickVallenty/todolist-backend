package com.todolist.dto.response;

import com.todolist.entity.Todo;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TodoResponse {
    private Long todoId;
    private Long userId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
                todo.getTodoId(),
                todo.getUser().getUserId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.getStatus().name(),
                todo.getCreatedDate(),
                todo.getUpdatedDate()
        );
    }
}
