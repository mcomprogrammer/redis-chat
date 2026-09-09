package com.pranavapp.redischat.exceptions;

import com.pranavapp.redischat.model.dto.JoinRoomResponse;
import com.pranavapp.redischat.model.dto.RoomCreationResponse;
import com.pranavapp.redischat.model.dto.SendMessageResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<SendMessageResponse> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(new SendMessageResponse(e.getMessage(), "ERROR"));
    }

    @ExceptionHandler(DuplicateRoomException.class)
    public ResponseEntity<RoomCreationResponse> handleDuplicateRoomException(DuplicateRoomException e) {
        var response = new RoomCreationResponse(
                e.getMessage(),
                e.getRoomId(),
                "ERROR"
        );

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<JoinRoomResponse> handleRoomNotFoundException(RoomNotFoundException e){
        var response = new JoinRoomResponse(
              e.getMessage(),
              "ERROR"
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ParticipantNotInRoom.class)
    public ResponseEntity<SendMessageResponse> handleParticipantNotInRoomException(ParticipantNotInRoom e){
        var response = new SendMessageResponse(
                e.getMessage(),
                "ERROR"
        );

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

}
