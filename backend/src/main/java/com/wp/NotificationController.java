package com.wp;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final UserRepo users;
    private final NotificationService notifications;

    public NotificationController(UserRepo users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @GetMapping
    public List<Notification> list(Authentication auth) {
        return notifications.forUser(current(auth));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> read(@PathVariable Long id, Authentication auth) {
        notifications.markReadForUser(id, current(auth));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private AppUser current(Authentication auth) {
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
    }
}
