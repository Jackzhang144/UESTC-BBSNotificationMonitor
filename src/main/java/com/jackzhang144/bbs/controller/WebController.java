package com.jackzhang144.bbs.controller;

import com.jackzhang144.bbs.config.AppConfig;
import com.jackzhang144.bbs.model.BbsMessage;
import com.jackzhang144.bbs.service.BbsMonitorService;
import com.jackzhang144.bbs.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class WebController {
    
    @Autowired
    private BbsMonitorService bbsMonitorService;
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private AppConfig appConfig;
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("config", appConfig);
        model.addAttribute("messageCount", bbsMonitorService.getMessageCount());
        return "index";
    }
    
    @GetMapping("/messages")
    public String messages(Model model) {
        List<BbsMessage> messages = bbsMonitorService.getAllMessages();
        model.addAttribute("messages", messages);
        return "messages";
    }
    
    @GetMapping("/logs")
    public String logs(Model model) {
        model.addAttribute("logs", logService.getLogs());
        return "logs";
    }
    
    @GetMapping("/config")
    public String config(Model model) {
        model.addAttribute("config", appConfig);
        return "config";
    }
    
        @PostMapping("/config/update")
    @ResponseBody
    public String updateConfig(@ModelAttribute AppConfig newConfig) {
        try {
            // 更新配置
            appConfig.setCookies(newConfig.getCookies());
            appConfig.setAuthToken(newConfig.getAuthToken());
            appConfig.setOutputFile(newConfig.getOutputFile());
            appConfig.setNotificationApiTemplate(newConfig.getNotificationApiTemplate());
            appConfig.setNotificationUserId(newConfig.getNotificationUserId());
            appConfig.setCheckIntervalSeconds(newConfig.getCheckIntervalSeconds());
            appConfig.setEnabled(newConfig.isEnabled());
            appConfig.setNotificationEnabled(newConfig.isNotificationEnabled());

            bbsMonitorService.updateConfig(appConfig);
            logService.addLog("配置已更新");

            return "success";
        } catch (Exception e) {
            logService.addLog("配置更新失败: " + e.getMessage());
            return "error: " + e.getMessage();
        }
    }
    
    @PostMapping("/logs/clear")
    @ResponseBody
    public String clearLogs() {
        logService.clearLogs();
        return "success";
    }
    
    @GetMapping("/api/messages")
    @ResponseBody
    public List<BbsMessage> getMessages() {
        return bbsMonitorService.getAllMessages();
    }
    
    @GetMapping("/api/logs")
    @ResponseBody
    public List<LogService.LogEntry> getLogs() {
        return logService.getLogs();
    }
    
    @GetMapping("/api/stats")
    @ResponseBody
    public Object getStats() {
        return new Object() {
            public final int messageCount = bbsMonitorService.getMessageCount();
            public final int logCount = logService.getLogs().size();
            public final boolean enabled = appConfig.isEnabled();
        };
    }
} 