package com.wp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Audit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String entity, action;
    public Long entityId;
    @Column(length = 1000) public String detail;
    public LocalDateTime at = LocalDateTime.now();
}
