package com.kuberfashion.backend.dto;

import com.kuberfashion.backend.entity.User;

import java.time.LocalDateTime;

public class UserResponseDto {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private User.Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    
    // Constructors
    public UserResponseDto() {}
    
    public UserResponseDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.createdAt = user.getCreatedAt();
        this.lastActivity = user.getLastActivity();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public User.Role getRole() { return role; }
    public void setRole(User.Role role) { this.role = role; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    
    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
