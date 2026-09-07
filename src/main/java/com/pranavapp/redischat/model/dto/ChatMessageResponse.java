package com.pranavapp.redischat.model.dto;

import java.time.Instant;

public record ChatMessageResponse(
        String participant,
        String message,
        Instant timestamp
) {
}
