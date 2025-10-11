package com.kuberfashion.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coin_transactions", indexes = {
    @Index(name = "idx_tx_user", columnList = "user_id"),
    @Index(name = "idx_tx_source_user", columnList = "source_user_id"),
    @Index(name = "idx_tx_created_at", columnList = "created_at")
})
public class CoinTransaction {

    public enum TxType { EARN, SPEND, ADJUST }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // recipient of the coin change

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_user_id")
    private User sourceUser; // the user who triggered this earning (e.g., referred user)

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TxType type = TxType.EARN;

    @Column(name = "amount", nullable = false)
    private long amount; // positive for earn, negative for spend

    @Column(name = "level")
    private Integer level; // referral level if applicable (1..7)

    @Column(name = "reason", length = 100)
    private String reason; // e.g., REGISTRATION_BONUS, REFERRAL_LEVEL

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CoinTransaction() {}

    public CoinTransaction(User user, User sourceUser, TxType type, long amount, Integer level, String reason) {
        this.user = user;
        this.sourceUser = sourceUser;
        this.type = type;
        this.amount = amount;
        this.level = level;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getSourceUser() { return sourceUser; }
    public void setSourceUser(User sourceUser) { this.sourceUser = sourceUser; }
    public TxType getType() { return type; }
    public void setType(TxType type) { this.type = type; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
