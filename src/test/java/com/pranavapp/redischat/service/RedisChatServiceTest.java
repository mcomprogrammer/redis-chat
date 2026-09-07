package com.pranavapp.redischat.service;

import com.pranavapp.redischat.exceptions.DuplicateRoomException;
import com.pranavapp.redischat.exceptions.ParticipantNotInRoom;
import com.pranavapp.redischat.exceptions.RoomNotFoundException;
import com.pranavapp.redischat.model.dto.ChatHistoryResponse;
import com.pranavapp.redischat.model.dto.ChatMessageResponse;
import com.pranavapp.redischat.model.dto.InternalJoinRoomRequest;
import com.pranavapp.redischat.model.dto.RoomCreationRequest;
import com.pranavapp.redischat.model.dto.RoomCreationResponse;
import com.pranavapp.redischat.model.dto.SendMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisChatServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    private RedisChatService redisChatService;

    @BeforeEach
    void setUp() {
        redisChatService = new RedisChatService(stringRedisTemplate);
    }

    @Test
    void createRoom_trimsRoomNameStoresItAndReturnsSuccessResponse() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.putIfAbsent("chat:room:general", "roomName", "general"))
                .thenReturn(true);

        RoomCreationResponse response = redisChatService.createRoom(
                new RoomCreationRequest("  general  ")
        );

        assertEquals("general", response.roomId());
        assertEquals("SUCCESS", response.status());
        assertEquals("Chat room general created successfully.", response.message());
        verify(hashOperations).putIfAbsent("chat:room:general", "roomName", "general");
    }

    @Test
    void createRoom_whenRoomAlreadyExists_throwsDuplicateRoomException() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.putIfAbsent("chat:room:general", "roomName", "general"))
                .thenReturn(false);

        DuplicateRoomException exception = assertThrows(
                DuplicateRoomException.class,
                () -> redisChatService.createRoom(new RoomCreationRequest("general"))
        );

        assertEquals("general", exception.getRoomId());
        verify(hashOperations).putIfAbsent("chat:room:general", "roomName", "general");
    }

    @Test
    void joinRoom_trimsRoomIdAddsParticipantAndReturnsSuccessResponse() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("chat:room:general", "roomName"))
                .thenReturn(true);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add("chat:room:general:participants", "alice"))
                .thenReturn(1L);

        var response = redisChatService.joinRoom(
                new InternalJoinRoomRequest("  general  ", "alice")
        );

        assertEquals("User alice joined chat. general", response.message());
        assertEquals("SUCCESS", response.status());
        verify(hashOperations).hasKey("chat:room:general", "roomName");
        verify(setOperations).add("chat:room:general:participants", "alice");
    }

    @Test
    void joinRoom_whenRoomDoesNotExist_throwsRoomNotFoundException() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("chat:room:general", "roomName"))
                .thenReturn(false);

        RoomNotFoundException exception = assertThrows(
                RoomNotFoundException.class,
                () -> redisChatService.joinRoom(
                        new InternalJoinRoomRequest("general", "alice")
                )
        );

        assertEquals("general", exception.getRoomId());
        verify(hashOperations).hasKey("chat:room:general", "roomName");
    }

    @Test
    void sendMessage_whenParticipantIsInRoomStoresMessageAndReturnsSuccessResponse() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("chat:room:general", "roomName"))
                .thenReturn(true);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("chat:room:general:participants", "alice"))
                .thenReturn(true);
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPush(eq("chat:room:general:messages"), anyString()))
                .thenReturn(1L);

        var response = redisChatService.sendMessage(
                new SendMessageRequest("alice", "Hello, everyone!"),
                "general"
        );

        assertEquals("Message sent successfully", response.message());
        assertEquals("SUCCESS", response.status());

        ArgumentCaptor<String> storedJson = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(
                eq("chat:room:general:messages"),
                storedJson.capture()
        );

        ChatMessageResponse storedMessage = new ObjectMapper()
                .readValue(storedJson.getValue(), ChatMessageResponse.class);
        assertEquals("alice", storedMessage.participant());
        assertEquals("Hello, everyone!", storedMessage.message());
        assertNotNull(storedMessage.timestamp());
    }

    @Test
    void sendMessage_whenParticipantIsNotInRoom_throwsParticipantNotInRoom() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("chat:room:general", "roomName"))
                .thenReturn(true);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.isMember("chat:room:general:participants", "alice"))
                .thenReturn(false);

        ParticipantNotInRoom exception = assertThrows(
                ParticipantNotInRoom.class,
                () -> redisChatService.sendMessage(
                        new SendMessageRequest("alice", "Hello"),
                        "general"
                )
        );

        assertEquals(
                "Participant named alice is not in the room general",
                exception.getMessage()
        );
        verify(stringRedisTemplate, never()).opsForList();
    }

    @Test
    void sendMessage_whenRoomDoesNotExist_throwsRoomNotFoundException() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("chat:room:general", "roomName"))
                .thenReturn(false);

        RoomNotFoundException exception = assertThrows(
                RoomNotFoundException.class,
                () -> redisChatService.sendMessage(
                        new SendMessageRequest("alice", "Hello"),
                        "general"
                )
        );

        assertEquals("general", exception.getRoomId());
        verify(stringRedisTemplate, never()).opsForSet();
    }

    @Test
    void getChatHistory_returnsLatestMessagesInChronologicalOrder() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("chat:room:general", "roomName"))
                .thenReturn(true);
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);

        String firstMessage = "{\"participant\":\"guest_user\","
                + "\"message\":\"Hello, everyone!\","
                + "\"timestamp\":\"2024-01-01T10:00:00Z\"}";
        String secondMessage = "{\"participant\":\"another_user\","
                + "\"message\":\"Hi, guest_user!\","
                + "\"timestamp\":\"2024-01-01T10:01:00Z\"}";

        when(listOperations.range("chat:room:general:messages", -2L, -1L))
                .thenReturn(List.of(firstMessage, secondMessage));

        ChatHistoryResponse response = redisChatService.getChatHistory("general", 2);

        assertEquals(2, response.messages().size());
        assertEquals("guest_user", response.messages().get(0).participant());
        assertEquals("Hello, everyone!", response.messages().get(0).message());
        assertEquals(Instant.parse("2024-01-01T10:00:00Z"), response.messages().get(0).timestamp());
        assertEquals("another_user", response.messages().get(1).participant());
        assertEquals("Hi, guest_user!", response.messages().get(1).message());
        assertEquals(Instant.parse("2024-01-01T10:01:00Z"), response.messages().get(1).timestamp());
        verify(listOperations).range("chat:room:general:messages", -2L, -1L);
    }

    @Test
    void getChatHistory_whenRoomDoesNotExist_throwsRoomNotFoundException() {
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey("chat:room:general", "roomName"))
                .thenReturn(false);

        RoomNotFoundException exception = assertThrows(
                RoomNotFoundException.class,
                () -> redisChatService.getChatHistory("general", 10)
        );

        assertEquals("general", exception.getRoomId());
        verify(stringRedisTemplate, never()).opsForList();
    }
}
