package com.kuberfashion.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EnvironmentConfig {

    @PostConstruct
    public void loadEnvironmentVariables() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            // Set system properties from .env file for Spring Boot to pick up
            dotenv.entries().forEach(entry -> {
                // Set both system property and environment variable style
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
                // Also set as environment variable for Spring Boot
                System.setProperty("spring.config.import", "optional:file:.env[.properties]");
            });

            System.out.println("Environment variables loaded from .env file");
            System.out.println("DATABASE_URL: " + (dotenv.get("DATABASE_URL") != null ? "SET" : "NOT SET"));
            System.out.println("DATABASE_USERNAME: " + (dotenv.get("DATABASE_USERNAME") != null ? "SET" : "NOT SET"));
            System.out.println("DATABASE_PASSWORD: " + (dotenv.get("DATABASE_PASSWORD") != null ? "SET" : "NOT SET"));
        } catch (Exception e) {
            System.err.println("Failed to load .env file: " + e.getMessage());
            e.printStackTrace();
            // Don't fail the application if .env file is missing
        }
    }
}
