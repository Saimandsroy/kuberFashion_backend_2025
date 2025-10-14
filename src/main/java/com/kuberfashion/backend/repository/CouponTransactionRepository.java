package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.CouponTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponTransactionRepository extends JpaRepository<CouponTransaction, Long> {

    @Query("SELECT COUNT(ct) FROM CouponTransaction ct WHERE ct.user.id = :userId")
    long countCouponsByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(ct) FROM CouponTransaction ct WHERE ct.user.id = :userId AND ct.sourceUser.id = :sourceUserId")
    long countCouponsByUserFromSource(@Param("userId") Long userId, @Param("sourceUserId") Long sourceUserId);
}
