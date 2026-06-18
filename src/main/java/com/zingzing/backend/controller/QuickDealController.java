package com.zingzing.backend.controller;

import com.zingzing.backend.config.security.AuthenticatedUser;
import com.zingzing.backend.dto.quickdeal.QuickDealCreateRequest;
import com.zingzing.backend.dto.quickdeal.QuickDealCreateResponse;
import com.zingzing.backend.dto.quickdeal.QuickDealRespondRequest;
import com.zingzing.backend.dto.quickdeal.QuickDealRespondResponse;
import com.zingzing.backend.entity.enums.OfferStatus;
import com.zingzing.backend.exception.ApiException;
import com.zingzing.backend.service.QuickDealService;
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

