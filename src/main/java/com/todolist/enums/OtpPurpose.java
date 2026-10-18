package com.todolist.enums;

/**
 * Mục đích xin OTP qua POST /api/auth/request-otp.
 * Mỗi mục đích dùng bảng OTP riêng nên OTP của mục đích này không dùng được cho mục đích kia.
 */
public enum OtpPurpose {
    CHANGE_PASSWORD,
    DELETE_ACCOUNT
}