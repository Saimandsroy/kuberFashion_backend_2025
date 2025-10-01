package com.kuberfashion.backend.controller;

import com.kuberfashion.backend.dto.*;
import com.kuberfashion.backend.dto.SupabaseUserSyncDto;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.security.JwtTokenProvider;
import com.kuberfashion.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> authenticateUser(@Valid @RequestBody UserLoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(),
                        loginDto.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        
        UserResponseDto userDto = userService.getUserByEmail(loginDto.getEmail());
        JwtAuthenticationResponse response = new JwtAuthenticationResponse(jwt, userDto);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDto>> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        UserResponseDto user = userService.registerUser(registrationDto);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", user), HttpStatus.CREATED);
    }
    
    @PostMapping("/sync-user")
    public ResponseEntity<ApiResponse<UserResponseDto>> syncSupabaseUser(@Valid @RequestBody SupabaseUserSyncDto syncDto) {
        try {
            // This endpoint is called after Supabase user creation to create corresponding backend user
            UserRegistrationDto registrationDto = new UserRegistrationDto();
            registrationDto.setEmail(syncDto.getEmail());
            registrationDto.setFirstName(syncDto.getFirstName());
            registrationDto.setLastName(syncDto.getLastName());
            registrationDto.setPhone(syncDto.getPhone());
            registrationDto.setPassword("SUPABASE_USER"); // Placeholder password for Supabase users
            
            UserResponseDto user = userService.registerSupabaseUser(registrationDto, syncDto.getSupabaseId());
            return new ResponseEntity<>(ApiResponse.success("User synced successfully", user), HttpStatus.CREATED);
        } catch (Exception e) {
            System.err.println("Error syncing user: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(ApiResponse.error("Failed to sync user: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/create-admin")
    public ResponseEntity<ApiResponse<String>> createAdminUser(@RequestParam String email) {
        try {
            boolean updated = userService.updateUserRoleToAdmin(email);
            if (updated) {
                return ResponseEntity.ok(ApiResponse.success("User role updated to ADMIN", email));
            } else {
                return new ResponseEntity<>(ApiResponse.error("User not found with email: " + email), HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(ApiResponse.error("Failed to update role: " + e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(@RequestParam String email) {
        boolean isAvailable = !userService.existsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Email availability checked", isAvailable));
    }
    
    @PostMapping("/check-phone")
    public ResponseEntity<ApiResponse<Boolean>> checkPhoneAvailability(@RequestParam String phone) {
        boolean isAvailable = !userService.existsByPhone(phone);
        return ResponseEntity.ok(ApiResponse.success("Phone availability checked", isAvailable));
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(@AuthenticationPrincipal User user) {
        UserResponseDto userResponse = new UserResponseDto(user);
        return ResponseEntity.ok(ApiResponse.success("Current user retrieved successfully", userResponse));
    }
}
