package com.kuberfashion.backend.service;

import com.kuberfashion.backend.entity.CouponTransaction;
import com.kuberfashion.backend.entity.ReferralRelation;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.CouponTransactionRepository;
import com.kuberfashion.backend.repository.ReferralRelationRepository;
import com.kuberfashion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReferralService {

    private static final int MAX_LEVELS = 6;

    @Autowired private UserRepository userRepository;
    @Autowired private ReferralRelationRepository referralRelationRepository;
    @Autowired private CouponTransactionRepository couponTransactionRepository;

    public void handlePostRegistration(User newUser, String referralCode) {
        // No coupon for the new user; coupons go to referrers only
        if (referralCode == null || referralCode.trim().isEmpty()) return;
        
        // Check for global company referral code
        if ("kuberfashion2025".equalsIgnoreCase(referralCode.trim())) {
            // Global referral - just log it, no specific referrer
            System.out.println("User registered with global referral code: kuberfashion2025");
            return;
        }
        
        Optional<User> maybeRef = userRepository.findByPhone(referralCode.trim());
        if (maybeRef.isEmpty()) return;
        User referrer = maybeRef.get();

        if (referrer.getId().equals(newUser.getId())) return;

        // Prevent cycles: ensure referrer is not in newUser's descendant chain
        if (wouldCreateCycle(newUser, referrer)) return;

        // Link referral one-time (unique child)
        linkReferral(newUser, referrer);

        // Award 1 coupon to each ancestor up to 6 levels
        User current = referrer;
        for (int level = 1; level <= MAX_LEVELS && current != null; level++) {
            awardCoupon(current, newUser, level);
            Optional<ReferralRelation> rr = referralRelationRepository.findByUserId(current.getId());
            if (rr.isEmpty() || rr.get().getParent() == null) break;
            current = rr.get().getParent();
        }
    }

    private boolean wouldCreateCycle(User child, User parentCandidate) {
        // Traverse up from parentCandidate; if we find child, cycle would occur
        User current = parentCandidate;
        for (int i = 0; i < MAX_LEVELS && current != null; i++) {
            if (current.getId().equals(child.getId())) return true;
            Optional<ReferralRelation> rr = referralRelationRepository.findByUserId(current.getId());
            if (rr.isEmpty() || rr.get().getParent() == null) break;
            current = rr.get().getParent();
        }
        return false;
    }

    private void linkReferral(User child, User parent) {
        if (referralRelationRepository.findByUserId(child.getId()).isPresent()) return;
        ReferralRelation rr = new ReferralRelation(child, parent);
        referralRelationRepository.save(rr);
    }

    private void awardCoupon(User to, User source, Integer level) {
        // optimistic lock via @Version on User
        to.setKuberCoupons(to.getKuberCoupons() + 1);
        userRepository.save(to);

        CouponTransaction tx = new CouponTransaction(to, source, level);
        couponTransactionRepository.save(tx);
    }

    public static class ReferralItem {
        public Long userId;
        public String masked;
        public String status;
        public String signupTime;
        public long couponsEarnedFrom;
    }

    public static class ReferralStats {
        public String referralCode;
        public int totalDirectReferrals;
        public long totalCouponsEarned;
        public List<ReferralItem> referrals = new ArrayList<>();
    }

    public ReferralStats getReferralStats(User user) {
        ReferralStats stats = new ReferralStats();
        stats.referralCode = user.getPhone();
        // Total coupons earned overall
        stats.totalCouponsEarned = couponTransactionRepository.countCouponsByUser(user.getId());

        // Direct referrals list
        List<ReferralRelation> directs = referralRelationRepository.getDirectReferrals(user.getId());
        stats.totalDirectReferrals = directs.size();

        for (ReferralRelation rr : directs) {
            User referred = rr.getUser();
            ReferralItem item = new ReferralItem();
            item.userId = referred.getId();
            String phone = referred.getPhone();
            if (phone != null && phone.length() >= 4) {
                String last4 = phone.substring(phone.length() - 4);
                item.masked = "******" + last4;
            } else {
                item.masked = "user-" + referred.getId();
            }
            item.status = referred.isEnabled() ? "active" : "inactive";
            item.signupTime = referred.getCreatedAt() != null ? referred.getCreatedAt().toString() : null;
            item.couponsEarnedFrom = couponTransactionRepository.countCouponsByUserFromSource(user.getId(), referred.getId());
            stats.referrals.add(item);
        }

        return stats;
    }
}
