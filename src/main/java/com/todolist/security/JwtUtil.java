package com.todolist.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component  // khai báo với Spring là có một thể hiện của class JwtUtil trong đây và nhét vào container để quản lý
public class JwtUtil {

    @Value("${app.jwt.secret}") //dùng để đọc 1 giá trị từ file cấu hình application.yml rồi gán vào object secret,
    private String secret;  // tạo ra một chìa khóa dùng để verify jwt thông qua method SecretKey key()

    @Value("${app.jwt.expiration-ms}")  // trong file yml nó mặc đinh là string(86400000) -> long khi trong class JwtUtil
    private long expirationMs;  // dùng để set thời gian tồn tại của một token

    // biến này dùng để trả về mảng byte của chuỗi secret và sẽ được mã hóa bằng thuật toán HmacSHA512 dù nó cũng chả mã hóa
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // tạo token
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();   // nhét thông tin của user gồm userid và role của họ
        claims.put("userId", userId);
        claims.put("role", role);

        Date now = new Date();  // lấy thời gian hiện tại
        Date expiry = new Date(now.getTime() + expirationMs);    // tính toán thời gian hết hạn của token now + expirationMs

        return Jwts.builder()
                .claims(claims)     // nhét userid và role
                .subject(username)  // set rằng token này là của user nào
                .issuedAt(now)      // lấy thời gian tạo token để làm mốc tham chiếu cho
                .expiration(expiry) // xem thời gian hết hạn
                .signWith(key())    // gọi method key() để mã hóa bằng thuật toán HmacSHA512
                .compact();         // đóng gói token
    }

    // lấy xem token của user
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    //lấy value của userID
    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    //lấy ra valude của role là ADMIN hay customer
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // chẹcking token có hợp lệ không
    public boolean isTokenValid(String token, String username) {
        String extracted = extractUsername(token);   // lấy token của user
        return extracted.equals(username) && !isTokenExpired(token); // xem token này có đúng của user này không và kiểm tra token hết hạn chưa bằng cách dùng mothod isTokenExpriment()
    }

    // kiểm tra token hết hạn chưa nếu chưa trả về false
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // chuyển hóa lại token sau khi nó được mã hóa bằng thuật toán HmacSHA512
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    // giống cái trên nhưng nó mã hóa toàn bộ thông tin của người dùng trên Claim
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
