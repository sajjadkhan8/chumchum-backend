package com.chamcham.backend.dto.conversation;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConversationCreateRequest(
        @NotNull UUID to,
        UUID from
) {
}

