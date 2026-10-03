package com.todolist.service;

import com.todolist.dto.request.LoginRequest;
import com.todolist.dto.response.LoginResponse;
import com.todolist.entity.AppUser;
import com.todolist.enums.UserStatus;
import com.todolist.repository.AppUserRepository;
import com.todolist.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
