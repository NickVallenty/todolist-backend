package com.todolist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTodoRequest {

    @NotBlank(message = "Title không được để trống")
    @Size(max = 200)
    private String title;

    private String description;
}
