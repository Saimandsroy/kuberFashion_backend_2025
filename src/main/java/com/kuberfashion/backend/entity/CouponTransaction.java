package com.kuberfashion.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_transactions", indexes = {
        @Index(name = "idx_coupon_user", columnList = "user_id"),
        @Index(name = "idx_coupon_source", columnList = "source_user_id")
})
public class CouponTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_user_id")
    private User sourceUser;

    @Column(name = "level")
    private Integer level;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CouponTransaction() {}
    public CouponTransaction(User user, User sourceUser, Integer level) {
        this.user = user;
        this.sourceUser = sourceUser;
        this.level = level;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getSourceUser() { return sourceUser; }
    public void setSourceUser(User sourceUser) { this.sourceUser = sourceUser; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
