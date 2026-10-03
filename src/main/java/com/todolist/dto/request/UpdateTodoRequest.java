package com.todolist.dto.request;

import com.todolist.enums.TodoStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateTodoRequest {

    @Size(max = 200)
    private String title;

    private String description;

    private TodoStatus status;
}
