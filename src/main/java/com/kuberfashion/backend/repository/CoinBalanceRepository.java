package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.CoinBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface CoinBalanceRepository extends JpaRepository<CoinBalance, Long> {

    Optional<CoinBalance> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cb FROM CoinBalance cb WHERE cb.user.id = :userId")
    Optional<CoinBalance> findByUserIdForUpdate(@Param("userId") Long userId);
}
