package com.wp;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String serialNumber;
    public String model, category;
    public LocalDate purchaseDate;
    public Long customerId;
}
