package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.quickdeal.QuickDealCreateRequest;
import com.chamcham.backend.dto.quickdeal.QuickDealCreateResponse;
import com.chamcham.backend.dto.quickdeal.QuickDealRespondRequest;
import com.chamcham.backend.dto.quickdeal.QuickDealRespondResponse;
import com.chamcham.backend.entity.enums.OfferStatus;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.service.QuickDealService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quick-deals")
public class QuickDealController {

    private final QuickDealService quickDealService;

    public QuickDealController(QuickDealService quickDealService) {
        this.quickDealService = quickDealService;
    }

    @PostMapping
    public ResponseEntity<QuickDealCreateResponse> createQuickDeal(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody QuickDealCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quickDealService.createOffer(authUser.userId(), authUser.role(), request));
    }

    @PatchMapping("/{offerId}/respond")
    public ResponseEntity<QuickDealRespondResponse> respondToQuickDeal(
            @PathVariable UUID offerId,
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody QuickDealRespondRequest request
    ) {
        OfferStatus response;
        try {
            response = OfferStatus.valueOf(request.action().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "action must be accepted or rejected");
        }

        return ResponseEntity.ok(quickDealService.respond(offerId, authUser.userId(), authUser.role(), response));
    }
}

