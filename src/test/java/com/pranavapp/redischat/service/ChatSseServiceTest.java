package com.pranavapp.redischat.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatSseServiceTest {

    @Test
    void forwardsRoomMessageToSubscribedEmitter() throws Exception {
        ChatSseService chatSseService = new ChatSseService();
        SseEmitter emitter = mock(SseEmitter.class);
        Message message = mock(Message.class);

        chatSseService.register("general", emitter);
        when(message.getChannel())
                .thenReturn("chat:room:general:events".getBytes(StandardCharsets.UTF_8));
        when(message.getBody())
                .thenReturn("{\"participant\":\"alice\",\"message\":\"Hello\"}"
                        .getBytes(StandardCharsets.UTF_8));

        chatSseService.onMessage(message, "chat:room:*:events".getBytes(StandardCharsets.UTF_8));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }
}
