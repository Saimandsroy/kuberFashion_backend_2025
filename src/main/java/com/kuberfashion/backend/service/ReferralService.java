package com.kuberfashion.backend.service;

import com.kuberfashion.backend.entity.CoinBalance;
import com.kuberfashion.backend.entity.CoinTransaction;
import com.kuberfashion.backend.entity.ReferralRelation;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.CoinBalanceRepository;
import com.kuberfashion.backend.repository.CoinTransactionRepository;
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

    private static final long REGISTRATION_BONUS = 50L;
    private static final int[] PERCENT_BY_LEVEL = {30, 20, 15, 10, 10, 10, 5};

    @Autowired private UserRepository userRepository;
    @Autowired private ReferralRelationRepository referralRelationRepository;
    @Autowired private CoinBalanceRepository coinBalanceRepository;
    @Autowired private CoinTransactionRepository coinTransactionRepository;

    public void handlePostRegistration(User newUser, String referralPhone) {
        ensureBalance(newUser);
        credit(newUser, REGISTRATION_BONUS, null, null, "REGISTRATION_BONUS");

        if (referralPhone == null || referralPhone.trim().isEmpty()) return;
        Optional<User> maybeRef = userRepository.findByPhone(referralPhone.trim());
        if (maybeRef.isEmpty()) return;
        User referrer = maybeRef.get();

        if (referrer.getId().equals(newUser.getId())) return;
        linkReferral(newUser, referrer);

        ensureBalance(referrer);
        credit(referrer, REGISTRATION_BONUS, newUser, 1, "REFERRAL_DIRECT_BONUS");

        User current = referrer;
        for (int i = 0; i < PERCENT_BY_LEVEL.length; i++) {
            int level = i + 1;
            long amount = Math.round(REGISTRATION_BONUS * (PERCENT_BY_LEVEL[i] / 100.0));
            if (amount <= 0) continue;
            ensureBalance(current);
            credit(current, amount, newUser, level, "REFERRAL_LEVEL_" + level);

            Optional<ReferralRelation> rr = referralRelationRepository.findByUserId(current.getId());
            if (rr.isEmpty() || rr.get().getParent() == null) break;
            current = rr.get().getParent();
        }
    }

    private void linkReferral(User child, User parent) {
        if (referralRelationRepository.findByUserId(child.getId()).isPresent()) return;
        ReferralRelation rr = new ReferralRelation(child, parent);
        referralRelationRepository.save(rr);
    }

    private void ensureBalance(User user) {
        if (coinBalanceRepository.findByUserId(user.getId()).isEmpty()) {
            CoinBalance cb = new CoinBalance(user);
            cb.setBalance(0L);
            coinBalanceRepository.save(cb);
        }
    }

    private void credit(User to, long amount, User source, Integer level, String reason) {
        CoinBalance cb = coinBalanceRepository.findByUserIdForUpdate(to.getId())
                .orElseGet(() -> coinBalanceRepository.findByUserId(to.getId()).orElseGet(() -> {
                    CoinBalance x = new CoinBalance(to);
                    x.setBalance(0L);
                    return coinBalanceRepository.save(x);
                }));
        cb.setBalance(cb.getBalance() + amount);
        coinBalanceRepository.save(cb);

        CoinTransaction tx = new CoinTransaction(to, source, CoinTransaction.TxType.EARN, amount, level, reason);
        coinTransactionRepository.save(tx);
    }

    public static class ReferralItem {
        public Long userId;
        public String masked;
        public String status;
        public String signupTime;
        public long coinsEarnedFrom;
    }

    public static class ReferralStats {
        public String referralCode;
        public int totalDirectReferrals;
        public long totalCoinsEarned;
        public List<ReferralItem> referrals = new ArrayList<>();
    }

    public ReferralStats getReferralStats(User user) {
        ReferralStats stats = new ReferralStats();
        stats.referralCode = user.getPhone();
        // Total coins earned overall
        stats.totalCoinsEarned = coinTransactionRepository.sumEarnedByUser(user.getId());

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
            item.coinsEarnedFrom = coinTransactionRepository.sumEarnedByUserFromSource(user.getId(), referred.getId());
            stats.referrals.add(item);
        }

        return stats;
    }
}
