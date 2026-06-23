package com.zingzing.backend.service;

import com.zingzing.backend.entity.Creator;
import com.zingzing.backend.entity.PayoutMethod;
import com.zingzing.backend.entity.enums.PayoutMethodType;
import com.zingzing.backend.entity.enums.UserRole;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.repository.CreatorRepository;
import com.zingzing.backend.repository.PayoutMethodRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PayoutMethodService {

    private final PayoutMethodRepository payoutMethodRepository;
    private final CreatorRepository creatorRepository;
    private final PaymentValidationService paymentValidationService;
    private final PaymentAuditService paymentAuditService;

    public PayoutMethodService(PayoutMethodRepository payoutMethodRepository,
                               CreatorRepository creatorRepository,
                               PaymentValidationService paymentValidationService,
                               PaymentAuditService paymentAuditService) {
        this.payoutMethodRepository = payoutMethodRepository;
        this.creatorRepository = creatorRepository;
        this.paymentValidationService = paymentValidationService;
        this.paymentAuditService = paymentAuditService;
    }

    public List<PayoutMethod> list(UUID userId, UserRole role) {
        requireCreator(role);
        return payoutMethodRepository.findByCreatorId(userId);
    }

    @Transactional
    public PayoutMethod create(UUID userId, UserRole role, PayoutMethodType type, String name,
                               String accountDetails, boolean isDefault, String bankName) {
        requireCreator(role);
        paymentValidationService.validateCreatorPayoutDetails(type, accountDetails);
        Creator creator = findCreator(userId);
        if (isDefault) clearDefault(userId);
        PayoutMethod pm = PayoutMethod.builder()
                .creator(creator)
                .type(type)
                .name(name)
                .bankName(bankName)
                .accountDetails(accountDetails)
                .isDefault(isDefault)
                .build();
        PayoutMethod saved = payoutMethodRepository.save(pm);
        paymentAuditService.log(userId, null, "CREATOR_PAYOUT_METHOD_CREATED", "payout_method", saved.getId().toString(),
                "type=" + saved.getType().name().toLowerCase());
        return saved;
    }

    @Transactional
    public PayoutMethod update(UUID pmId, UUID userId, String name, String accountDetails, Boolean isDefault, String bankName) {
        PayoutMethod pm = findOwnedByCreator(pmId, userId);
        if (name != null) pm.setName(name);
        if (bankName != null) pm.setBankName(bankName);
        if (accountDetails != null) {
            paymentValidationService.validateCreatorPayoutDetails(pm.getType(), accountDetails);
            pm.setAccountDetails(accountDetails);
        }
        if (Boolean.TRUE.equals(isDefault)) { clearDefault(userId); pm.setDefault(true); }
        PayoutMethod saved = payoutMethodRepository.save(pm);
        paymentAuditService.log(userId, null, "CREATOR_PAYOUT_METHOD_UPDATED", "payout_method", saved.getId().toString(),
                "isDefault=" + saved.isDefault());
        return saved;
    }

    @Transactional
    public void delete(UUID pmId, UUID userId) {
        PayoutMethod pm = findOwnedByCreator(pmId, userId);
        payoutMethodRepository.delete(pm);
        paymentAuditService.log(userId, null, "CREATOR_PAYOUT_METHOD_DELETED", "payout_method", pmId.toString(), "deleted=true");
    }

    private void clearDefault(UUID creatorId) {
        payoutMethodRepository.findByCreatorIdAndIsDefaultTrue(creatorId)
                .ifPresent(p -> { p.setDefault(false); payoutMethodRepository.save(p); });
    }

    private PayoutMethod findOwnedByCreator(UUID pmId, UUID creatorId) {
        PayoutMethod pm = payoutMethodRepository.findById(pmId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payout method not found"));
        if (!pm.getCreator().getId().equals(creatorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return pm;
    }

    private Creator findCreator(UUID id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Creator profile not found"));
    }

    private void requireCreator(UserRole role) {
        if (!role.isCreator()) throw new ApiException(HttpStatus.FORBIDDEN, "Only creators can manage payout methods");
    }
}

