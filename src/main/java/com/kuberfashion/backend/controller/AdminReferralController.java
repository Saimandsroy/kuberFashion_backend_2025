package com.kuberfashion.backend.controller;

import com.kuberfashion.backend.dto.ApiResponse;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.ReferralRelationRepository;
import com.kuberfashion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/admin/referrals")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AdminReferralController {

    @Autowired private ReferralRelationRepository referralRepo;
    @Autowired private UserRepository userRepo;

    public static class TreeNode {
        public Long id;
        public String masked;
        public String phone;
        public String signupDate;
        public String status;
        public Integer coupons;
        public Integer totalReferrals;
        public List<TreeNode> children = new ArrayList<>();
    }

    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TreeNode>> getTree(@RequestParam(required = false) String phone) {
        Optional<User> rootOpt;
        if (phone != null && !phone.isBlank()) {
            rootOpt = userRepo.findByPhone(phone);
        } else {
            // default: pick the oldest user as root to avoid empty
            rootOpt = userRepo.findAll().stream().min(Comparator.comparing(User::getId));
        }
        if (rootOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No root user found", null));
        }
        User root = rootOpt.get();
        TreeNode rootNode = buildTree(root, 0);
        return ResponseEntity.ok(ApiResponse.success("Referral tree fetched", rootNode));
    }

    private TreeNode buildTree(User user, int depth) {
        TreeNode node = new TreeNode();
        node.id = user.getId();
        node.phone = user.getPhone();
        node.masked = mask(user.getPhone());
        node.signupDate = user.getCreatedAt() != null ? user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        node.status = user.isEnabled() ? "active" : "inactive";
        node.coupons = user.getKuberCoupons();
        var directs = referralRepo.getDirectReferrals(user.getId());
        node.totalReferrals = directs.size();
        if (depth >= 6) return node;
        for (var rr : directs) {
            node.children.add(buildTree(rr.getUser(), depth + 1));
        }
        return node;
    }

    private String mask(String phone) {
        if (phone == null) return null;
        int n = phone.length();
        if (n <= 10) return "****";
        return "******" + phone.substring(n - 4);
    }
}
