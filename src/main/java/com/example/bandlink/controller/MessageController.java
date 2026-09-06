package com.example.bandlink.controller;

import com.example.bandlink.dto.*;
import com.example.bandlink.entity.Message;
import com.example.bandlink.repository.MessageRepository;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.ImageStorageService;
import com.example.bandlink.service.MessageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service; private final UserRepository users; private final MessageRepository messages; private final ImageStorageService storage;
    public MessageController(MessageService service, UserRepository users, MessageRepository messages, ImageStorageService storage) { this.service=service; this.users=users; this.messages=messages; this.storage=storage; }
    @PostMapping public MessageResponse send(Authentication a,@RequestParam Long recipientId,@Valid @RequestBody MessageRequests.Send r) { return MessageResponse.from(service.send(current(a),recipientId,r)); }
    @GetMapping("/conversation/{id}") public List<MessageResponse> list(Authentication a,@PathVariable Long id) { return service.messages(current(a),id).stream().map(MessageResponse::from).toList(); }
    @PatchMapping("/conversation/{id}/read") public void read(Authentication a,@PathVariable Long id) { service.markRead(current(a),id); }
    @GetMapping("/conversations") public List<ConversationResponse> conversations(Authentication a) { Long viewer=current(a); return service.conversations(viewer).stream().map(c->ConversationResponse.from(c,viewer)).toList(); }
    @PostMapping("/images") public String uploadImage(Authentication a,@RequestParam MultipartFile file) { Long id=current(a); service.requireVerified(id); String uploaded=storage.store(file); return "/api/messages/images/"+uploaded.substring(uploaded.lastIndexOf('/')+1); }
    @GetMapping("/images/{name}") public ResponseEntity<Resource> image(Authentication a,@PathVariable String name) { Long id=current(a); Message m=messages.findFirstByImageUrl("/api/messages/images/"+name).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)); if(!m.getConversation().includes(id))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN); String type=name.endsWith(".png")?"image/png":name.endsWith(".webp")?"image/webp":"image/jpeg"; return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(storage.load(name)); }
    private Long current(Authentication a){return users.findByEmail(a.getName()).orElseThrow().getId();}
}
