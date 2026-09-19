package com.example.bandlink.controller;

import com.example.bandlink.dto.*;
import com.example.bandlink.entity.Message;
import com.example.bandlink.repository.MessageRepository;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.ImageStorageService;
import com.example.bandlink.service.MessageService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service; private final UserRepository users; private final MessageRepository messages; private final ImageStorageService storage; private final com.example.bandlink.repository.ReportRepository reports; private final com.example.bandlink.service.MessageEventHub events; private final EntityManagerFactory entityManagerFactory;
    public MessageController(MessageService service, UserRepository users, MessageRepository messages, ImageStorageService storage, com.example.bandlink.repository.ReportRepository reports, com.example.bandlink.service.MessageEventHub events, EntityManagerFactory entityManagerFactory) { this.service=service; this.users=users; this.messages=messages; this.storage=storage; this.reports=reports; this.events=events; this.entityManagerFactory=entityManagerFactory; }
    @PostMapping public MessageResponse send(Authentication a,@RequestParam Long recipientId,@Valid @RequestBody MessageRequests.Send r) { Message message=service.send(current(a),recipientId,r); events.publish(message); return MessageResponse.from(message); }
    @GetMapping("/conversation/{id}") public List<MessageResponse> list(Authentication a,@PathVariable Long id) { return service.messages(current(a),id).stream().map(MessageResponse::from).toList(); }
    @PatchMapping("/conversation/{id}/read") public void read(Authentication a,@PathVariable Long id) { service.markRead(current(a),id); events.publishRead(id); }
    @GetMapping("/conversations") public List<ConversationResponse> conversations(Authentication a) { Long viewer=current(a); return service.conversations(viewer).stream().map(c->ConversationResponse.from(c,viewer,messages.countByConversationIdAndSenderIdNotAndReadAtIsNull(c.getId(),viewer))).toList(); }
    @GetMapping(value="/conversation/{id}/stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication a,@PathVariable Long id) { Long viewer=current(a); service.requireParticipant(viewer,id); releaseOpenInViewConnection(); return events.subscribe(id); }
    /**
     * spring.jpa.open-in-view keeps this request's EntityManager - and the Hikari connection
     * behind it - bound until the whole request completes. For every other endpoint that happens
     * quickly; this one doesn't complete until the SSE stream itself closes, up to ten minutes
     * later (MessageEventHub.STREAM_TIMEOUT_MS), so without this the connection sits reserved and
     * unused for that entire time. The two lookups above are the only database work this request
     * needs, so the now-unneeded, connection-holding EntityManager is swapped for a fresh, never
     * queried one before the emitter is returned - Hikari gets its connection back immediately.
     * OSIV's own end-of-request cleanup still finds a holder bound (satisfying
     * TransactionSynchronizationManager's bookkeeping) and closes that fresh one instead, which
     * never touched the pool to begin with (Hibernate only acquires a physical connection on the
     * first query run against a session, and nothing in this request runs one against it).
     */
    private void releaseOpenInViewConnection() {
        Object resource = TransactionSynchronizationManager.getResource(entityManagerFactory);
        if (!(resource instanceof EntityManagerHolder holder)) return;
        EntityManagerFactoryUtils.closeEntityManager(holder.getEntityManager());
        TransactionSynchronizationManager.unbindResource(entityManagerFactory);
        TransactionSynchronizationManager.bindResource(entityManagerFactory, new EntityManagerHolder(entityManagerFactory.createEntityManager()));
    }
    @PostMapping(value="/images", produces=MediaType.TEXT_PLAIN_VALUE) public String uploadImage(Authentication a,@RequestParam MultipartFile file) { Long id=current(a); service.requireVerified(id); return storage.storePrivate(file); }
    @GetMapping("/images/{name}") public ResponseEntity<Resource> image(Authentication a,@PathVariable String name) { Long id=current(a); Message m=messages.findFirstByImageUrl("/api/messages/images/"+name).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)); if(!m.getConversation().includes(id)&&!moderatingReportedImage(a,"/api/messages/images/"+name))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN); String type=name.endsWith(".png")?"image/png":name.endsWith(".webp")?"image/webp":"image/jpeg"; return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(storage.loadPrivate(name)); }
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
