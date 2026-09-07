package com.pranavapp.redischat.exceptions;

import lombok.Getter;

@Getter
public class RoomNotFoundException extends RuntimeException {

    private final String roomId;
    public RoomNotFoundException(String roomId) {
        super("The Room name " + roomId + " does not exist.");
        this.roomId = roomId;
    }
}
