package com.kuberfashion.backend.controller;

import com.kuberfashion.backend.dto.ApiResponse;
import com.kuberfashion.backend.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/storage")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AdminStorageController {

    @Autowired
    private SupabaseStorageService storageService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categorySlug", required = false, defaultValue = "general") String categorySlug,
            @RequestParam(value = "filename", required = false) String filename) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only image files are allowed"));
            }

            // Generate unique filename if not provided
            if (filename == null || filename.isBlank()) {
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename != null && originalFilename.contains(".") 
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
                filename = UUID.randomUUID().toString() + extension;
            }

            // Upload to Supabase
            String publicUrl = storageService.uploadPublic(
                categorySlug,
                filename,
                file.getBytes(),
                contentType
            );

            // Return response
            Map<String, String> data = new HashMap<>();
            data.put("publicUrl", publicUrl);
            data.put("filename", filename);
            data.put("categorySlug", categorySlug);

            return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", data));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to read file: " + e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Storage configuration error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-multiple")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "categorySlug", required = false, defaultValue = "general") String categorySlug) {
        
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No files provided"));
            }

            java.util.List<String> urls = new java.util.ArrayList<>();
            
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("image/")) {
                        String originalFilename = file.getOriginalFilename();
                        String extension = originalFilename != null && originalFilename.contains(".") 
                            ? originalFilename.substring(originalFilename.lastIndexOf("."))
                            : ".jpg";
                        String filename = UUID.randomUUID().toString() + extension;

                        String publicUrl = storageService.uploadPublic(
                            categorySlug,
                            filename,
                            file.getBytes(),
                            contentType
                        );
                        urls.add(publicUrl);
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("urls", urls);
            data.put("count", urls.size());

            return ResponseEntity.ok(ApiResponse.success("Images uploaded successfully", data));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }
}
