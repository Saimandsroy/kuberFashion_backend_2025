package com.kuberfashion.backend.controller;

import com.kuberfashion.backend.dto.ApiResponse;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173", "https://kuberfashions.in",
        "https://www.kuberfashions.in" })
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    /**
     * Initiate PhonePe payment for an order
     */
    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<Map<String, String>>> initiatePayment(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Long> request) {
        Long orderId = request.get("orderId");
        if (orderId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Order ID is required"));
        }

        try {
            Map<String, String> result = paymentService.initiatePayment(orderId);
            return ResponseEntity.ok(ApiResponse.success("Payment initiated successfully", result));
        } catch (Exception e) {
            logger.error("Payment initiation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Payment initiation failed: " + e.getMessage()));
        }
    }

    /**
     * Check payment status
     */
    @GetMapping("/status/{merchantOrderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPaymentStatus(
            @PathVariable String merchantOrderId) {
        try {
            Map<String, Object> status = paymentService.checkPaymentStatus(merchantOrderId);
            return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", status));
        } catch (Exception e) {
            logger.error("Error checking payment status: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error checking status: " + e.getMessage()));
        }
    }

    /**
     * PhonePe webhook/callback handler
     * This endpoint should be public (no authentication required)
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody String requestBody) {
        logger.info("Received PhonePe webhook");

        try {
            boolean success = paymentService.handleCallback(authorization, requestBody);
            if (success) {
                return ResponseEntity.ok("OK");
            } else {
                return ResponseEntity.badRequest().body("Callback validation failed");
            }
        } catch (Exception e) {
            logger.error("Webhook processing error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    /**
     * Manual payment confirmation (for testing/simulation)
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<String>> confirmPayment(
            @RequestBody Map<String, String> request) {
        String merchantOrderId = request.get("merchantOrderId");
        String transactionId = request.getOrDefault("transactionId", "SIM_" + System.currentTimeMillis());

        if (merchantOrderId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Merchant order ID is required"));
        }

        try {
            paymentService.markPaymentSuccess(merchantOrderId, transactionId);
            return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully"));
        } catch (Exception e) {
            logger.error("Payment confirmation failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Payment confirmation failed: " + e.getMessage()));
        }
    }

    /**
     * Manual payment failure (for testing/simulation)
     */
    @PostMapping("/fail")
    public ResponseEntity<ApiResponse<String>> failPayment(
            @RequestBody Map<String, String> request) {
        String merchantOrderId = request.get("merchantOrderId");
        String errorCode = request.getOrDefault("errorCode", "USER_CANCELLED");
        String errorMessage = request.getOrDefault("errorMessage", "Payment was cancelled by user");

        if (merchantOrderId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Merchant order ID is required"));
        }

        try {
            paymentService.markPaymentFailed(merchantOrderId, errorCode, errorMessage);
            return ResponseEntity.ok(ApiResponse.success("Payment marked as failed"));
        } catch (Exception e) {
            logger.error("Payment failure marking failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Operation failed: " + e.getMessage()));
        }
    }
}
