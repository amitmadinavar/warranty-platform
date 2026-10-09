package com.wp;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RepairRepo extends JpaStore<Repair> {
    public RepairRepo() { super(Repair.class); }

    public List<Repair> findByRequestId(Long requestId) {
        return em.createQuery("select r from Repair r where r.requestId = :requestId order by r.id desc", Repair.class)
                .setParameter("requestId", requestId)
                .getResultList();
    }
}
