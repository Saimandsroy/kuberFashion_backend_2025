package com.kuberfashion.backend.service;

import com.kuberfashion.backend.entity.CouponTransaction;
import com.kuberfashion.backend.entity.ReferralRelation;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.CouponTransactionRepository;
import com.kuberfashion.backend.repository.ReferralRelationRepository;
import com.kuberfashion.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReferralService {

    private static final Logger logger = LoggerFactory.getLogger(ReferralService.class);

    private static final int MAX_LEVELS = 6;
    private static final String GENESIS_CODE = "kuberfashion2025";
    private static final String ROOT_ADMIN_PHONE = "1234567890";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReferralRelationRepository referralRelationRepository;
    @Autowired
    private CouponTransactionRepository couponTransactionRepository;

    public void validateReferralCode(String referralCode) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return; // No code, nothing to validate (registration allowed without referral)
        }

        String code = referralCode.trim();

        // Handle Genesis code (kuberfashion2025) - only works ONCE for the first root
        // member
        if (GENESIS_CODE.equalsIgnoreCase(code)) {
            // Genesis code only works if NO referral relations exist yet (no root member)
            if (referralRelationRepository.count() > 0) {
                throw new IllegalArgumentException(
                        "The Genesis Code (kuberfashion2025) has already been utilized. Please use a valid member referral code from an existing member.");
            }
            return; // Genesis code is valid (no root yet)
        }

        // For phone-based referral codes
        User referrer = userRepository.findByPhone(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid referral code: User not found"));

        if (!referrer.isEnabled()) {
            throw new IllegalArgumentException("Referrer account is inactive");
        }

        // CRITICAL: Referrer must be a KVision member (exist in referral tree)
        if (!isMember(referrer.getId())) {
            throw new IllegalArgumentException(
                    "Invalid referral code: This user is not a KVision member. Only existing KVision members can refer new users.");
        }

        // Check limit: Max 5 direct referrals (excluding self-references)
        long directCount = referralRelationRepository.countDirectReferrals(referrer.getId());
        if (directCount >= 5) {
            throw new IllegalArgumentException(
                    "Referral limit reached for this user (Max 5 direct referrals allowed).");
        }
    }

    /**
     * Validate referral code with user context - includes self-referral prevention.
     * Call this when you have the authenticated user.
     */
    public void validateReferralCodeForUser(String referralCode, User currentUser) {
        if (referralCode == null || referralCode.trim().isEmpty()) {
            return;
        }

        String code = referralCode.trim();

        // DEBUG: Log the values being compared
        logger.info("🔍 Self-referral check: user phone='{}', referral code='{}'",
                currentUser != null ? currentUser.getPhone() : "null", code);

        // Prevent self-referral: user cannot use their own phone number
        if (currentUser != null && currentUser.getPhone() != null
                && currentUser.getPhone().equals(code)) {
            logger.warn("❌ Self-referral attempt blocked for user: {}", currentUser.getEmail());
            throw new IllegalArgumentException(
                    "You cannot use your own phone number as a referral code. Please enter a valid referral code from an existing KVision member.");
        }

        // Delegate to standard validation
        validateReferralCode(referralCode);
    }

    /**
     * Check if a user is a KVision member.
     * A member is someone who:
     * 1. Is an Admin (inherently part of the program as root)
     * 2. Is a child in the referral tree (has a parent - joined via referral)
     * 3. Is a parent in the referral tree (has referred others)
     */
    public boolean isMember(Long userId) {
        // Check if user is Admin (they ARE the program root)
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ADMIN) {
            return true;
        }

        // Check if user is a child (has parent)
        boolean isChild = referralRelationRepository.findByUserId(userId).isPresent();
        if (isChild)
            return true;

        // Check if user is a parent (has referred others)
        boolean isParent = referralRelationRepository.countByParentId(userId) > 0;
        return isParent;
    }

    public void handlePostRegistration(User newUser, String referralCode) {
        // No coupon for the new user; coupons go to referrers only
        if (referralCode == null || referralCode.trim().isEmpty())
            return;

        // Check for global company referral code (GENESIS CODE)
        if (GENESIS_CODE.equalsIgnoreCase(referralCode.trim())) {
            // Genesis code makes the user a ROOT of the referral tree (no parent)
            // This can only be used ONCE - check if any root already exists

            // Check if ANY referral relation exists (meaning Genesis was already used)
            if (referralRelationRepository.count() > 0) {
                throw new IllegalArgumentException(
                        "The Genesis Code (kuberfashion2025) has already been utilized. Please use a valid member referral code from an existing member.");
            }

            // Genesis user becomes a STANDALONE ROOT - no parent relation created
            // They will appear as root in the referral tree and can now refer others
            // We create a special self-referential entry to mark them as "Genesis Root"
            ReferralRelation rootEntry = new ReferralRelation();
            rootEntry.setUser(newUser);
            rootEntry.setParent(newUser); // Self-reference indicates Genesis root
            referralRelationRepository.save(rootEntry);

            logger.info("✅ Genesis Root Member created: {} (ID: {})",
                    newUser.getFirstName() + " " + newUser.getLastName(), newUser.getId());
            return;
        }

        Optional<User> maybeRef = userRepository.findByPhone(referralCode.trim());
        if (maybeRef.isPresent()) {
            User referrer = maybeRef.get();

            // CRITICAL: Prevent self-referral - user cannot use their own phone as referral
            // code
            if (referrer.getId().equals(newUser.getId())) {
                throw new IllegalArgumentException(
                        "You cannot use your own phone number as a referral code. Please enter a valid referral code from an existing KVision member.");
            }

            processReferral(newUser, referrer);
        }
    }

    private void processReferral(User newUser, User referrer) {
        if (referrer.getId().equals(newUser.getId()))
            return;

        // Prevent cycles: ensure referrer is not in newUser's descendant chain
        if (wouldCreateCycle(newUser, referrer))
            return;

        // Validate max 5 direct referrals (excluding self-references)
        long directCount = referralRelationRepository.countDirectReferrals(referrer.getId());
        if (directCount >= 5) {
            throw new IllegalArgumentException(
                    "Referral limit reached for this user (Max 5 allowed). Please use a different referral code.");
        }

        // Link referral one-time (unique child)
        linkReferral(newUser, referrer);

        // Award 1 coupon to each ancestor up to 6 levels
        // Use Set to prevent duplicate awards if tree has issues
        java.util.Set<Long> awardedUserIds = new java.util.HashSet<>();
        User current = referrer;

        for (int level = 1; level <= MAX_LEVELS && current != null; level++) {
            // Prevent duplicate coupon awards
            if (awardedUserIds.contains(current.getId())) {
                logger.warn("🚫 Skipping duplicate coupon award to user ID: {} at level {}", current.getId(), level);
                break;
            }

            awardCoupon(current, newUser, level);
            awardedUserIds.add(current.getId());
            logger.info("✅ Awarded 1 coupon to user ID: {} at level {} (from new member: {})",
                    current.getId(), level, newUser.getId());

            // Get parent for next level
            Optional<ReferralRelation> rr = referralRelationRepository.findByUserId(current.getId());
            if (rr.isEmpty() || rr.get().getParent() == null) {
                break; // No parent, stop
            }

            User parent = rr.get().getParent();
            // Stop if self-referential (Genesis root)
            if (parent.getId().equals(current.getId())) {
                logger.info("🌳 Reached Genesis root (self-referential), stopping coupon distribution");
                break;
            }

            current = parent;
        }

        logger.info("📊 Total coupons distributed for new member {}: {}", newUser.getId(), awardedUserIds.size());
    }

    private boolean wouldCreateCycle(User child, User parentCandidate) {
        // Traverse up from parentCandidate; if we find child, cycle would occur
        User current = parentCandidate;
        for (int i = 0; i < MAX_LEVELS && current != null; i++) {
            if (current.getId().equals(child.getId()))
                return true;
            Optional<ReferralRelation> rr = referralRelationRepository.findByUserId(current.getId());
            if (rr.isEmpty() || rr.get().getParent() == null)
                break;
            current = rr.get().getParent();
        }
        return false;
    }

    private void linkReferral(User child, User parent) {
        if (referralRelationRepository.findByUserId(child.getId()).isPresent())
            return;
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
        // Total coupons is the user's current kuberCoupons balance (updated by admin or
        // referrals)
        stats.totalCouponsEarned = user.getKuberCoupons();

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
            item.couponsEarnedFrom = couponTransactionRepository.countCouponsByUserFromSource(user.getId(),
                    referred.getId());
            stats.referrals.add(item);
        }

        return stats;
    }

    /**
     * Optimized method to fetch the referral tree up to 6 levels.
     * Uses Level-by-Level Bulk Fetching: exactly 6 queries regardless of tree size.
     */
    public com.kuberfashion.backend.dto.ReferralTreeDto getReferralTree(Long rootUserId) {
        User rootUser = userRepository.findById(rootUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Create root node
        com.kuberfashion.backend.dto.ReferralTreeDto root = createTreeNode(rootUser, 0);

        // Level-by-level fetching
        // Map to track parent -> children for efficient attachment
        java.util.Map<Long, com.kuberfashion.backend.dto.ReferralTreeDto> nodeMap = new java.util.HashMap<>();
        nodeMap.put(rootUserId, root);

        List<Long> currentLevelIds = List.of(rootUserId);

        for (int level = 1; level <= MAX_LEVELS; level++) {
            if (currentLevelIds.isEmpty())
                break;

            // Fetch all children of current level in ONE query
            List<ReferralRelation> relations = referralRelationRepository.findByParentIdIn(currentLevelIds);
            if (relations.isEmpty())
                break;

            List<Long> nextLevelIds = new ArrayList<>();

            for (ReferralRelation rr : relations) {
                User child = rr.getUser();
                Long parentId = rr.getParent().getId();

                com.kuberfashion.backend.dto.ReferralTreeDto childNode = createTreeNode(child, level);
                childNode.setCouponsEarned(couponTransactionRepository.countCouponsByUser(child.getId()));

                // Attach to parent
                com.kuberfashion.backend.dto.ReferralTreeDto parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    parentNode.addChild(childNode);
                }

                // Add to map for next level
                nodeMap.put(child.getId(), childNode);
                nextLevelIds.add(child.getId());
            }

            currentLevelIds = nextLevelIds;
        }

        return root;
    }

    private com.kuberfashion.backend.dto.ReferralTreeDto createTreeNode(User user, int level) {
        com.kuberfashion.backend.dto.ReferralTreeDto node = new com.kuberfashion.backend.dto.ReferralTreeDto();
        node.setUserId(user.getId());
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "")).trim();
        node.setName(fullName);
        node.setLevel(level);
        node.setStatus(user.isEnabled() ? "active" : "inactive");
        node.setSignupDate(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);

        // Mask phone
        String phone = user.getPhone();
        if (phone != null && phone.length() >= 4) {
            node.setMaskedPhone("******" + phone.substring(phone.length() - 4));
        } else {
            node.setMaskedPhone("user-" + user.getId());
        }

        return node;
    }

    public java.util.Map<String, Object> getReferrerDetails(String referralCode) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (referralCode == null || referralCode.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "Code cannot be empty");
            return result;
        }

        String code = referralCode.trim();

        // GENESIS CODE Check - only valid if NO referral relations exist yet
        if (GENESIS_CODE.equalsIgnoreCase(code)) {
            if (referralRelationRepository.count() > 0) {
                result.put("valid", false);
                result.put("message",
                        "Genesis Code has already been used. Please use a KVision member's phone number.");
                return result;
            }
            result.put("valid", true);
            result.put("name", "Kuber Fashion (Genesis)");
            return result;
        }

        // Phone-based referral code lookup
        Optional<User> maybeRef = userRepository.findByPhone(code);
        if (maybeRef.isEmpty()) {
            result.put("valid", false);
            result.put("message", "Invalid Referral Code - User not found");
            return result;
        }

        User referrer = maybeRef.get();

        if (!referrer.isEnabled()) {
            result.put("valid", false);
            result.put("message", "Referrer account is inactive");
            return result;
        }

        // CRITICAL: Check if referrer is a KVision member (in referral tree)
        if (!isMember(referrer.getId())) {
            result.put("valid", false);
            result.put("message", "Invalid Referral Code - This user is not a KVision member");
            return result;
        }

        long directCount = referralRelationRepository.countDirectReferrals(referrer.getId());
        if (directCount >= 5) {
            result.put("valid", false);
            result.put("message", "Referral Limit Reached (Max 5 direct referrals)");
            return result;
        }

        result.put("valid", true);
        result.put("name", referrer.getFirstName() + " " + referrer.getLastName());
        return result;
    }
}
