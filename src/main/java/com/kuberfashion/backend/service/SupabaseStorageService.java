package com.kuberfashion.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    @Value("${SUPABASE_SERVICE_KEY:}")
    private String serviceKey;

    public String uploadPublic(String categorySlug, String filename, byte[] bytes, String contentType) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("SUPABASE_SERVICE_KEY is not configured");
        }
        String path = "products/" + categorySlug + "/" + filename;
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.set("apikey", serviceKey);
        headers.set("x-upsert", "true");
        headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE));

        HttpEntity<byte[]> entity = new HttpEntity<>(bytes, headers);
        ResponseEntity<String> resp = rest.exchange(uploadUrl, HttpMethod.PUT, entity, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Upload failed with status: " + resp.getStatusCode());
        }

        // Public URL format (bucket must be public)
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}
