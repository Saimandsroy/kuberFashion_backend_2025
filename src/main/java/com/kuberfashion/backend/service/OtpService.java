package com.kuberfashion.backend.service;

public interface OtpService {
    boolean sendOtp(String phone);
    boolean verifyOtp(String phone, String otp);
}
