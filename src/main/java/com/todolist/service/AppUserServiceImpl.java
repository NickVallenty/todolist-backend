package com.todolist.service;

import com.todolist.dto.request.CreateUserRequest;
import com.todolist.dto.request.ResetPasswordRequest;
import com.todolist.dto.request.UpdateUserRequest;
import com.todolist.dto.response.UserResponse;
import com.todolist.entity.AppUser;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import com.todolist.exception.DuplicateResourceException;
import com.todolist.exception.ResourceNotFoundException;
import com.todolist.repository.AppUserRepository;
import com.todolist.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username đã tồn tại: " + request.getUsername());
        }
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã tồn tại: " + request.getEmail());
        }

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
                .build();

        return UserResponse.from(appUserRepository.save(user));
    }

    @Override
    public Page<UserResponse> getUsers(String keyword, Role role, UserStatus status, Pageable pageable) {
        return appUserRepository.findAll(UserSpecifications.search(keyword, role, status), pageable)
                .map(UserResponse::from);
    }

    @Override
    public UserResponse getUserById(Long userId) {
        return UserResponse.from(findUserOrThrow(userId));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        AppUser user = findUserOrThrow(userId);

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (appUserRepository.existsByUsernameAndUserIdNot(request.getUsername(), userId)) {
                throw new DuplicateResourceException("Username đã tồn tại: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (appUserRepository.existsByEmailAndUserIdNot(request.getEmail(), userId)) {
                throw new DuplicateResourceException("Email đã tồn tại: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        return UserResponse.from(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, ResetPasswordRequest request) {
        AppUser user = findUserOrThrow(userId);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        appUserRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        AppUser user = findUserOrThrow(userId);
        // Cascade delete: nhờ cấu hình cascade = ALL + orphanRemoval trên AppUser.todos,
        // toàn bộ Todo của user này sẽ tự động bị xóa theo.
        appUserRepository.delete(user);
    }

    private AppUser findUserOrThrow(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User với id: " + userId));
    }
}
