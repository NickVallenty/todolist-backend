package com.todolist.repository;

import com.todolist.entity.PasswordOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PasswordOtpRepository extends JpaRepository<PasswordOtp, Long> {

    /**
     * OTP mới nhất của user. Khóa ghi (PESSIMISTIC_WRITE) để các request đồng thời
     * không thể "làm mất" bộ đếm số lần nhập sai (chống brute-force song song).
     * Phải gọi trong transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordOtp> findFirstByUserIdOrderByOtpIdDesc(Long userId);

    void deleteByUserId(Long userId);
}
