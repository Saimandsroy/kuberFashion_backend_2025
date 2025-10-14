package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByPhone(String phone);
    
    Optional<User> findByPhone(String phone);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.enabled = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.phone = :phone AND u.enabled = true")
    Optional<User> findActiveUserByPhone(@Param("phone") String phone);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER'")
    long countCustomers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countActiveUsers();

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) " +
            "AND (:enabled IS NULL OR u.enabled = :enabled) " +
            "AND (:q IS NULL OR (LOWER(CAST(u.email AS string)) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))))" )
    Page<User> findAllFiltered(@Param("role") User.Role role,
                               @Param("enabled") Boolean enabled,
                               @Param("q") String q,
                               Pageable pageable);
}
