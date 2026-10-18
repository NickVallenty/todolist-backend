package com.todolist.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTodoRequest {

    @Schema(description = "Tiêu đề công việc", example = "Học Spring Boot", maxLength = 200)
    @NotBlank(message = "Title không được để trống")
    @Size(max = 200)
    private    String title;

    @Schema(description = "Mô tả chi tiết", example = "Làm xong phần Swagger")
    private String description;
}