package com.jackzhang144.bbs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class LogService {
    
    private final CopyOnWriteArrayList<LogEntry> logs = new CopyOnWriteArrayList<>();
    private static final int MAX_LOGS = 1000;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public void addLog(String message) {
        LogEntry entry = new LogEntry(LocalDateTime.now(), message);
        logs.add(entry);
        
        // 限制日志数量
        if (logs.size() > MAX_LOGS) {
            logs.remove(0);
        }
        
        log.info(message);
    }
    
    public CopyOnWriteArrayList<LogEntry> getLogs() {
        return new CopyOnWriteArrayList<>(logs);
    }
    
    public void clearLogs() {
        logs.clear();
    }
    
    public static class LogEntry {
        private final LocalDateTime timestamp;
        private final String message;
        
        public LogEntry(LocalDateTime timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getFormattedTimestamp() {
            return timestamp.format(formatter);
        }
    }
} 