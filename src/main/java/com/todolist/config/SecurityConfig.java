package com.todolist.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todolist.dto.response.ApiError;
import com.todolist.security.CustomUserDetailsService;
import com.todolist.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;

@Configuration     // khai báo các @Bean trong SercurityConfig để lúc chạy Spring nó sẽ tự động chạy
@EnableWebSecurity  // để chuyển từ cấu hình bảo mật mặc định sang chuỗi được tực hiện ở securityFilterChain
@EnableMethodSecurity // phân quyền theo từng role và method, user chỉ được dùng chức năng của user không được dùng của admin
@RequiredArgsConstructor  // tự động tạo ra một constructor cho 2 biên private final
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService; // lấy user từ DB thông qua object userDetailsService
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // dùng để mã hoá / so sánh mật khẩu bằng thuật toán BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // lấy user từ DB rồi so sánh password
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();  // Tạo 1 provider mới
        provider.setUserDetailsService(userDetailsService);  //tiêm data của user rồ set vào provider
        provider.setPasswordEncoder(passwordEncoder());   // gán và so sánh password
        return provider;  // Trả provider đã cấu hình đầy đủ
    }

    // tiêm vào cấu hình được lưu trong provider(đã được xác thực)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();  // xác thực mỗi khi user gọi API login
    }

    // định nghĩa "chuỗi filter bảo mật" phải áp dụng
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF protection vì chỉ session và cookie sử dụng
                // Không cho Spring tạo/lưu HttpSession để mỗi req phải mang theo token để xác thực lại từ đầu
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // khai báo xem mọi người sẽ vào đâu theo thứ tự dưới đây
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()  // Các API dưới /api/auth/ (login, register...) -> cho phép tất cả đăng nhập không cần token nào
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/todos/**").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeJsonError(response, HttpStatus.UNAUTHORIZED, "Yêu cầu đăng nhập (token thiếu/không hợp lệ/hết hạn)"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeJsonError(response, HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập chức năng này"))
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJsonError(HttpServletResponse response, HttpStatus status, String message)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, null);
        response.getWriter().write(new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .writeValueAsString(error));
    }
}
