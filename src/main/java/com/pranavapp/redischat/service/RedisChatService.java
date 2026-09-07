package com.pranavapp.redischat.service;

import com.pranavapp.redischat.exceptions.DuplicateRoomException;
import com.pranavapp.redischat.exceptions.ParticipantNotInRoom;
import com.pranavapp.redischat.exceptions.RoomNotFoundException;
import com.pranavapp.redischat.model.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Service
public class RedisChatService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatService(StringRedisTemplate stringRedisTemplate) {
        this(stringRedisTemplate, new ObjectMapper());
    }

    @Autowired
    public RedisChatService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public RoomCreationResponse createRoom(RoomCreationRequest roomCreationRequest) {
        String roomId = roomCreationRequest.roomName().trim();

        String roomKey = "chat:room:" + roomId;

        Boolean created = stringRedisTemplate.opsForHash()
                .putIfAbsent(roomKey, "roomName", roomId);

        if (Boolean.FALSE.equals(created)) {
            throw new DuplicateRoomException(roomId);
        }

        var response = new RoomCreationResponse("Chat room " + roomId + " created successfully.", roomId, "SUCCESS");

        return response;
    }

    public JoinRoomResponse joinRoom(InternalJoinRoomRequest joinRoomRequest) {
        // no such room exist
        String roomId = joinRoomRequest.roomId().trim();
        roomExists(roomId);

        String participantsKey = "chat:room:" + roomId + ":participants";
        var participant = joinRoomRequest.participant();
        stringRedisTemplate.opsForSet().add(participantsKey, participant);

        return new JoinRoomResponse(
                "User " + participant + " joined chat. " + roomId,
                "SUCCESS"
        );
    }

    private void roomExists(String roomId) {
        String roomKey = "chat:room:" + roomId;

        Boolean roomExists = stringRedisTemplate.opsForHash()
                .hasKey(roomKey, "roomName");

        if (Boolean.FALSE.equals(roomExists)) {
            throw new RoomNotFoundException(roomId);
        }
    }

    public SendMessageResponse sendMessage(SendMessageRequest request , String roomId){
        // room exists or not.
        validateParticipantInRoom(roomId, request.participant());

        var participant = request.participant();

        ChatMessageResponse chatMessage = new ChatMessageResponse(
                participant,
                request.message(),
                Instant.now()
        );

        stringRedisTemplate.opsForList().rightPush(
                messagesKey(roomId),
                objectMapper.writeValueAsString(chatMessage)
        );

        stringRedisTemplate.convertAndSend(
                eventsKey(roomId),
                objectMapper.writeValueAsString(chatMessage)
        );

        return new SendMessageResponse(
                "Message sent successfully",
                "SUCCESS"
        );
    }

    public void validateParticipantInRoom(String roomId, String participant) {
        roomExists(roomId);

        String participantsKey = "chat:room:" + roomId + ":participants";
        Boolean participantExistsInRoom = stringRedisTemplate.opsForSet()
                .isMember(participantsKey, participant);

        if (Boolean.FALSE.equals(participantExistsInRoom)) {
            throw new ParticipantNotInRoom(roomId, participant);
        }
    }

    public ChatHistoryResponse getChatHistory(String roomId, int limit) {
        roomExists(roomId);

        List<String> serializedMessages = stringRedisTemplate.opsForList()
                .range(messagesKey(roomId), -((long) limit), -1L);

        if (serializedMessages == null || serializedMessages.isEmpty()) {
            return new ChatHistoryResponse(List.of());
        }

        List<ChatMessageResponse> messages = serializedMessages.stream()
                .map(serializedMessage -> objectMapper.readValue(
                        serializedMessage,
                        ChatMessageResponse.class
                ))
                .toList();

        return new ChatHistoryResponse(messages);
    }

    private String messagesKey(String roomId) {
        return "chat:room:" + roomId + ":messages";
    }

    private String eventsKey(String roomId) {
        return "chat:room:" + roomId + ":events";
    }

}
