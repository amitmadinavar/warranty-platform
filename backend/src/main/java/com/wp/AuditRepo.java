package com.wp;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AuditRepo extends JpaStore<Audit> {
    public AuditRepo() { super(Audit.class); }

    public List<Audit> findAllByOrderByAtDesc() {
        return em.createQuery("select a from Audit a order by a.at desc, a.id desc", Audit.class)
                .getResultList();
    }
}
