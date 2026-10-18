package com.todolist.service;

import com.todolist.exception.OtpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Gửi email chứa OTP. SMTP cấu hình qua biến môi trường (SPRING_MAIL_HOST, SPRING_MAIL_PORT,
 * SPRING_MAIL_USERNAME, SPRING_MAIL_PASSWORD...), không có credential nào trong source.
 * JavaMailSender lấy qua ObjectProvider để ứng dụng vẫn khởi động bình thường khi chưa cấu hình SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpMailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.otp.mail-from:${spring.mail.username:}}")
    private String from;

    public void sendOtp(String toEmail, String username, String otp, long expirationMinutes) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.error("Chưa cấu hình SMTP (spring.mail.host) nên không thể gửi OTP");
            throw OtpException.mailUnavailable();
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(toEmail);
        message.setSubject("[TodoList] Mã OTP đổi mật khẩu");
        message.setText("Xin chào " + username + ",\n\n"
                + "Mã OTP đổi mật khẩu của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong " + expirationMinutes + " phút và chỉ dùng được một lần.\n\n"
                + "Nếu bạn không yêu cầu đổi mật khẩu, hãy bỏ qua email này và không chia sẻ mã cho bất kỳ ai.");

        try {
            sender.send(message);
        } catch (MailException ex) {
            // Không log OTP / nội dung mail.
            log.error("Gửi email OTP thất bại: {}", ex.getClass().getSimpleName());
            throw OtpException.mailUnavailable();
        }
    }
    public void sendDeleteAccountOtp(String toEmail, String username, String otp, long expirationMinutes) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.error("Chưa cấu hình SMTP (spring.mail.host) nên không thể gửi OTP");
            throw OtpException.mailUnavailable();
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(toEmail);
        message.setSubject("[TodoList] Mã OTP xác nhận xóa tài khoản");
        message.setText("Xin chào " + username + ",\n\n"
                + "Mã OTP xác nhận XÓA TÀI KHOẢN của bạn là: " + otp + "\n"
                + "Mã có hiệu lực trong " + expirationMinutes + " phút và chỉ dùng được một lần.\n"
                + "Sau khi xác nhận, tài khoản và toàn bộ công việc (todo) của bạn sẽ bị xóa vĩnh viễn, không thể khôi phục.\n\n"
                + "Nếu bạn không yêu cầu xóa tài khoản, hãy bỏ qua email này, đổi mật khẩu ngay và không chia sẻ mã cho bất kỳ ai.");

        try {
            sender.send(message);
        } catch (MailException ex) {
            // Không log OTP / nội dung mail.
            log.error("Gửi email OTP thất bại: {}", ex.getClass().getSimpleName());
            throw OtpException.mailUnavailable();
        }
    }
}
