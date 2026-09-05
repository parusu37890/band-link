package com.example.bandlink.controller;

import com.example.bandlink.dto.*;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service;
    private final UserRepository users;
    public MessageController(MessageService service, UserRepository users) { this.service = service; this.users = users; }
    @PostMapping
    public MessageResponse send(Authentication a, @RequestParam Long recipientId, @Valid @RequestBody MessageRequests.Send r) {
        return MessageResponse.from(service.send(current(a), recipientId, r));
    }
    @GetMapping("/conversation/{id}")
    public List<MessageResponse> list(Authentication a, @PathVariable Long id) {
        return service.messages(current(a), id).stream().map(MessageResponse::from).toList();
    }
    @PatchMapping("/conversation/{id}/read")
    public void read(Authentication a, @PathVariable Long id) { service.markRead(current(a), id); }
    @GetMapping("/conversations")
    public List<ConversationResponse> conversations(Authentication a) {
        Long viewer = current(a);
        return service.conversations(viewer).stream().map(c -> ConversationResponse.from(c, viewer)).toList();
    }
    private Long current(Authentication a) { return users.findByEmail(a.getName()).orElseThrow().getId(); }
}
