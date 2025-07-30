package com.jackzhang144.bbs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bbs")
public class AppConfig {
    private String cookies;
    private String authToken;
    private String outputFile = "new_messages.json";
    private String notificationApiTemplate;
    private String notificationUserId;
    private int checkIntervalSeconds = 15;
    private boolean enabled = true;
    private boolean notificationEnabled = true;
} 