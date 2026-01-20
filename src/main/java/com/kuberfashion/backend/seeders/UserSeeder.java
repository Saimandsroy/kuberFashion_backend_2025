package com.kuberfashion.backend.seeders;

import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// @Component - DISABLED for clean referral testing - test users should be created manually via signup
public class UserSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create Default Regular User
        if (!userRepository.existsByPhone("9876543210")) {
            User user = new User();
            user.setFirstName("Test");
            user.setLastName("User");
            user.setEmail("user@kuberfashion.com");
            user.setPhone("9876543210");
            user.setPassword(passwordEncoder.encode("user123456"));
            user.setRole(User.Role.USER);
            user.setEnabled(true);

            userRepository.save(user);
            System.out.println("✅ Default regular user seeded successfully!");
        } else {
            System.out.println("ℹ️ Default regular user already exists.");
        }
    }
}
