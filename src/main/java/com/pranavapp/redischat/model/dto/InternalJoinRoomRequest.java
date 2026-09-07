package com.pranavapp.redischat.model.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalJoinRoomRequest(@NotBlank String roomId, @NotBlank String participant) {};
