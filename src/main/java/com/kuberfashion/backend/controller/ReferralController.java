package com.kuberfashion.backend.controller;

import com.kuberfashion.backend.dto.ApiResponse;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.service.ReferralService;
import com.kuberfashion.backend.service.ReferralService.ReferralStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/referral")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "https://kuberfashions.in", "https://www.kuberfashions.in"})
public class ReferralController {

    @Autowired
    private ReferralService referralService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ReferralStats>> getStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        ReferralStats stats = referralService.getReferralStats(user);
        return ResponseEntity.ok(ApiResponse.success("Referral stats retrieved", stats));
    }

    @GetMapping("/code")
    public ResponseEntity<ApiResponse<String>> getCode(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        String code = user.getPhone(); // use phone as referral code
        return ResponseEntity.ok(ApiResponse.success("Referral code retrieved", code));
    }
}
