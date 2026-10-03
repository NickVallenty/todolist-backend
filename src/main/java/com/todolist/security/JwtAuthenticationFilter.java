package com.todolist.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component   // khai báo biến này ở trong container để mỗi lần request tới thì spring sẽ tự dộng quát qua
@RequiredArgsConstructor  // tự động constructure cho từng object
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // tạo ra thể hiện của Class để sử đụng chức năng
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override // dùng để ghi đè lớp cha OncePerRequestFilter
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization"); // bóc tách phần header của request

        // nếu header == null và nó không bắt đầu với Bearer thì bỏ qua bước xử lý khác và dừng lại
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);  // chuyển request và respone cho thằng filter khác xử lý hoặc tới thẳng controller
            return;
        }

        String token = authHeader.substring(7); //trả về phần chuỗi còn lại từ 0->7

        try {
            String username = jwtUtil.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {  // nếu user không null và xử lý lại nếu đã có Authentication
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);   // lấy thông tin của user

                if (jwtUtil.isTokenValid(token, username) && userDetails.isEnabled()) {   // nếu token này hợp lệ và thông tin của user cũng hợp lệ
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());  // lấy ra thông tin và quyền truy cập của người này
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));   // lấy thông tin tùy thuộc vào ngữ cảnh rồi nhét vào token
                    SecurityContextHolder.getContext().setAuthentication(authToken);  //lưu trữ toàn bộ thông tin của một token, mỗi lần có một request mới sẽ lấy ra SecurityContext rồi gán vào authToken
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // Token không hợp lệ / hết hạn -> để request đi tiếp không có Authentication,
            // Spring Security sẽ trả 401/403 tương ứng ở bước authorization.
        }

        filterChain.doFilter(request, response); // chuyển request và respone cho thằng filter khác xử lý hoặc tới thẳng controller
    }
}
