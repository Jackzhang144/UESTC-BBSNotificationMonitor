package com.jackzhang144.bbs.service;

import com.jackzhang144.bbs.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class NotificationService {
    
    @Autowired
    private AppConfig appConfig;
    
    private final OkHttpClient client = new OkHttpClient();
    
    public void sendNotification(String author, String subject) {
        try {
            // 构建POST请求的URL（不包含消息内容在URL中）
            String baseUrl = "http://api.chuckfang.com/" + appConfig.getNotificationUserId() + "/BBS消息通知";
            
            // 构建消息内容
            String message = author + "回复了您的帖子\"" + subject + "\"";
            
            log.info("发送通知到: {}, 消息内容: {}", baseUrl, message);
            
            // 构建POST请求体
            RequestBody requestBody = RequestBody.create(
                message, 
                MediaType.parse("text/plain; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .post(requestBody)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("通知发送结果: {} 回复了「{}」, 状态码: {}, 响应: {}", 
                        author, subject, response.code(), responseBody);
                
                if (!response.isSuccessful()) {
                    log.error("通知发送失败，状态码: {}, 响应: {}", response.code(), responseBody);
                }
            }
        } catch (Exception e) {
            log.error("发送通知失败: " + e.getMessage(), e);
        }
    }
} 