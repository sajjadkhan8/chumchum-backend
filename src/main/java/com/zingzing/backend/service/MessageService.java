package com.zingzing.backend.service;

import com.zingzing.backend.dto.message.MessageCreateRequest;
import com.zingzing.backend.dto.message.MessageResponse;
import com.zingzing.backend.entity.Conversation;
import com.zingzing.backend.entity.Message;
import com.zingzing.backend.entity.QuickDealOffer;
import com.zingzing.backend.entity.User;
import com.zingzing.backend.entity.enums.DealType;
import com.zingzing.backend.entity.enums.OfferStatus;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.mapper.MessageMapper;
import com.zingzing.backend.repository.ConversationRepository;
import com.zingzing.backend.repository.MessageRepository;
import com.zingzing.backend.repository.QuickDealOfferRepository;
import com.zingzing.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.zingzing.backend.service.FileStorageService.PRIVATE_FILE_TYPES;

import java.time.Instant;
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
    private final AuthRateLimitService rateLimitService;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          UserRepository userRepository,
                          QuickDealOfferRepository quickDealOfferRepository,
                          MessageMapper messageMapper,
                          FileStorageService fileStorageService,
                          NotificationService notificationService,
                          AuthRateLimitService rateLimitService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.quickDealOfferRepository = quickDealOfferRepository;
        this.messageMapper = messageMapper;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public MessageResponse sendTextMessage(UUID userId, UserRole role, UUID conversationId,
                                           MessageCreateRequest request) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot send messages");
        }
        if (rateLimitService.recordAndCheck("msg_send", userId.toString(), 60, 1, 10)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "You are sending messages too quickly. Please wait a moment.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message content is required");
        }
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        validateNotBlocked(conversation);
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
        if (rateLimitService.recordAndCheck("msg_offer", userId.toString(), 10, 60, 60)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many offer messages sent. Please wait before sending another.");
        }
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        validateNotBlocked(conversation);
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
        updateConversationAfterSend(conversation, role, "[Offer]", savedMessage.getId(), savedMessage.getCreatedAt());
        return messageMapper.toResponse(savedMessage);
    }

    public List<MessageResponse> getMessages(UUID conversationId, UUID userId) {
        Conversation conversation = findConversation(conversationId);
        validateParticipant(userId, conversation);
        Instant clearedAt = clearedAtFor(userId, conversation);
        List<Message> messages = clearedAt == null
                ? messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                : messageRepository.findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(conversationId, clearedAt);
        return messages
                .stream().map(messageMapper::toResponse).toList();
    }

    @Transactional
    public MessageResponse sendAttachmentMessage(UUID userId, UserRole role, UUID conversationId, MultipartFile file) {
        if (role.isAdmin()) throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot send messages");
        if (rateLimitService.recordAndCheck("msg_attachment", userId.toString(), 30, 1, 30)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "You are sending attachments too quickly. Please wait a moment.");
        }
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        validateNotBlocked(conversation);
        User sender = findUser(userId);

        String attachmentUrl = fileStorageService.validateAndStoreProtected(
                file, PRIVATE_FILE_TYPES, 100, "attachments/" + conversationId);

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .senderType(role.name().toLowerCase())
                .type(Message.MessageType.ATTACHMENT)
                .attachmentUrl(attachmentUrl)
                .attachmentOriginalName(sanitizeOriginalFilename(file.getOriginalFilename()))
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

    @Transactional
    public void clearChat(UUID conversationId, UUID userId, UserRole role) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot clear marketplace conversations");
        }
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        if (role.isCreator()) {
            conversation.setClearedAtCreator(Instant.now());
            conversation.setUnreadCountCreator(0);
        } else {
            conversation.setClearedAtBrand(Instant.now());
            conversation.setUnreadCountBrand(0);
        }
        conversationRepository.save(conversation);
        messageRepository.markIncomingRead(conversationId, userId);
    }

    @Transactional
    public void blockConversation(UUID conversationId, UUID userId, UserRole role) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot block marketplace conversations");
        }
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        if (role.isCreator()) {
            conversation.setBlockedAtCreator(Instant.now());
            conversation.setUnreadCountCreator(0);
        } else {
            conversation.setBlockedAtBrand(Instant.now());
            conversation.setUnreadCountBrand(0);
        }
        conversationRepository.save(conversation);
        messageRepository.markIncomingRead(conversationId, userId);
    }

    @Transactional
    public void unblockConversation(UUID conversationId, UUID userId, UserRole role) {
        if (role.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin cannot unblock marketplace conversations");
        }
        Conversation conversation = findConversationForUpdate(conversationId);
        validateParticipant(userId, conversation);
        if (role.isCreator()) {
            conversation.setBlockedAtCreator(null);
        } else {
            conversation.setBlockedAtBrand(null);
        }
        conversationRepository.save(conversation);
    }

    // ---- helpers ----

    private MessageResponse save(Message message, Conversation conversation, UserRole senderRole, String preview) {
        Message saved = messageRepository.save(message);
        updateConversationAfterSend(conversation, senderRole, preview, saved.getId(), saved.getCreatedAt());
        return messageMapper.toResponse(saved);
    }

    private void updateConversationAfterSend(Conversation conversation, UserRole senderRole, String preview, UUID messageId, Instant messageCreatedAt) {
        // increment unread for the OTHER party
        if (senderRole.isCreator()) {
            conversation.setUnreadCountBrand(conversation.getUnreadCountBrand() + 1);
        } else {
            conversation.setUnreadCountCreator(conversation.getUnreadCountCreator() + 1);
        }
        conversation.setLastMessage(preview != null && preview.length() > 200
                ? preview.substring(0, 200) : preview);
        conversation.setLastMessageId(messageId);
        conversation.setLastMessageAt(messageCreatedAt == null ? Instant.now() : messageCreatedAt);
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

    private void validateNotBlocked(Conversation conversation) {
        if (conversation.getBlockedAtCreator() != null || conversation.getBlockedAtBrand() != null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This conversation is blocked");
        }
    }

    private Instant clearedAtFor(UUID userId, Conversation conversation) {
        if (conversation.getCreator().getId().equals(userId)) {
            return conversation.getClearedAtCreator();
        }
        return conversation.getClearedAtBrand();
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

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        String normalized = originalFilename.replace("\\", "/");
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (baseName.isEmpty()) {
            return null;
        }
        return baseName.length() > 255 ? baseName.substring(baseName.length() - 255) : baseName;
    }
}
