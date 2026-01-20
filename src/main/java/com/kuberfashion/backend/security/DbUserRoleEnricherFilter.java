package com.kuberfashion.backend.security;

import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(DbUserRoleEnricherFilter.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        logger.debug("🔐 DbUserRoleEnricherFilter processing: {}", requestURI);

        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        
        if (current == null) {
            logger.debug("⚠️ No authentication found in SecurityContext");
        } else {
            logger.debug("📝 Current authentication type: {}", current.getClass().getSimpleName());
        }

        if (current instanceof JwtAuthenticationToken jwtAuth) {
            logger.info("🎫 JWT Token detected - Processing Supabase authentication");
            
            String email = (String) jwtAuth.getToken().getClaims().get("email");
            String sub = (String) jwtAuth.getToken().getClaims().get("sub");
            
            logger.info("  📧 Email from JWT: {}", email);
            logger.info("  🆔 Subject (sub) from JWT: {}", sub);
            
            if (email != null && !email.isBlank()) {
                User user = userRepository.findByEmail(email).orElseGet(() -> {
                    logger.warn("⚠️ User not found in DB - Creating new user for: {}", email);
                    User u = new User();
                    u.setEmail(email);
                    u.setFirstName("New");
                    u.setLastName("User");
                    u.setPhone("0000000000");
                    u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    u.setRole(User.Role.USER);
                    u.setEnabled(true);
                    User savedUser = userRepository.save(u);
                    logger.info("✅ New user created with ID: {}", savedUser.getId());
                    return savedUser;
                });

                logger.info("👤 User found/created: {} | Role: {}", user.getEmail(), user.getRole());

                // Update last activity timestamp
                try {
                    user.setUpdatedAt(LocalDateTime.now());
                    user.setLastActivity(LocalDateTime.now());
                    userRepository.save(user);
                    logger.debug("  ⏰ Updated last activity timestamp");
                } catch (Exception e) {
                    logger.error("  ❌ Failed to update last activity: {}", e.getMessage());
                }

                Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(user, null, authorities);
                newAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(newAuth);
                
                logger.info("✅ Authentication enriched with DB user - Authorities: {}", authorities);
            } else {
                logger.warn("⚠️ No email found in JWT token claims");
            }
        } else if (current != null) {
            logger.debug("ℹ️ Authentication type is not JWT - Skipping enrichment");
        }

        filterChain.doFilter(request, response);
    }
}
