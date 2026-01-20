package com.kuberfashion.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_relations", indexes = {
    @Index(name = "idx_ref_user", columnList = "user_id", unique = true),
    @Index(name = "idx_ref_parent", columnList = "parent_id")
})
public class ReferralRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user; // the referred user (child)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private User parent; // the referrer (immediate parent)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ReferralRelation() {}
    public ReferralRelation(User user, User parent) {
        this.user = user;
        this.parent = parent;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
