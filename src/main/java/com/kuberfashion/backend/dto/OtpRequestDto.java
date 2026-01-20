package com.kuberfashion.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpRequestDto {
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Invalid phone format")
    private String phone;

    public OtpRequestDto() {}
    public OtpRequestDto(String phone) { this.phone = phone; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
