package com.wp;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationRepo extends JpaStore<Notification> {
    public NotificationRepo() { super(Notification.class); }

    public List<Notification> findTop50ByRecipientUserIdOrTargetRoleOrderByCreatedAtDesc(Long userId, String role) {
        return em.createQuery("select n from Notification n where n.recipientUserId = :userId or lower(n.targetRole) = lower(:role) order by n.createdAt desc, n.id desc", Notification.class)
                .setParameter("userId", userId)
                .setParameter("role", role)
                .setMaxResults(50)
                .getResultList();
    }

    public List<Notification> findTop50ByRecipientUserIdOrderByCreatedAtDesc(Long userId) {
        return em.createQuery("select n from Notification n where n.recipientUserId = :userId order by n.createdAt desc, n.id desc", Notification.class)
                .setParameter("userId", userId)
                .setMaxResults(50)
                .getResultList();
    }
}
