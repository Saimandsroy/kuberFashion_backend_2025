package com.kuberfashion.backend.service;

import com.kuberfashion.backend.dto.UserRegistrationDto;
import com.kuberfashion.backend.dto.UserResponseDto;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.exception.UserAlreadyExistsException;
import com.kuberfashion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ReferralService referralService;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found or disabled with email: " + email));
    }
    
    public UserResponseDto registerUser(UserRegistrationDto registrationDto) {
        // Check if passwords match
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        
        // Check if user already exists
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + registrationDto.getEmail());
        }
        
        if (userRepository.existsByPhone(registrationDto.getPhone())) {
            throw new UserAlreadyExistsException("User already exists with phone: " + registrationDto.getPhone());
        }
        
        // Create new user
        User user = new User();
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setEmail(registrationDto.getEmail());
        user.setPhone(registrationDto.getPhone());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        
        User savedUser = userRepository.save(user);
        
        // Referral handling (optional referral code/phone)
        try {
            referralService.handlePostRegistration(savedUser, registrationDto.getCleanedReferralCode());
        } catch (Exception ex) {
            // do not block registration on referral errors
            System.err.println("[Referral] Post-registration handling failed: " + ex.getMessage());
        }
        return new UserResponseDto(savedUser);
    }
    
    public UserResponseDto registerSupabaseUser(UserRegistrationDto registrationDto, String supabaseId) {
        // Check if user already exists by email or supabaseId
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + registrationDto.getEmail());
        }
        
        // Check if phone is provided and already exists
        if (registrationDto.getPhone() != null && !registrationDto.getPhone().isEmpty() 
            && userRepository.existsByPhone(registrationDto.getPhone())) {
            throw new UserAlreadyExistsException("User already exists with phone: " + registrationDto.getPhone());
        }
        
        // Create new user for Supabase authentication
        User user = new User();
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setEmail(registrationDto.getEmail());
        user.setPhone(registrationDto.getPhone());
        user.setPassword(passwordEncoder.encode("SUPABASE_USER")); // Placeholder password
        user.setSupabaseId(supabaseId);
        user.setRole(User.Role.USER);
        user.setEnabled(true);
        
        User savedUser = userRepository.save(user);
        
        // No referral phone provided in sync flow, but keep hook if present
        try {
            referralService.handlePostRegistration(savedUser, registrationDto.getCleanedReferralCode());
        } catch (Exception ex) {
            System.err.println("[Referral] Post-registration handling failed (supabase): " + ex.getMessage());
        }
        return new UserResponseDto(savedUser);
    }
    
    
    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return new UserResponseDto(user);
    }
    
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }
    
    public User updateUser(Long id, String firstName, String lastName, String phone) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        // Check if phone is being changed and if it already exists
        if (!user.getPhone().equals(phone) && userRepository.existsByPhone(phone)) {
            throw new UserAlreadyExistsException("User already exists with phone: " + phone);
        }
        
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        
        return userRepository.save(user);
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
    
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    public void updateUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setEnabled(enabled);
        userRepository.save(user);
    }
    
    public void updateUserRole(Long userId, User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setRole(role);
        userRepository.save(user);
    }
    
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(user);
    }
    
    public void enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(true);
        userRepository.save(user);
    }
    
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(false);
        userRepository.save(user);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }
    
    public long getTotalCustomers() {
        return userRepository.countCustomers();
    }
    
    public long getActiveUsers() {
        return userRepository.countActiveUsers();
    }
    
    public boolean updateUserRoleToAdmin(String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                System.err.println("User not found with email: " + email);
                return false;
            }
            
            user.setRole(User.Role.ADMIN);
            userRepository.save(user);
            System.out.println("✅ User role updated to ADMIN for: " + email);
            return true;
        } catch (Exception e) {
            System.err.println("Error updating user role: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
