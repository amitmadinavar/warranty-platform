package com.wp;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepo notifications;
    private final SimpMessagingTemplate messaging;

    public NotificationService(NotificationRepo notifications, SimpMessagingTemplate messaging) {
        this.notifications = notifications;
        this.messaging = messaging;
    }

    public Notification sendToUser(Long userId, String type, String title, String message) {
        Notification n = save(userId, null, type, title, message);
        messaging.convertAndSend("/topic/notifications/user/" + userId, n);
        return n;
    }

    public void sendToRole(String role, String type, String title, String message) {
        Notification n = save(null, role, type, title, message);
        messaging.convertAndSend("/topic/notifications/role/" + role, n);
    }

    public List<Notification> forUser(AppUser user) {
        return notifications.findTop50ByRecipientUserIdOrTargetRoleOrderByCreatedAtDesc(user.id, user.role);
    }

    public void markReadForUser(Long id, AppUser user) {
        notifications.findById(id).ifPresent(n -> {
            boolean allowed = (n.recipientUserId != null && n.recipientUserId.equals(user.id))
                    || (n.targetRole != null && n.targetRole.equalsIgnoreCase(user.role));
            if (allowed) { n.readFlag = true; notifications.save(n); }
        });
    }

    private Notification save(Long userId, String role, String type, String title, String message) {
        Notification n = new Notification();
        n.recipientUserId = userId;
        n.targetRole = role;
        n.type = type;
        n.title = title;
        n.message = message;
        return notifications.save(n);
    }
}
