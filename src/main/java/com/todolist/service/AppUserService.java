package com.todolist.service;

import com.todolist.dto.request.CreateUserRequest;
import com.todolist.dto.request.ResetPasswordRequest;
import com.todolist.dto.request.UpdateUserRequest;
import com.todolist.dto.response.UserResponse;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Nghiệp vụ CRUD User, chỉ dùng cho Admin — không đụng chạm gì tới Todo.
 */
public interface AppUserService {

    UserResponse createUser(CreateUserRequest request);

    Page<UserResponse> getUsers(String keyword, Role role, UserStatus status, Pageable pageable);

    UserResponse getUserById(Long userId);

    UserResponse updateUser(Long userId, UpdateUserRequest request);

    void resetPassword(Long userId, ResetPasswordRequest request);

    void deleteUser(Long userId);
}
