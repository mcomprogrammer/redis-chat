package com.pranavapp.redischat.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMessageRequest (@NotBlank String participant, @NotBlank String message) {
}
