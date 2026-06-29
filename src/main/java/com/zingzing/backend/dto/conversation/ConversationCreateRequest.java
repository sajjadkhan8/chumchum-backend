package com.zingzing.backend.dto.conversation;

import java.util.UUID;

public record ConversationCreateRequest(
        UUID to,
        UUID from,
        String contextType,
        UUID contextId
) {
}
