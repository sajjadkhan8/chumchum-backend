package com.zingzing.backend.service;

import com.zingzing.backend.dto.quickdeal.QuickDealCreateRequest;
import com.zingzing.backend.dto.quickdeal.QuickDealCreateResponse;
import com.zingzing.backend.dto.quickdeal.QuickDealRespondResponse;
import com.zingzing.backend.entity.Brand;
import com.zingzing.backend.entity.Conversation;
import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.Message;
import com.zingzing.backend.entity.QuickDealOffer;
import com.zingzing.backend.entity.ServicePackage;
import com.zingzing.backend.entity.enums.DealType;
import com.zingzing.backend.entity.enums.OfferStatus;
import com.zingzing.backend.entity.enums.PackageCategory;
import com.zingzing.backend.entity.enums.PackagePlatform;
import com.zingzing.backend.entity.enums.PackageStatus;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.BrandRepository;
import com.zingzing.backend.repository.ConversationRepository;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.MessageRepository;
import com.zingzing.backend.repository.QuickDealOfferRepository;
import com.zingzing.backend.repository.ServicePackageRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class QuickDealService {

    private final QuickDealOfferRepository offerRepository;
    private final ConversationRepository conversationRepository;
    private final CreatorRepository creatorRepository;
    private final BrandRepository brandRepository;
    private final MessageRepository messageRepository;
    private final ServicePackageRepository servicePackageRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final AuthRateLimitService rateLimitService;

    public QuickDealService(QuickDealOfferRepository offerRepository,
                            ConversationRepository conversationRepository,
                            CreatorRepository creatorRepository,
                            BrandRepository brandRepository,
                            MessageRepository messageRepository,
                            ServicePackageRepository servicePackageRepository,
                            OrderService orderService,
                            NotificationService notificationService,
                            AuthRateLimitService rateLimitService) {
        this.offerRepository = offerRepository;
        this.conversationRepository = conversationRepository;
        this.creatorRepository = creatorRepository;
        this.brandRepository = brandRepository;
        this.messageRepository = messageRepository;
        this.servicePackageRepository = servicePackageRepository;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public QuickDealCreateResponse createOffer(UUID senderId, UserRole role, QuickDealCreateRequest request) {
        if (!role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only brands can create quick deals");
        }

        if (rateLimitService.recordAndCheck("quick_deal_create", senderId.toString(), 10, 60, 120)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many quick deal offers sent. Please wait before sending another.");
        }

        validateDealPayload(request.dealType(), request.amount(), request.barterDetails(), request.estimatedBarterValue());

        Creator creator = creatorRepository.findById(request.creatorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator not found"));
        Brand brand = brandRepository.findById(senderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Brand profile not found"));

        Conversation conversation = conversationRepository.findByCreatorIdAndBrandId(creator.getId(), brand.getId())
                .map(existing -> conversationRepository.findByIdForUpdate(existing.getId()).orElse(existing))
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .id(UUID.randomUUID())
                        .creator(creator)
                        .brand(brand)
                        .build()));

        if (conversation.getBlockedAtCreator() != null || conversation.getBlockedAtBrand() != null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This conversation is blocked");
        }

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
                .platform(request.platform() != null ? request.platform() : PackagePlatform.INSTAGRAM)
                .deliveryDays(request.deliveryDays() != null && request.deliveryDays() > 0 ? request.deliveryDays() : 7)
                .status(OfferStatus.PENDING)
                .build());

        message.setQuickDealOffer(offer);

        conversation.setUnreadCountCreator(conversation.getUnreadCountCreator() + 1);
        conversation.setLastMessage("[Offer]");
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageAt(message.getCreatedAt() == null ? Instant.now() : message.getCreatedAt());
        conversationRepository.save(conversation);
        notificationService.sendMessageNotification(creator.getId(), "New deal offer from " + brand.getDisplayName(),
                request.message() == null || request.message().isBlank() ? "New deal offer" : request.message(),
                conversation.getId());

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
        UUID orderId = null;

        if (response == OfferStatus.ACCEPTED) {
            ServicePackage quickDealPackage = createQuickDealPackage(saved);
            orderId = orderService.createPrivateDealOrder(
                    quickDealPackage.getId(),
                    saved.getConversation().getBrand().getId(),
                    saved.getAmount(),
                    saved.getBarterDetails(),
                    saved.getMessage(),
                    saved.getDealType()
            ).id();
        }

        if (saved.getMessageEntity() != null) {
            Message message = saved.getMessageEntity();
            message.setOfferStatus(response.name().toLowerCase());
            messageRepository.save(message);
        }

        return new QuickDealRespondResponse(saved.getId(), saved.getStatus().name().toLowerCase(), orderId);
    }

    private ServicePackage createQuickDealPackage(QuickDealOffer offer) {
        Creator creator = offer.getConversation().getCreator();
        String title = "Quick Deal - " + creator.getName();
        String description = offer.getMessage();
        Integer price = offer.getDealType() == DealType.BARTER ? 0 : offer.getAmount();

        return servicePackageRepository.save(ServicePackage.builder()
                .creator(creator)
                .name("quick-deal-" + offer.getId())
                .title(title.length() > 150 ? title.substring(0, 150) : title)
                .shortDescription("Accepted quick deal")
                .description(description)
                .fullDescription(description)
                .platform(offer.getPlatform() != null ? offer.getPlatform() : PackagePlatform.INSTAGRAM)
                .category(PackageCategory.QUICK_DEAL)
                .dealType(offer.getDealType())
                .status(PackageStatus.ACTIVE)
                .visibility("private")
                .price(price == null ? 0 : price)
                .barterDetails(offer.getBarterDetails())
                .barterDescription(offer.getBarterDetails())
                .hybridCashAmount(offer.getDealType() == DealType.HYBRID ? offer.getAmount() : null)
                .creatorExpectations(offer.getCreatorExpectation())
                .deliverables(java.util.List.of("Quick deal deliverable"))
                .deliveryDays(offer.getDeliveryDays() > 0 ? offer.getDeliveryDays() : 7)
                .revisions(1)
                .tags(java.util.List.of("quick-deal"))
                .currency("PKR")
                .responseTime("Within 24 hours")
                .active(true)
                .build());
    }

    private void validateDealPayload(DealType dealType, Integer amount, String barterDetails, Integer estimatedBarterValue) {
        if ((dealType == DealType.PAID || dealType == DealType.HYBRID) && (amount == null || amount <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "amount is required when dealType is PAID/HYBRID");
        }
        if ((dealType == DealType.BARTER || dealType == DealType.HYBRID)
                && (barterDetails == null || barterDetails.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "barterDetails is required when dealType is BARTER/HYBRID");
        }
        if ((dealType == DealType.BARTER || dealType == DealType.HYBRID)
                && (estimatedBarterValue == null || estimatedBarterValue <= 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "estimatedBarterValue is required and must be greater than 0 for barter and hybrid deals");
        }
    }
}
