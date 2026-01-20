package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    @Query("SELECT a FROM Address a WHERE a.user.id = :userId ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT a FROM Address a WHERE a.id = :id AND a.user.id = :userId")
    Optional<Address> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.isDefault = true")
    Optional<Address> findByUserIdAndIsDefaultTrue(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.id != :addressId")
    void clearDefaultForUser(@Param("userId") Long userId, @Param("addressId") Long addressId);

    @Query("SELECT COUNT(a) FROM Address a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Address a WHERE a.id = :id AND a.user.id = :userId")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
