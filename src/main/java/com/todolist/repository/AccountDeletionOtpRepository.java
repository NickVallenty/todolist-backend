package com.todolist.repository;

import com.todolist.entity.AccountDeletionOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AccountDeletionOtpRepository extends JpaRepository<AccountDeletionOtp, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountDeletionOtp> findFirstByUserIdOrderByOtpIdDesc(Long userId);

    void deleteByUserId(Long userId);
}