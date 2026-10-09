package com.wp;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RequestRepo extends JpaStore<ServiceRequest> {
    public RequestRepo() { super(ServiceRequest.class); }

    public List<ServiceRequest> findBySerialNumber(String serialNumber) {
        return em.createQuery("select r from ServiceRequest r where lower(r.serialNumber) = lower(:serial) order by r.createdAt desc", ServiceRequest.class)
                .setParameter("serial", serialNumber)
                .getResultList();
    }

    public List<ServiceRequest> findByAssignedToIgnoreCase(String assignedTo) {
        return em.createQuery("select r from ServiceRequest r where lower(r.assignedTo) = lower(:assignedTo) order by r.createdAt desc", ServiceRequest.class)
                .setParameter("assignedTo", assignedTo)
                .getResultList();
    }
}
