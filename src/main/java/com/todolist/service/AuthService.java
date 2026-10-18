package com.todolist.service;

import com.todolist.dto.request.LoginRequest;
import com.todolist.dto.request.RegisterRequest;
import com.todolist.dto.response.LoginResponse;
import com.todolist.dto.response.UserResponse;
import com.todolist.entity.AppUser;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import com.todolist.exception.DuplicateResourceException;
import com.todolist.repository.AppUserRepository;
import com.todolist.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Đăng ký tài khoản mới. Tài khoản luôn có Role=USER và Status=ACTIVE,
     * không cho phép client tự chọn role.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (appUserRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username đã tồn tại: " + username);
        }
        if (appUserRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email đã tồn tại: " + email);
        }

        AppUser user = AppUser.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return UserResponse.from(appUserRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Sai username hoặc password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Sai username hoặc password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Tài khoản đã bị vô hiệu hóa (Inactive)");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole().name());

        return new LoginResponse(token, "Bearer", user.getUserId(), user.getUsername(), user.getRole().name());
    }
}
