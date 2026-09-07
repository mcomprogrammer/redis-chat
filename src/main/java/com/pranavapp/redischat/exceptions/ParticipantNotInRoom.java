package com.pranavapp.redischat.exceptions;

import lombok.Getter;

public class ParticipantNotInRoom extends RuntimeException {
    public ParticipantNotInRoom(String roomId, String participant) {
        super("Participant named " + participant + " is not in the room " + roomId);
    }
}
