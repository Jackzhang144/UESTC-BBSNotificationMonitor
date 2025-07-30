package com.jackzhang144.bbs.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BbsMessage {
    private Long id;
    private String author;
    private String subject;
    private String htmlMessage;
    private Long dateline;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fetchTime;
    
    private String threadId;
    private String postId;
    private String authorId;
    private String userId;
    private String type;
    private boolean unread;
    private String category;
    private String kind;
} 