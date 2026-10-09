package com.wp;

import jakarta.persistence.*;

@Entity
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String name, email, phone;
}
