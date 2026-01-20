package com.kuberfashion.backend.repository;

import com.kuberfashion.backend.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByMerchantOrderId(String merchantOrderId);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.order.id = :orderId ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByOrderIdOrderByCreatedAtDesc(@Param("orderId") Long orderId);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.order.id = :orderId ORDER BY pt.createdAt DESC LIMIT 1")
    Optional<PaymentTransaction> findTopByOrderIdOrderByCreatedAtDesc(@Param("orderId") Long orderId);

    List<PaymentTransaction> findByStatus(PaymentTransaction.TransactionStatus status);
}
