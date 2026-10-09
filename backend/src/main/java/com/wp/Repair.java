package com.wp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Repair {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long requestId;
    public String diagnosis, action, parts;
    public double laborCost;
    public LocalDateTime completedAt = LocalDateTime.now();
}
