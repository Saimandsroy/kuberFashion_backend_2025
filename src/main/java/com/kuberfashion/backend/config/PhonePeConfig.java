package com.kuberfashion.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * PhonePe Payment Gateway Configuration
 * 
 * Configure these values in your .env file:
 * PHONEPE_CLIENT_ID=your_client_id
 * PHONEPE_CLIENT_SECRET=your_client_secret
 * PHONEPE_CLIENT_VERSION=1
 * PHONEPE_ENVIRONMENT=SANDBOX or PRODUCTION
 * PHONEPE_REDIRECT_URL=https://yourdomain.com/payment/status
 * PHONEPE_WEBHOOK_USERNAME=webhook_username
 * PHONEPE_WEBHOOK_PASSWORD=webhook_password
 */
@Configuration
public class PhonePeConfig {

    @Value("${phonepe.client-id:}")
    private String clientId;

    @Value("${phonepe.client-secret:}")
    private String clientSecret;

    @Value("${phonepe.client-version:1}")
    private String clientVersion;

    @Value("${phonepe.environment:SANDBOX}")
    private String environment;

    @Value("${phonepe.redirect-url:http://localhost:5173/payment/status}")
    private String redirectUrl;

    @Value("${phonepe.webhook-username:}")
    private String webhookUsername;

    @Value("${phonepe.webhook-password:}")
    private String webhookPassword;

    // Getters
    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getWebhookUsername() {
        return webhookUsername;
    }

    public String getWebhookPassword() {
        return webhookPassword;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty();
    }

    public boolean isSandbox() {
        return "SANDBOX".equalsIgnoreCase(environment);
    }
}
