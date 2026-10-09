package com.wp;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Warranty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String serialNumber;
    public Long policyId;
    public LocalDate activationDate, expiryDate;
}
