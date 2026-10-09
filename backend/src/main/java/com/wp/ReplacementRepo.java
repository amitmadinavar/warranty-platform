package com.wp;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReplacementRepo extends JpaStore<Replacement> {
    public ReplacementRepo() { super(Replacement.class); }

    public List<Replacement> findByOriginalSerial(String originalSerial) {
        return em.createQuery("select r from Replacement r where lower(r.originalSerial) = lower(:serial) order by r.id desc", Replacement.class)
                .setParameter("serial", originalSerial)
                .getResultList();
    }
}
