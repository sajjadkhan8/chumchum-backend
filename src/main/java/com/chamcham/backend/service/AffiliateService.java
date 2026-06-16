package com.chamcham.backend.service;

import com.chamcham.backend.dto.affiliate.AffiliateCommissionPageResponse;
import com.chamcham.backend.dto.affiliate.AffiliateCommissionResponse;
import com.chamcham.backend.dto.affiliate.AffiliateOverviewResponse;
import com.chamcham.backend.entity.AffiliateAttribution;
import com.chamcham.backend.entity.AffiliateCommission;
import com.chamcham.backend.entity.AffiliateLink;
import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.Order;
import com.chamcham.backend.entity.Transaction;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.entity.enums.AffiliateCommissionStatus;
import com.chamcham.backend.entity.enums.TransactionStatus;
import com.chamcham.backend.entity.enums.TransactionType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.AffiliateAttributionRepository;
import com.chamcham.backend.repository.AffiliateCommissionRepository;
import com.chamcham.backend.repository.AffiliateLinkRepository;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.TransactionRepository;
import com.chamcham.backend.repository.UserRepository;
import com.chamcham.backend.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

@Service
public class AffiliateService {

    public static final int COMMISSION_RATE_BASIS_POINTS = 100;
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final AffiliateLinkRepository linkRepository;
    private final AffiliateAttributionRepository attributionRepository;
    private final AffiliateCommissionRepository commissionRepository;
    private final UserRepository userRepository;
    private final CreatorRepository creatorRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String frontendBaseUrl;

    public AffiliateService(AffiliateLinkRepository linkRepository,
                            AffiliateAttributionRepository attributionRepository,
                            AffiliateCommissionRepository commissionRepository,
                            UserRepository userRepository,
                            CreatorRepository creatorRepository,
                            WalletRepository walletRepository,
                            TransactionRepository transactionRepository,
                            @Value("${app.frontend-base-url:http://localhost:3000}") String frontendBaseUrl) {
        this.linkRepository = linkRepository;
        this.attributionRepository = attributionRepository;
        this.commissionRepository = commissionRepository;
        this.userRepository = userRepository;
        this.creatorRepository = creatorRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public AffiliateOverviewResponse getOverview(UUID userId, UserRole role) {
        requireAffiliateCapable(role);
        AffiliateLink link = getOrCreateLink(userId, role);
        return overview(link);
    }

    @Transactional
    public AffiliateOverviewResponse createOrGetLink(UUID userId, UserRole role) {
        requireAffiliateCapable(role);
        return overview(getOrCreateLink(userId, role));
    }

    @Transactional(readOnly = true)
    public AffiliateCommissionPageResponse getCommissions(UUID userId, UserRole role, int page, int limit) {
        requireAffiliateCapable(role);
        Page<AffiliateCommission> result = commissionRepository.findByAffiliateOwnerIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(limit, 100)), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return new AffiliateCommissionPageResponse(
                result.getContent().stream().map(this::toCommissionResponse).toList(),
                result.getTotalElements(),
                page,
                limit
        );
    }

    @Transactional
    public void recordCreatorSignupAttribution(Creator referredCreator, String rawAffiliateCode) {
        String code = normalizeCode(rawAffiliateCode);
        if (code == null || referredCreator == null || referredCreator.getId() == null) {
            return;
        }
        if (attributionRepository.existsByReferredCreatorId(referredCreator.getId())) {
            return;
        }

        linkRepository.findByCodeIgnoreCaseAndActiveTrue(code).ifPresent(link -> {
            if (link.getOwner().getId().equals(referredCreator.getId())) {
                return;
            }
            AffiliateAttribution attribution = AffiliateAttribution.builder()
                    .affiliateLink(link)
                    .affiliateOwner(link.getOwner())
                    .referredCreator(referredCreator)
                    .sourceCode(link.getCode())
                    .build();
            attributionRepository.save(attribution);
        });
    }

