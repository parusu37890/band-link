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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service; private final UserRepository users; private final MessageRepository messages; private final ImageStorageService storage; private final com.example.bandlink.repository.ReportRepository reports; private final com.example.bandlink.service.MessageEventHub events;
    public MessageController(MessageService service, UserRepository users, MessageRepository messages, ImageStorageService storage, com.example.bandlink.repository.ReportRepository reports, com.example.bandlink.service.MessageEventHub events) { this.service=service; this.users=users; this.messages=messages; this.storage=storage; this.reports=reports; this.events=events; }
    @PostMapping public MessageResponse send(Authentication a,@RequestParam Long recipientId,@Valid @RequestBody MessageRequests.Send r) { Message message=service.send(current(a),recipientId,r); events.publish(message); return MessageResponse.from(message); }
    @GetMapping("/conversation/{id}") public List<MessageResponse> list(Authentication a,@PathVariable Long id) { return service.messages(current(a),id).stream().map(MessageResponse::from).toList(); }
    @PatchMapping("/conversation/{id}/read") public void read(Authentication a,@PathVariable Long id) { service.markRead(current(a),id); events.publishRead(id); }
    @GetMapping("/conversations") public List<ConversationResponse> conversations(Authentication a) { Long viewer=current(a); return service.conversations(viewer).stream().map(c->ConversationResponse.from(c,viewer)).toList(); }
    @GetMapping(value="/conversation/{id}/stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication a,@PathVariable Long id) { Long viewer=current(a); service.requireParticipant(viewer,id); return events.subscribe(id); }
    @PostMapping(value="/images", produces=MediaType.TEXT_PLAIN_VALUE) public String uploadImage(Authentication a,@RequestParam MultipartFile file) { Long id=current(a); service.requireVerified(id); String uploaded=storage.store(file); return "/api/messages/images/"+uploaded.substring(uploaded.lastIndexOf('/')+1); }
    @GetMapping("/images/{name}") public ResponseEntity<Resource> image(Authentication a,@PathVariable String name) { Long id=current(a); Message m=messages.findFirstByImageUrl("/api/messages/images/"+name).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)); if(!m.getConversation().includes(id)&&!moderatingReportedImage(a,"/api/messages/images/"+name))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN); String type=name.endsWith(".png")?"image/png":name.endsWith(".webp")?"image/webp":"image/jpeg"; return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(storage.load(name)); }
    private Long current(Authentication a){return users.findByEmail(a.getName()).orElseThrow().getId();}

    /**
     * A moderator handling a report has to see the image that was reported, but should not gain
     * access to conversation images generally. Access is granted only for an image a report
     * actually points at (requirements 8章), and only to an ADMIN.
     */
    private boolean moderatingReportedImage(Authentication authentication, String imageUrl) {
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> "ROLE_ADMIN".equals(granted.getAuthority()));
        return admin && reports.existsByImageSnapshot(imageUrl);
    }
}
