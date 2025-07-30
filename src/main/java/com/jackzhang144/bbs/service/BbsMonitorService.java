package com.jackzhang144.bbs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackzhang144.bbs.config.AppConfig;
import com.jackzhang144.bbs.model.BbsMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class BbsMonitorService {
    
    @Autowired
    private AppConfig appConfig;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private LogService logService;
    
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    
    // 使用内存缓存提高性能
    private final Set<Long> existingDatelines = ConcurrentHashMap.newKeySet();
    private final List<BbsMessage> allMessages = new CopyOnWriteArrayList<>();
    private boolean isFirstRun = true;
    private boolean isInitialLoad = true; // 标记是否为初始加载
    
                               @Scheduled(fixedDelayString = "${bbs.check-interval-seconds:15}000")
    public void checkNewMessages() {
        if (!appConfig.isEnabled()) {
            return;
        }
        
        try {
            logService.addLog("开始检查新消息...");
            
            // 首次运行加载历史数据
            if (isFirstRun) {
                loadExistingMessages();
                isFirstRun = false;
            }
            
            // 获取最新数据
            Map<String, Object> data = fetchNotifications();
            if (data == null || !data.containsKey("data")) {
                logService.addLog("⚠️ 获取数据失败");
                return;
            }
            
            // 处理新消息
            List<BbsMessage> newMessages = processMessages(data);
            
            if (newMessages.isEmpty()) {
                logService.addLog("🔄 本次未发现新消息");
                return;
            }
            
            logService.addLog("📨 发现 " + newMessages.size() + " 条新回复");
            
            // 发送通知并保存
            for (BbsMessage msg : newMessages) {
                if (appConfig.isNotificationEnabled()) {
                    notificationService.sendNotification(msg.getAuthor(), msg.getSubject());
                    logService.addLog("✉️ 已发送通知: " + msg.getAuthor() + " 回复了「" + msg.getSubject() + "」");
                } else {
                    logService.addLog("📝 记录消息: " + msg.getAuthor() + " 回复了「" + msg.getSubject() + "」（通知已关闭）");
                }
            }
            
            saveMessages(newMessages);
            logService.addLog("💾 已保存 " + newMessages.size() + " 条新消息");
            
        } catch (Exception e) {
            logService.addLog("❌ 检查新消息时发生异常: " + e.getMessage());
            log.error("检查新消息时发生异常", e);
        }
    }
    
    private void loadExistingMessages() {
        File file = new File(appConfig.getOutputFile());
        if (!file.exists() || file.length() == 0) {
            logService.addLog("🆕 首次运行，无历史数据");
            // 首次运行时，获取所有历史消息但不推送通知
            loadAllHistoricalMessages();
            return;
        }
        
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(appConfig.getOutputFile()));
            List<Map<String, Object>> messages = objectMapper.readValue(jsonData, new TypeReference<List<Map<String, Object>>>() {});
            
            for (Map<String, Object> msgData : messages) {
                BbsMessage msg = convertToBbsMessage(msgData);
                if (msg.getDateline() != null) {
                    existingDatelines.add(msg.getDateline());
                    allMessages.add(msg);
                }
            }
            
            logService.addLog("📚 加载了 " + messages.size() + " 条历史消息");
        } catch (Exception e) {
            logService.addLog("❌ 加载历史消息失败: " + e.getMessage());
            logService.addLog("🔄 将重新创建消息文件");
            log.error("加载历史消息失败", e);
            
            // 备份损坏的文件
            try {
                File backupFile = new File(appConfig.getOutputFile() + ".backup." + System.currentTimeMillis());
                Files.copy(Paths.get(appConfig.getOutputFile()), backupFile.toPath());
                logService.addLog("💾 已备份损坏的文件: " + backupFile.getName());
            } catch (Exception backupException) {
                log.error("备份损坏文件失败", backupException);
            }
            
            // 清空内存缓存，重新开始
            existingDatelines.clear();
            allMessages.clear();
            
            // 重新获取所有历史消息
            loadAllHistoricalMessages();
        }
    }
    
    private void loadAllHistoricalMessages() {
        try {
            logService.addLog("🔄 正在获取所有历史消息...");
            
            // 获取所有历史消息（多页）
            List<BbsMessage> allHistoricalMessages = new ArrayList<>();
            int page = 1;
            boolean hasMore = true;
            
            while (hasMore && page <= 10) { // 最多获取10页，避免无限循环
                Map<String, Object> data = fetchNotificationsByPage(page);
                if (data == null || !data.containsKey("data")) {
                    break;
                }
                
                List<BbsMessage> pageMessages = processMessages(data);
                if (pageMessages.isEmpty()) {
                    hasMore = false;
                } else {
                    allHistoricalMessages.addAll(pageMessages);
                    page++;
                }
            }
            
            // 保存所有历史消息
            if (!allHistoricalMessages.isEmpty()) {
                allMessages.addAll(allHistoricalMessages);
                allMessages.sort((a, b) -> Long.compare(b.getDateline(), a.getDateline()));
                
                // 写入文件
                objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValue(new File(appConfig.getOutputFile()), allMessages);
                
                logService.addLog("📚 已获取并保存 " + allHistoricalMessages.size() + " 条历史消息（启动时不推送通知）");
            }
            
        } catch (Exception e) {
            logService.addLog("❌ 获取历史消息失败: " + e.getMessage());
            log.error("获取历史消息失败", e);
        }
    }
    
    private Map<String, Object> fetchNotificationsByPage(int page) throws IOException {
        String url = "https://bbs.uestc.edu.cn/star/api/v1/messages/notifications?kind=reply&page=" + page;
        
        Headers headers = new Headers.Builder()
                .add("Accept", "application/json, text/plain, */*")
                .add("Referer", "https://bbs.uestc.edu.cn/messages/posts")
                .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36 Edg/139.0.0.0")
                .add("Origin", "https://bbs.uestc.edu.cn")
                .add("X-Requested-With", "XMLHttpRequest")
                .add("Authorization", appConfig.getAuthToken())
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .addHeader("Cookie", appConfig.getCookies())
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }
            return null;
        }
    }
    
    private Map<String, Object> fetchNotifications() throws IOException {
        String url = "https://bbs.uestc.edu.cn/star/api/v1/messages/notifications?kind=reply&page=1";
        
        Headers headers = new Headers.Builder()
                .add("Accept", "application/json, text/plain, */*")
                .add("Referer", "https://bbs.uestc.edu.cn/messages/posts")
                .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36 Edg/139.0.0.0")
                .add("Origin", "https://bbs.uestc.edu.cn")
                .add("X-Requested-With", "XMLHttpRequest")
                .add("Authorization", appConfig.getAuthToken())
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                .addHeader("Cookie", appConfig.getCookies())
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            }
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<BbsMessage> processMessages(Map<String, Object> data) {
        List<BbsMessage> newMessages = new ArrayList<>();
        
        Map<String, Object> dataData = (Map<String, Object>) data.get("data");
        if (dataData == null || !dataData.containsKey("rows")) {
            return newMessages;
        }
        
        List<Map<String, Object>> rows = (List<Map<String, Object>>) dataData.get("rows");
        
        for (Map<String, Object> msgData : rows) {
            Long dateline = getLongValue(msgData, "dateline");
            if (dateline != null && !existingDatelines.contains(dateline)) {
                BbsMessage msg = convertToBbsMessage(msgData);
                msg.setFetchTime(LocalDateTime.now());
                newMessages.add(msg);
                existingDatelines.add(dateline);
            }
        }
        
        return newMessages;
    }
    
    private BbsMessage convertToBbsMessage(Map<String, Object> msgData) {
        BbsMessage msg = new BbsMessage();
        msg.setId(getLongValue(msgData, "id"));
        msg.setAuthor(getStringValue(msgData, "author"));
        msg.setSubject(getStringValue(msgData, "subject"));
        msg.setHtmlMessage(getStringValue(msgData, "html_message"));
        msg.setDateline(getLongValue(msgData, "dateline"));
        msg.setThreadId(getStringValue(msgData, "thread_id"));
        msg.setPostId(getStringValue(msgData, "post_id"));
        msg.setAuthorId(getStringValue(msgData, "author_id"));
        msg.setUserId(getStringValue(msgData, "user_id"));
        msg.setType(getStringValue(msgData, "type"));
        msg.setUnread(getBooleanValue(msgData, "unread"));
        msg.setCategory(getStringValue(msgData, "category"));
        msg.setKind(getStringValue(msgData, "kind"));
        return msg;
    }
    
    private void saveMessages(List<BbsMessage> newMessages) {
        try {
            allMessages.addAll(newMessages);
            
            // 按时间戳排序
            allMessages.sort((a, b) -> Long.compare(b.getDateline(), a.getDateline()));
            
            // 写入文件
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(appConfig.getOutputFile()), allMessages);
        } catch (Exception e) {
            logService.addLog("❌ 保存消息失败: " + e.getMessage());
            log.error("保存消息失败", e);
        }
    }
    
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
    
    public List<BbsMessage> getAllMessages() {
        return new ArrayList<>(allMessages);
    }
    
    public int getMessageCount() {
        return allMessages.size();
    }
    
    public void updateConfig(AppConfig newConfig) {
        this.appConfig = newConfig;
        logService.addLog("⚙️ 配置已更新");
    }
} 