package com.kuberfashion.backend.service;

import com.kuberfashion.backend.config.CloudflareR2Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Autowired
    private S3Client s3Client;

    @Autowired
    private CloudflareR2Config r2Config;

    @Value("${cloudflare.r2.public-url:}")
    private String publicUrl;

    /**
     * Upload file to Cloudflare R2
     * @param file The file to upload
     * @param folder The folder path (e.g., "products", "users", "categories")
     * @return The public URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String filename = folder + "/" + timestamp + "_" + uniqueId + extension;

            // Upload to R2
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(filename)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            PutObjectResponse response = s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            logger.info("File uploaded successfully: {} (ETag: {})", filename, response.eTag());

            // Return public URL
            return getPublicUrl(filename);

        } catch (IOException e) {
            logger.error("Failed to upload file: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    /**
     * Delete file from Cloudflare R2
     * @param fileUrl The public URL of the file to delete
     */
    public void deleteFile(String fileUrl) {
        try {
            String filename = extractFilenameFromUrl(fileUrl);
            if (filename != null) {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(r2Config.getBucketName())
                        .key(filename)
                        .build();

                s3Client.deleteObject(deleteObjectRequest);
                logger.info("File deleted successfully: {}", filename);
            }
        } catch (Exception e) {
            logger.error("Failed to delete file: {}", e.getMessage());
            // Don't throw exception for delete failures
        }
    }

    /**
     * Check if file exists in R2
     * @param fileUrl The public URL of the file
     * @return true if file exists
     */
    public boolean fileExists(String fileUrl) {
        try {
            String filename = extractFilenameFromUrl(fileUrl);
            if (filename == null) return false;

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(filename)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Error checking file existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get file metadata
     * @param fileUrl The public URL of the file
     * @return File metadata
     */
    public FileMetadata getFileMetadata(String fileUrl) {
        try {
            String filename = extractFilenameFromUrl(fileUrl);
            if (filename == null) return null;

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(filename)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            
            return new FileMetadata(
                    filename,
                    response.contentType(),
                    response.contentLength(),
                    response.lastModified()
            );
        } catch (Exception e) {
            logger.error("Error getting file metadata: {}", e.getMessage());
            return null;
        }
    }

    private String getPublicUrl(String filename) {
        if (publicUrl != null && !publicUrl.isEmpty()) {
            return publicUrl + "/" + filename;
        }
        // Fallback to R2 direct URL (if bucket is public)
        return "https://pub-" + r2Config.getBucketName() + ".r2.dev/" + filename;
    }

    private String extractFilenameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return null;
        
        try {
            // Extract filename from URL
            if (fileUrl.contains("/")) {
                String[] parts = fileUrl.split("/");
                // Find the part that contains the folder structure
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].matches("\\d{8}_\\d{6}_[a-f0-9]{8}.*")) {
                        // Reconstruct the path from folder onwards
                        StringBuilder path = new StringBuilder();
                        for (int j = i - 1; j < parts.length; j++) {
                            if (j > i - 1) path.append("/");
                            path.append(parts[j]);
                        }
                        return path.toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error extracting filename from URL: {}", e.getMessage());
            return null;
        }
    }

    // Inner class for file metadata
    public static class FileMetadata {
        private final String filename;
        private final String contentType;
        private final Long size;
        private final java.time.Instant lastModified;

        public FileMetadata(String filename, String contentType, Long size, java.time.Instant lastModified) {
            this.filename = filename;
            this.contentType = contentType;
            this.size = size;
            this.lastModified = lastModified;
        }

        // Getters
        public String getFilename() { return filename; }
        public String getContentType() { return contentType; }
        public Long getSize() { return size; }
        public java.time.Instant getLastModified() { return lastModified; }
    }
}
