package com.wp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ServiceRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String serialNumber;
    @Column(length = 1000) public String issue;
    public String status = "REGISTERED", assignedTo, eligibility, path;
    @Column(length = 1000) public String reason;
    public boolean escalated;
    public LocalDateTime createdAt = LocalDateTime.now(), resolvedAt;
}