    @Transactional
    public void releaseCommissionForCompletedOrder(Order order) {
        int amount = order.getAmount() == null ? 0 : order.getAmount();
        if (amount <= 0 || commissionRepository.existsByOrderId(order.getId())) {
            return;
        }

        attributionRepository.findByReferredCreatorId(order.getCreator().getId()).ifPresent(attribution -> {
            User affiliateOwner = attribution.getAffiliateOwner();
            if (affiliateOwner.getId().equals(order.getCreator().getId())) {
                return;
            }

            int commissionAmount = amount * COMMISSION_RATE_BASIS_POINTS / 10_000;
            if (commissionAmount <= 0) {
                return;
            }

            AffiliateCommissionStatus status = affiliateOwner.getRole().isCreator()
                    ? AffiliateCommissionStatus.CREDITED
                    : AffiliateCommissionStatus.PENDING_PAYOUT_UNSUPPORTED;

            AffiliateCommission commission = commissionRepository.save(AffiliateCommission.builder()
                    .affiliateOwner(affiliateOwner)
                    .earningCreator(order.getCreator())
                    .order(order)
                    .baseAmount(amount)
                    .rateBasisPoints(COMMISSION_RATE_BASIS_POINTS)
                    .commissionAmount(commissionAmount)
                    .status(status)
                    .build());

            if (status == AffiliateCommissionStatus.CREDITED) {
                creatorRepository.findById(affiliateOwner.getId()).ifPresent(affiliateCreator -> {
                    walletRepository.creditCreatorEarnings(affiliateCreator.getId(), commissionAmount);
                    String orderLabel = order.getOrderNumber() == null ? order.getId().toString() : order.getOrderNumber();
                    transactionRepository.save(Transaction.builder()
                            .creator(affiliateCreator)
                            .order(order)
                            .type(TransactionType.AFFILIATE_COMMISSION)
                            .amount(commission.getCommissionAmount())
                            .description("Affiliate commission from order " + orderLabel)
                            .status(TransactionStatus.COMPLETED)
                            .build());
                });
            }
        });
    }

    private AffiliateOverviewResponse overview(AffiliateLink link) {
        UUID ownerId = link.getOwner().getId();
        return new AffiliateOverviewResponse(
                link.getCode(),
                frontendBaseUrl + "/signup?affiliate=" + link.getCode(),
                COMMISSION_RATE_BASIS_POINTS,
                (int) commissionRepository.sumCommissionAmountByOwner(ownerId),
                attributionRepository.countByAffiliateOwnerId(ownerId),
                commissionRepository.countByAffiliateOwnerId(ownerId)
        );
    }

    private AffiliateCommissionResponse toCommissionResponse(AffiliateCommission commission) {
        Order order = commission.getOrder();
        Creator creator = commission.getEarningCreator();
        return new AffiliateCommissionResponse(
                commission.getId(),
                order.getId(),
                order.getOrderNumber(),
                creator.getId(),
                creator.getName(),
                commission.getBaseAmount(),
                commission.getRateBasisPoints(),
                commission.getCommissionAmount(),
                commission.getStatus().name().toLowerCase(),
                commission.getCreatedAt()
        );
    }

    private AffiliateLink getOrCreateLink(UUID userId, UserRole role) {
        return linkRepository.findByOwnerId(userId).orElseGet(() -> {
            User owner = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
            return linkRepository.save(AffiliateLink.builder()
                    .owner(owner)
                    .code(generateCode(owner, role))
                    .active(true)
                    .build());
        });
    }

    private String generateCode(User owner, UserRole role) {
        String prefix = role.isBrand() ? "BR" : "CR";
        String seed = owner.getUsername() == null ? "" : owner.getUsername().replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
        if (seed.length() > 6) seed = seed.substring(0, 6);
        if (seed.length() < 3) seed = prefix + randomSuffix(4);

        for (int i = 0; i < 8; i++) {
            String candidate = seed + randomSuffix(4);
            if (!linkRepository.existsByCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return prefix + randomSuffix(10);
    }

    private String randomSuffix(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return raw.trim().replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private void requireAffiliateCapable(UserRole role) {
        if (!role.isCreator() && !role.isBrand()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creators and brands can access affiliate features");
        }
    }
}
