package com.kuberfashion.backend.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for representing a referral tree node.
 * Each node represents a user and their direct referrals (children).
 */
public class ReferralTreeDto {

    private Long userId;
    private String name;
    private String maskedPhone;
    private String email;
    private int level; // 1 = direct referral, 2 = second level, etc.
    private long couponsEarned;
    private String status; // active/inactive
    private String signupDate;
    private List<ReferralTreeDto> children = new ArrayList<>();

    public ReferralTreeDto() {
    }

    public ReferralTreeDto(Long userId, String name, String maskedPhone, int level) {
        this.userId = userId;
        this.name = name;
        this.maskedPhone = maskedPhone;
        this.level = level;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getCouponsEarned() {
        return couponsEarned;
    }

    public void setCouponsEarned(long couponsEarned) {
        this.couponsEarned = couponsEarned;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSignupDate() {
        return signupDate;
    }

    public void setSignupDate(String signupDate) {
        this.signupDate = signupDate;
    }

    public List<ReferralTreeDto> getChildren() {
        return children;
    }

    public void setChildren(List<ReferralTreeDto> children) {
        this.children = children;
    }

    public void addChild(ReferralTreeDto child) {
        this.children.add(child);
    }
}
