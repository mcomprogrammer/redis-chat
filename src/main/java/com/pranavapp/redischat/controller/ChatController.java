package com.pranavapp.redischat.controller;

import com.pranavapp.redischat.model.dto.*;
import com.pranavapp.redischat.service.ChatSseService;
import com.pranavapp.redischat.service.RedisChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequestMapping("/api/chatapp/chatrooms")
@RequiredArgsConstructor
@RestController
@Validated
public class ChatController {

    private final RedisChatService redisChatService;
    private final ChatSseService chatSseService;

    @PostMapping
    public ResponseEntity<RoomCreationResponse> createRoom(@Valid @RequestBody RoomCreationRequest request){
        var response = redisChatService.createRoom(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<JoinRoomResponse> joinRoom(@Valid @RequestBody JoinRoomRequest request, @PathVariable String roomId){
        var intermediateRequest = new InternalJoinRoomRequest(
                roomId,
                request.participant()
        );

        var response = redisChatService.joinRoom(intermediateRequest);

        return ResponseEntity.ok(response);
    }

    @PostMapping("{roomId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request, @PathVariable String roomId){

        var response = redisChatService.sendMessage(request,roomId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "1") @Min(1) int limit
    ) {
        var response = redisChatService.getChatHistory(roomId, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String roomId,
            @RequestParam @NotBlank String participant
    ) {
        redisChatService.validateParticipantInRoom(roomId, participant);
        return chatSseService.subscribe(roomId);
    }

}
