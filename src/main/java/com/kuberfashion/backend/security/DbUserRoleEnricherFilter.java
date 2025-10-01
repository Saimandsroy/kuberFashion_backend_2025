package com.kuberfashion.backend.security;

import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class DbUserRoleEnricherFilter extends OncePerRequestFilter {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current instanceof JwtAuthenticationToken jwtAuth) {
            String email = (String) jwtAuth.getToken().getClaims().get("email");
            if (email != null && !email.isBlank()) {
                User user = userRepository.findByEmail(email).orElseGet(() -> {
                    User u = new User();
                    u.setEmail(email);
                    u.setFirstName("New");
                    u.setLastName("User");
                    u.setPhone("0000000000");
                    u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    u.setRole(User.Role.USER);
                    u.setEnabled(true);
                    return userRepository.save(u);
                });

                // Update last activity timestamp
                try {
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setLastActivity(LocalDateTime.now());
                    userRepository.save(user);
                } catch (Exception ignored) {}

                Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                newAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
