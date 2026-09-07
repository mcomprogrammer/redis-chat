package com.pranavapp.redischat.model.dto;

import java.util.List;

public record ChatHistoryResponse(List<ChatMessageResponse> messages) {
}
