package com.pranavapp.redischat.exceptions;

import lombok.Getter;

@Getter
public class DuplicateRoomException extends RuntimeException {

    private final String roomId;

    public DuplicateRoomException(String roomId) {
        super("The room name" + roomId + "is already taken");
        this.roomId = roomId;
    }
}
