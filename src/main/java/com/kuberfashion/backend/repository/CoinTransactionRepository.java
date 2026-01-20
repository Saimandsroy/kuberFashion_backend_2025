package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {

    List<CoinTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(ct.amount),0) FROM CoinTransaction ct WHERE ct.user.id = :userId AND ct.type = 'EARN'")
    long sumEarnedByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(ct.amount),0) FROM CoinTransaction ct WHERE ct.user.id = :userId AND ct.type = 'SPEND'")
    long sumSpentByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(ct.amount),0) FROM CoinTransaction ct WHERE ct.user.id = :userId AND ct.sourceUser.id = :sourceUserId")
    long sumEarnedByUserFromSource(@Param("userId") Long userId, @Param("sourceUserId") Long sourceUserId);
}
