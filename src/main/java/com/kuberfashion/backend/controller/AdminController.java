package com.kuberfashion.backend.controller;

import com.kuberfashion.backend.dto.ApiResponse;
import com.kuberfashion.backend.dto.UserResponseDto;
import com.kuberfashion.backend.dto.PagedResponse;
import com.kuberfashion.backend.entity.Order;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.repository.OrderRepository;
import com.kuberfashion.backend.repository.UserRepository;
import com.kuberfashion.backend.service.SupabaseStorageService;
import com.kuberfashion.backend.service.UserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SupabaseStorageService storageService;

    // Users
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String q
    ) {
        Pageable pageable = PageRequest.of(page, size);
        User.Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            roleEnum = User.Role.valueOf(role);
        }
        Page<User> p = userRepository.findAllFiltered(roleEnum, enabled, (q == null || q.isBlank()) ? null : q, pageable);
        List<UserResponseDto> dtos = p.getContent().stream().map(UserResponseDto::new).collect(Collectors.toList());
        PagedResponse<UserResponseDto> out = new PagedResponse<>(dtos, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success("Users fetched", out));
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        userService.updateUserStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(enabled ? "User enabled" : "User disabled"));
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        userService.updateUserRole(id, User.Role.valueOf(role));
        return ResponseEntity.ok(ApiResponse.success("Role updated"));
    }

    // Storage uploads (public bucket)
    @PostMapping(value = "/storage/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("categorySlug") @NotBlank String categorySlug,
            @RequestParam("filename") @NotBlank String filename
    ) throws IOException {
        String publicUrl = storageService.uploadPublic(categorySlug, filename, file.getBytes(), file.getContentType());
        return ResponseEntity.ok(ApiResponse.success("Uploaded", Map.of("publicUrl", publicUrl)));
    }

    // Orders listing with simple optional filters
    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<OrderAdminResponseDto>>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Order.OrderStatus st = null;
        if (status != null && !status.isBlank()) {
            st = Order.OrderStatus.valueOf(status);
        }
        Page<Order> p = orderRepository.findAllByFilters(st, (email == null || email.isBlank()) ? null : email, pageable);
        List<OrderAdminResponseDto> dtos = p.getContent().stream().map(OrderAdminResponseDto::from).collect(Collectors.toList());
        PagedResponse<OrderAdminResponseDto> out = new PagedResponse<>(dtos, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        return ResponseEntity.ok(ApiResponse.success("Orders fetched", out));
    }

    @PutMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(Order.OrderStatus.valueOf(status));
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return ResponseEntity.ok(ApiResponse.success("Order status updated"));
    }

    // DTO for admin order list
    public static class OrderAdminResponseDto {
        public Long id;
        public String orderNumber;
        public String status;
        public BigDecimal totalAmount;
        public LocalDateTime createdAt;
        public String userEmail;

        public static OrderAdminResponseDto from(Order o) {
            OrderAdminResponseDto dto = new OrderAdminResponseDto();
            dto.id = o.getId();
            dto.orderNumber = o.getOrderNumber();
            dto.status = o.getStatus() != null ? o.getStatus().name() : null;
            dto.totalAmount = o.getTotalAmount();
            dto.createdAt = o.getCreatedAt();
            dto.userEmail = o.getUser() != null ? o.getUser().getEmail() : null;
            return dto;
        }
    }
}
