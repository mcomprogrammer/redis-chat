package com.pranavapp.redischat.model.dto;

import lombok.NonNull;

public record SendMessageResponse (@NonNull String message, @NonNull String status) {
}
