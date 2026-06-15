package com.chamcham.backend.service;

import com.chamcham.backend.dto.message.MessageCreateRequest;
import com.chamcham.backend.dto.message.MessageResponse;
import com.chamcham.backend.entity.Conversation;
import com.chamcham.backend.entity.Message;
import com.chamcham.backend.entity.QuickDealOffer;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.OfferStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.MessageMapper;
import com.chamcham.backend.repository.ConversationRepository;
import com.chamcham.backend.repository.MessageRepository;
import com.chamcham.backend.repository.QuickDealOfferRepository;
import com.chamcham.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.chamcham.backend.service.FileStorageService.PRIVATE_FILE_TYPES;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final QuickDealOfferRepository quickDealOfferRepository;
    private final MessageMapper messageMapper;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository,
                          QuickDealOfferRepository quickDealOfferRepository,
                          MessageMapper messageMapper,
                          FileStorageService fileStorageService,
                          NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.quickDealOfferRepository = quickDealOfferRepository;
        this.messageMapper = messageMapper;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    @Transactional
    public MessageResponse sendTextMessage(UUID userId, UserRole role, UUID conversationId,
                                           MessageCreateRequest request) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot send messages");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message content is required");
        }
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        User sender = findUser(userId);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .senderType(role.name().toLowerCase())
                .content(request.content())
                .type(Message.MessageType.TEXT)
                .isRead(false)
                .build();

        return save(message, conversation, role, request.content());
    }

    @Transactional
    public MessageResponse sendOfferMessage(UUID userId, UserRole role, UUID conversationId,
                                            MessageCreateRequest request) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot send offers");
        }
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        User sender = findUser(userId);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .senderType(role.name().toLowerCase())
                .content(request.content())
                .type(Message.MessageType.OFFER)
                .offerDealType(request.offerDealType() == null ? null : request.offerDealType().toLowerCase())
                .offerAmount(request.offerAmount())
                .offerBarterDetails(request.offerBarterDetails())
                .offerBarterCategory(request.offerBarterCategory())
                .offerMessage(request.content())
                .offerStatus("pending")
                .isRead(false)
                .build();
        Message savedMessage = messageRepository.save(message);

        DealType dealType = parseDealType(request.offerDealType());
        QuickDealOffer quickDealOffer = quickDealOfferRepository.save(QuickDealOffer.builder()
                .messageEntity(savedMessage)
                .conversation(conversation)
                .dealType(dealType)
                .amount(request.offerAmount())
                .barterDetails(request.offerBarterDetails())
                .barterCategory(request.offerBarterCategory())
                .estimatedBarterValue(request.offerEstimatedBarterValue())
                .creatorExpectation(null)
                .message(request.content() == null ? "Offer" : request.content())
                .status(OfferStatus.PENDING)
                .build());

        savedMessage.setQuickDealOffer(quickDealOffer);
        updateConversationAfterSend(conversation, role, "[Offer]", savedMessage.getId());
        return messageMapper.toResponse(savedMessage);
    }

    public List<MessageResponse> getMessages(UUID conversationId, UUID userId) {
        Conversation conversation = findConversation(conversationId);
        validateParticipant(userId, conversation);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().map(messageMapper::toResponse).toList();
    }

    @Transactional
    public MessageResponse sendAttachmentMessage(UUID userId, UserRole role, UUID conversationId, MultipartFile file) {
        if (role.isAdmin()) throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot send messages");
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        User sender = findUser(userId);

        String attachmentUrl = fileStorageService.validateAndStoreProtected(
                file, PRIVATE_FILE_TYPES, 100, "attachments/" + conversationId);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .senderType(role.name().toLowerCase())
                .type(Message.MessageType.ATTACHMENT)
                .attachmentUrl(attachmentUrl)
                .isRead(false)
                .build();

        return save(message, conversation, role, "[Attachment]");
    }

    @Transactional
    public void markRead(UUID conversationId, UUID userId, UserRole role) {
        Conversation conversation = findConversation(conversationId);
        validateParticipant(userId, conversation);
        if (role.isCreator()) {
            conversationRepository.markReadForCreator(conversationId);
        } else {
            conversationRepository.markReadForBrand(conversationId);
        }
        messageRepository.markIncomingRead(conversationId, userId);
    }

    // ---- helpers ----

    private MessageResponse save(Message message, Conversation conversation, UserRole senderRole, String preview) {
        Message saved = messageRepository.save(message);
        updateConversationAfterSend(conversation, senderRole, preview, saved.getId());
        return messageMapper.toResponse(saved);
    }

    private void updateConversationAfterSend(Conversation conversation, UserRole senderRole, String preview, UUID messageId) {
        // increment unread for the OTHER party
        if (senderRole.isCreator()) {
            conversation.setUnreadCountBrand(conversation.getUnreadCountBrand() + 1);
            conversation.setReadByCreator(true);
            conversation.setReadByBrand(false);
        } else {
            conversation.setUnreadCountCreator(conversation.getUnreadCountCreator() + 1);
            conversation.setReadByBrand(true);
            conversation.setReadByCreator(false);
        }
        conversation.setLastMessage(preview != null && preview.length() > 200
                ? preview.substring(0, 200) : preview);
        conversation.setLastMessageId(messageId);
        conversationRepository.save(conversation);
        UUID recipientId = senderRole.isCreator()
                ? conversation.getBrand().getId()
                : conversation.getCreator().getId();
        String senderName = senderRole.isCreator()
                ? conversation.getCreator().getName()
                : conversation.getBrand().getDisplayName();
        notificationService.sendMessageNotification(recipientId, "New message from " + senderName,
                preview == null ? "New message" : preview, conversation.getId());
    }

    private DealType parseDealType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "offerDealType is required for offer messages");
        }
        try {
            return DealType.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid offerDealType: " + rawValue);
        }
    }

    private void validateParticipant(UUID userId, Conversation conv) {
        if (!conv.getCreator().getId().equals(userId) && !conv.getBrand().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant in this conversation");
        }
    }

    private Conversation findConversation(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private Conversation findConversationForUpdate(UUID id) {
        return conversationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
