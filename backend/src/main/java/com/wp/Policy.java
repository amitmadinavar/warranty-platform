package com.wp;

import jakarta.persistence.*;

@Entity
public class Policy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name, category, model;
    public int durationMonths;
    public String exclusions;
    public int replaceAfterRepairs = 3;
    public boolean active = true;
}
