package com.wp;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class WarrantyRepo extends JpaStore<Warranty> {
    public WarrantyRepo() { super(Warranty.class); }

    public Optional<Warranty> findBySerialNumber(String serialNumber) {
        return em.createQuery("select w from Warranty w where lower(w.serialNumber) = lower(:serial)", Warranty.class)
                .setParameter("serial", serialNumber)
                .getResultStream().findFirst();
    }
}
