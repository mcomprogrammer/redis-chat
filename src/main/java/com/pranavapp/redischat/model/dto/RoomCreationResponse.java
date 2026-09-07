package com.pranavapp.redischat.model.dto;

import lombok.NonNull;

public record RoomCreationResponse (
        @NonNull String message,
        @NonNull String roomId,
        @NonNull String status
){
}
