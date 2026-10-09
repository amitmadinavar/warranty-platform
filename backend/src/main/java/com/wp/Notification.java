package com.wp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long recipientUserId;
    public String targetRole;
    public String type;
    public String title;
    @Column(length = 1000) public String message;
    public boolean readFlag = false;
    public LocalDateTime createdAt = LocalDateTime.now();
}
