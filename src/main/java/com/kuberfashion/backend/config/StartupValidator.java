package com.kuberfashion.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Validates critical configuration and dependencies during application startup.
 * Runs before DataInitializer to ensure all prerequisites are met.
 */
//@Component
@Order(1) // Run before DataInitializer (which has default order)
public class StartupValidator implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupValidator.class);
    private static final int DB_MAX_RETRIES = 5;
    private static final long DB_RETRY_BACKOFF_MS = 2000L; // 2s base, grows linearly

    private final DataSource dataSource;

    @Value("${DATABASE_URL:#{null}}")
    private String databaseUrl;

    @Value("${JWT_SECRET:#{null}}")
    private String jwtSecret;

    @Value("${spring.datasource.url}")
    private String springDatasourceUrl;

    @Value("${jwt.secret}")
    private String springJwtSecret;

    public StartupValidator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("🔍 Starting application validation...");

        try {
            validateEnvironmentVariables();
            validateDatabaseConnection();
            validateJwtConfiguration();
            
            logger.info("✅ All startup validations passed successfully!");
            
        } catch (Exception e) {
            logger.error("❌ Startup validation failed: {}", e.getMessage());
            logger.error("🚨 Application cannot start safely. Please fix the configuration issues above.");
            throw new RuntimeException("Startup validation failed", e);
        }
    }

    private void validateEnvironmentVariables() {
        logger.info("📋 Validating environment variables...");

        // Check critical environment variables
        if (springDatasourceUrl == null || springDatasourceUrl.trim().isEmpty()) {
            throw new RuntimeException("DATABASE_URL is not configured. Please set the DATABASE_URL environment variable.");
        }

        if (springJwtSecret == null || springJwtSecret.trim().isEmpty()) {
            throw new RuntimeException("JWT_SECRET is not configured. Please set the JWT_SECRET environment variable.");
        }

        // Validate JWT secret strength
        if (springJwtSecret.length() < 32) {
            logger.warn("⚠️  JWT_SECRET is shorter than recommended (32+ characters). Consider using a stronger secret.");
        }

        // Log configuration status (without exposing sensitive data)
        logger.info("✅ Database URL configured: {}", maskSensitiveUrl(springDatasourceUrl));
        logger.info("✅ JWT Secret configured: {} characters", springJwtSecret.length());
        
        // Log environment variable usage
        if (databaseUrl != null) {
            logger.info("✅ Using DATABASE_URL environment variable");
        } else {
            logger.info("ℹ️  Using default database configuration");
        }

        if (jwtSecret != null) {
            logger.info("✅ Using JWT_SECRET environment variable");
        } else {
            logger.info("ℹ️  Using default JWT secret (not recommended for production)");
        }
    }

    private void validateDatabaseConnection() {
        logger.info("🔌 Validating database connection...");

        SQLException lastException = null;
        for (int attempt = 1; attempt <= DB_MAX_RETRIES; attempt++) {
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(10)) {
                    logger.info("✅ Database connection successful (attempt {}/{})", attempt, DB_MAX_RETRIES);
                    logger.info("📊 Database URL: {}", maskSensitiveUrl(connection.getMetaData().getURL()));
                    logger.info("📊 Database Product: {} {}",
                        connection.getMetaData().getDatabaseProductName(),
                        connection.getMetaData().getDatabaseProductVersion());
                    return;
                } else {
                    throw new SQLException("Database connection is not valid");
                }
            } catch (SQLException e) {
                lastException = e;
                String msg = e.getMessage() == null ? "unknown" : e.getMessage();
                logger.warn("⚠️  DB connection attempt {}/{} failed: {}", attempt, DB_MAX_RETRIES, msg);

                if (attempt < DB_MAX_RETRIES) {
                    try {
                        Thread.sleep(DB_RETRY_BACKOFF_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // Provide helpful error messages for common issues
        String errorMessage = lastException != null && lastException.getMessage() != null
            ? lastException.getMessage().toLowerCase() : "";
        if (errorMessage.contains("connection refused")) {
            throw new RuntimeException("Database connection refused. Please check if the database server is running and accessible.", lastException);
        } else if (errorMessage.contains("authentication failed") || errorMessage.contains("password authentication")) {
            throw new RuntimeException("Database authentication failed. Please verify DATABASE_USERNAME and DATABASE_PASSWORD.", lastException);
        } else if (errorMessage.contains("no pg_hba.conf entry")) {
            throw new RuntimeException("Access denied by PostgreSQL (pg_hba.conf). Ensure your DB allows connections from this host.", lastException);
        } else if (errorMessage.contains("does not exist")) {
            throw new RuntimeException("Database or schema does not exist. Run initial schema creation (set HIBERNATE_DDL_AUTO=update once).", lastException);
        } else if (errorMessage.contains("ssl") || errorMessage.contains("handshake")) {
            throw new RuntimeException("SSL/TLS error. Ensure DATABASE_URL includes '?sslmode=require' for Supabase.", lastException);
        } else if (errorMessage.contains("timeout")) {
            throw new RuntimeException("Database connection timeout. Please check network connectivity and db server status.", lastException);
        }

        throw new RuntimeException("Unable to establish database connection after " + DB_MAX_RETRIES + " attempts: "
            + (lastException != null ? lastException.getMessage() : "unknown error"), lastException);
    }

    private void validateJwtConfiguration() {
        logger.info("🔐 Validating JWT configuration...");

        // Basic JWT secret validation
        if (springJwtSecret.equals("your-jwt-secret-key-here") || 
            springJwtSecret.equals("dev-secret-key-not-for-production")) {
            logger.warn("⚠️  Using default/example JWT secret. Please set a secure JWT_SECRET for production!");
        }

        logger.info("✅ JWT configuration validated");
    }

    /**
     * Masks sensitive information in URLs for logging
     */
    private String maskSensitiveUrl(String url) {
        if (url == null) return "null";
        
        // Mask password in JDBC URLs
        return url.replaceAll("password=[^&;]+", "password=***")
                 .replaceAll("://[^:]+:[^@]+@", "://***:***@");
    }
}
