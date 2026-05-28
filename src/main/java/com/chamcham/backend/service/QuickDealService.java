package com.chamcham.backend.service;

import com.chamcham.backend.entity.Conversation;
import com.chamcham.backend.entity.QuickDealOffer;
import com.chamcham.backend.entity.enums.DealType;
import com.chamcham.backend.entity.enums.OfferStatus;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.ConversationRepository;
import com.chamcham.backend.repository.QuickDealOfferRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QuickDealService {

    private final QuickDealOfferRepository offerRepository;
    private final ConversationRepository conversationRepository;

    public QuickDealService(QuickDealOfferRepository offerRepository,
                            ConversationRepository conversationRepository) {
        this.offerRepository = offerRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public QuickDealOffer createOffer(UUID conversationId, UUID senderId, UserRole role,
                                      DealType dealType, Integer amount, String barterDetails,
                                      String barterCategory, Integer estimatedBarterValue,
                                      String creatorExpectation, String message) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.getCreator().getId().equals(senderId)
                && !conversation.getBrand().getId().equals(senderId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant in this conversation");
        }

        QuickDealOffer offer = QuickDealOffer.builder()
                .conversation(conversation)
                .dealType(dealType)
                .amount(amount)
                .barterDetails(barterDetails)
                .barterCategory(barterCategory)
                .estimatedBarterValue(estimatedBarterValue)
                .creatorExpectation(creatorExpectation)
                .message2(message)
                .status(OfferStatus.PENDING)
                .build();

        return offerRepository.save(offer);
    }

    @Transactional
    public QuickDealOffer respond(UUID offerId, UUID responderId, OfferStatus response) {
        QuickDealOffer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Offer not found"));

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Offer already responded to");
        }
        if (response == OfferStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid response status");
        }

        offer.setStatus(response);
        return offerRepository.save(offer);
    }
}

