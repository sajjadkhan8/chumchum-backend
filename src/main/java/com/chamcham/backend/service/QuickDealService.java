package com.chamcham.backend.service;

import com.chamcham.backend.dto.quickdeal.QuickDealCreateRequest;
import com.chamcham.backend.dto.quickdeal.QuickDealCreateResponse;
import com.chamcham.backend.dto.quickdeal.QuickDealRespondResponse;
import com.chamcham.backend.entity.Brand;
import com.chamcham.backend.entity.Conversation;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.Message;
import com.chamcham.backend.entity.QuickDealOffer;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.OfferStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.BrandRepository;
import com.chamcham.backend.repository.ConversationRepository;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.MessageRepository;
import com.chamcham.backend.repository.QuickDealOfferRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuickDealService {

    private final QuickDealOfferRepository offerRepository;
    private final ConversationRepository conversationRepository;
    private final CreatorRepository creatorRepository;
    private final BrandRepository brandRepository;
    private final MessageRepository messageRepository;

    public QuickDealService(QuickDealOfferRepository offerRepository,
                            ConversationRepository conversationRepository,
                            CreatorRepository creatorRepository,
                            BrandRepository brandRepository,
                            MessageRepository messageRepository) {
        this.offerRepository = offerRepository;
        this.conversationRepository = conversationRepository;
        this.creatorRepository = creatorRepository;
        this.brandRepository = brandRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public QuickDealCreateResponse createOffer(UUID senderId, UserRole role, QuickDealCreateRequest request) {
        if (!role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can create quick deals");
        }

        validateDealPayload(request.dealType(), request.amount(), request.barterDetails());

        Creator creator = creatorRepository.findById(request.creatorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        Brand brand = brandRepository.findById(senderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));

        Conversation conversation = conversationRepository.findByCreatorIdAndBrandId(creator.getId(), brand.getId())
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .id(UUID.randomUUID())
                        .creator(creator)
                        .brand(brand)
                        .readByCreator(false)
                        .readByBrand(true)
                        .build()));

        Message message = messageRepository.save(Message.builder()
                .conversation(conversation)
                .sender(brand)
                .senderType("brand")
                .content(request.message())
                .type(Message.MessageType.OFFER)
                // Keep legacy inline fields populated while transitioning fully to quick_deal_offers.
                .offerDealType(request.dealType().name().toLowerCase())
                .offerAmount(request.amount())
                .offerBarterDetails(request.barterDetails())
                .offerBarterCategory(request.barterCategory())
                .offerEstimatedBarterValue(request.estimatedBarterValue())
                .offerCreatorExpectation(request.creatorExpectation())
                .offerMessage(request.message())
                .offerStatus("pending")
                .isRead(false)
                .build());

        QuickDealOffer offer = offerRepository.save(QuickDealOffer.builder()
                .messageEntity(message)
                .conversation(conversation)
                .dealType(request.dealType())
                .amount(request.amount())
                .barterDetails(request.barterDetails())
                .barterCategory(request.barterCategory())
                .estimatedBarterValue(request.estimatedBarterValue())
                .creatorExpectation(request.creatorExpectation())
                .message(request.message())
                .status(OfferStatus.PENDING)
                .build());

        message.setQuickDealOffer(offer);

        conversation.setUnreadCountCreator(conversation.getUnreadCountCreator() + 1);
        conversation.setReadByCreator(false);
        conversation.setReadByBrand(true);
        conversation.setLastMessage("[Offer]");
        conversation.setLastMessageId(message.getId());
        conversationRepository.save(conversation);

        return new QuickDealCreateResponse(conversation.getId(), message.getId(), offer.getId());
    }

    @Transactional
    public QuickDealRespondResponse respond(UUID offerId, UUID responderId, UserRole role, OfferStatus response) {
        if (!role.isCreator()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can respond to quick deals");
        }

        QuickDealOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));

        if (!offer.getConversation().getCreator().getId().equals(responderId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the target creator can respond to this offer");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Offer already responded to");
        }
        if (response == OfferStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid response status");
        }

        offer.setStatus(response);
        QuickDealOffer saved = offerRepository.save(offer);

        if (saved.getMessageEntity() != null) {
            Message message = saved.getMessageEntity();
            message.setOfferStatus(response.name().toLowerCase());
            messageRepository.save(message);
        }

        return new QuickDealRespondResponse(saved.getId(), saved.getStatus().name().toLowerCase());
    }

    private void validateDealPayload(DealType dealType, Integer amount, String barterDetails) {
        if ((dealType == DealType.PAID || dealType == DealType.HYBRID) && (amount == null || amount <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount is required when dealType is PAID/HYBRID");
        }
        if ((dealType == DealType.BARTER || dealType == DealType.HYBRID)
                && (barterDetails == null || barterDetails.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "barterDetails is required when dealType is BARTER/HYBRID");
        }
    }
}

