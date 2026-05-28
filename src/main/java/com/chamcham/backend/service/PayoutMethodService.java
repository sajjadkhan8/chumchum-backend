package com.chamcham.backend.service;

import com.chamcham.backend.entity.Creator;
import com.chamcham.backend.entity.PayoutMethod;
import com.chamcham.backend.entity.enums.PayoutMethodType;
import com.chamcham.backend.entity.enums.UserRole;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.repository.CreatorRepository;
import com.chamcham.backend.repository.PayoutMethodRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PayoutMethodService {

    private final PayoutMethodRepository payoutMethodRepository;
    private final CreatorRepository creatorRepository;

    public PayoutMethodService(PayoutMethodRepository payoutMethodRepository, CreatorRepository creatorRepository) {
        this.payoutMethodRepository = payoutMethodRepository;
        this.creatorRepository = creatorRepository;
    }

    public List<PayoutMethod> list(UUID userId, UserRole role) {
        requireCreator(role);
        return payoutMethodRepository.findByCreatorId(userId);
    }

    @Transactional
    public PayoutMethod create(UUID userId, UserRole role, PayoutMethodType type, String name,
                               String accountDetails, boolean isDefault) {
        requireCreator(role);
        Creator creator = findCreator(userId);
        if (isDefault) clearDefault(userId);
        PayoutMethod pm = PayoutMethod.builder()
                .creator(creator)
                .type(type)
                .name(name)
                .accountDetails(accountDetails)
                .isDefault(isDefault)
                .build();
        return payoutMethodRepository.save(pm);
    }

    @Transactional
    public PayoutMethod update(UUID pmId, UUID userId, String name, String accountDetails, Boolean isDefault) {
        PayoutMethod pm = findOwnedByCreator(pmId, userId);
        if (name != null) pm.setName(name);
        if (accountDetails != null) pm.setAccountDetails(accountDetails);
        if (Boolean.TRUE.equals(isDefault)) { clearDefault(userId); pm.setDefault(true); }
        return payoutMethodRepository.save(pm);
    }

    @Transactional
    public void delete(UUID pmId, UUID userId) {
        PayoutMethod pm = findOwnedByCreator(pmId, userId);
        payoutMethodRepository.delete(pm);
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

