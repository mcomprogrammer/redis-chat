package com.pranavapp.redischat.model.dto;

import jakarta.validation.constraints.NotNull;

public record JoinRoomRequest (@NotNull String participant){
}
