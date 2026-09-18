package com.example.bandlink.service;

import com.example.bandlink.dto.MessageResponse;
import com.example.bandlink.entity.Message;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Delivers conversation changes to currently open participants without polling. */
@Service
public class MessageEventHub {
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    /**
     * spring.jpa.open-in-view is on (the Spring Boot default, and this app relies on it - lazy
     * associations are read straight off the entity in controllers, e.g. MessageResponse.from()),
     * which binds a Hikari connection to a request until it fully completes. An SseEmitter with no
     * timeout never completes on its own; every browser tab left on the messages page pinned one
     * connection for as long as it stayed open, and with a 10-connection pool, ten people idling on
     * the page at once was enough to starve every other request in the app (HikariPool-1: Connection
     * is not available). A bounded timeout forces the request to complete periodically, releasing
     * the connection - the browser's EventSource reconnects on its own, so the stream itself still
     * reads as unbroken to anyone actively watching it.
     */
    private static final long STREAM_TIMEOUT_MS = 10 * 60 * 1000;

    public SseEmitter subscribe(Long conversationId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        CopyOnWriteArrayList<SseEmitter> room = subscribers.computeIfAbsent(conversationId,
                ignored -> new CopyOnWriteArrayList<>());
        room.add(emitter);
        Runnable remove = () -> remove(conversationId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ignored -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("ready").data(Map.of("conversationId", conversationId)));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    public void publish(Message message) {
        publish(message.getConversation().getId(), "message", MessageResponse.from(message));
    }

    public void publishRead(Long conversationId) {
        publish(conversationId, "read", Map.of("conversationId", conversationId));
    }

    private void publish(Long conversationId, String eventName, Object data) {
        var room = subscribers.get(conversationId);
        if (room == null) return;
        for (SseEmitter emitter : room) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                remove(conversationId, emitter);
            }
        }
    }

    private void remove(Long conversationId, SseEmitter emitter) {
        var room = subscribers.get(conversationId);
        if (room == null) return;
        room.remove(emitter);
        if (room.isEmpty()) subscribers.remove(conversationId, room);
    }
}
