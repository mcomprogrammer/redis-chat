package com.pranavapp.redischat.model.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest (@NotBlank String participant){
}
