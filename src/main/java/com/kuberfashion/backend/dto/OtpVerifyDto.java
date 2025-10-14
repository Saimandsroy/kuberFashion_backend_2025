package com.kuberfashion.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpVerifyDto {
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Invalid phone format")
    private String phone;

    @NotBlank(message = "OTP is required")
    private String otp;

    public OtpVerifyDto() {}

    public OtpVerifyDto(String phone, String otp) {
        this.phone = phone;
        this.otp = otp;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
