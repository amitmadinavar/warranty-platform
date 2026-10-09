package com.wp;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Replacement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long requestId;
    public String originalSerial, replacementSerial, reason;
    public LocalDate replacedOn = LocalDate.now();
}
