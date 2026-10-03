package com.todolist.controller;

import com.todolist.dto.request.CreateUserRequest;
import com.todolist.dto.request.ResetPasswordRequest;
import com.todolist.dto.request.UpdateUserRequest;
import com.todolist.dto.response.UserResponse;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import com.todolist.service.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Chỉ Admin được truy cập (đã enforce ở SecurityConfig: /api/admin/** -> hasRole("ADMIN")).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserService appUserService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appUserService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(appUserService.getUsers(keyword, role, status, pageable));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(appUserService.getUserById(userId));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long userId,
                                                    @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(appUserService.updateUser(userId, request));
    }

    @PutMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long userId,
                                               @Valid @RequestBody ResetPasswordRequest request) {
        appUserService.resetPassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        appUserService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
