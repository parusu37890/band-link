package com.example.bandlink.controller;

import com.example.bandlink.entity.Notification;
import com.example.bandlink.repository.NotificationRepository;
import com.example.bandlink.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository notifications; private final UserRepository users;
    public NotificationController(NotificationRepository n,UserRepository u){notifications=n;users=u;}
    @GetMapping public List<Notification> list(Authentication a){return notifications.findByUserIdOrderByCreatedAtDesc(current(a));}
    @GetMapping("/unread-count") public Map<String,Long> unread(Authentication a){return Map.of("count",notifications.countByUserIdAndReadAtIsNull(current(a)));}
    @PatchMapping("/{id}/read") public void read(Authentication a,@PathVariable Long id){Notification n=notifications.findByIdAndUserId(id,current(a)).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));if(n.getReadAt()==null){n.markRead(LocalDateTime.now());notifications.save(n);}}
    @PatchMapping("/read-all") public void readAll(Authentication a){List<Notification> unread=notifications.findByUserIdOrderByCreatedAtDesc(current(a)).stream().filter(n->n.getReadAt()==null).toList();unread.forEach(n->n.markRead(LocalDateTime.now()));if(!unread.isEmpty())notifications.saveAll(unread);}
    private Long current(Authentication a){return users.findByEmail(a.getName()).orElseThrow().getId();}
}
