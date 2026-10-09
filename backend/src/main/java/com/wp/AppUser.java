package com.wp;

import jakarta.persistence.*;

@Entity
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String name;
    @Column(nullable = false, unique = true) public String email;
    @Column(nullable = false) public String passwordHash;
    @Column(nullable = false) public String role = "ADMIN";
    public Long customerId;
}
