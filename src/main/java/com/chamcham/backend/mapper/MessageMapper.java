package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.message.MessageResponse;
import com.chamcham.backend.entity.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageMapper {

    public MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getDescription(),
                message.getCreatedAt()
        );
    }
}

