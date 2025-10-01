package com.kuberfashion.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SupabaseUserSyncDto {
    
    @NotBlank(message = "Supabase ID is required")
    private String supabaseId;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    private String phone;
    
    // Constructors
    public SupabaseUserSyncDto() {}
    
    public SupabaseUserSyncDto(String supabaseId, String email, String firstName, String lastName, String phone) {
        this.supabaseId = supabaseId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }
    
    // Getters and Setters
    public String getSupabaseId() {
        return supabaseId;
    }
    
    public void setSupabaseId(String supabaseId) {
        this.supabaseId = supabaseId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
}
