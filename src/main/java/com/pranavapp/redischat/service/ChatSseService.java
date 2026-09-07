package com.pranavapp.redischat.service;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatSseService implements MessageListener {

    private static final String CHANNEL_PREFIX = "chat:room:";
    private static final String CHANNEL_SUFFIX = ":events";

    private final Map<String, Set<SseEmitter>> emittersByRoom = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String roomId) {
        SseEmitter emitter = new SseEmitter(0L);
        register(roomId, emitter);
        return emitter;
    }

    void register(String roomId, SseEmitter emitter) {
        emittersByRoom
                .computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet())
                .add(emitter);

        Runnable cleanup = () -> remove(roomId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(() -> {
            cleanup.run();
            emitter.complete();
        });
        emitter.onError(ignored -> cleanup.run());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String roomId = roomIdFromChannel(message.getChannel());
        if (roomId == null) {
            return;
        }

        Set<SseEmitter> roomEmitters = emittersByRoom.get(roomId);
        if (roomEmitters == null) {
            return;
        }

        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        for (SseEmitter emitter : List.copyOf(roomEmitters)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(payload));
            } catch (IOException exception) {
                remove(roomId, emitter);
                emitter.completeWithError(exception);
            }
        }
    }

    private void remove(String roomId, SseEmitter emitter) {
        emittersByRoom.computeIfPresent(roomId, (ignored, roomEmitters) -> {
            roomEmitters.remove(emitter);
            return roomEmitters.isEmpty() ? null : roomEmitters;
        });
    }

    private String roomIdFromChannel(byte[] channel) {
        String channelName = new String(channel, StandardCharsets.UTF_8);
        if (!channelName.startsWith(CHANNEL_PREFIX) || !channelName.endsWith(CHANNEL_SUFFIX)) {
            return null;
        }

        return channelName.substring(
                CHANNEL_PREFIX.length(),
                channelName.length() - CHANNEL_SUFFIX.length()
        );
    }
}
