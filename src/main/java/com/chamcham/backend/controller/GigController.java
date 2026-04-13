package com.chamcham.backend.controller;

import com.chamcham.backend.config.security.AuthenticatedUser;
import com.chamcham.backend.dto.gig.GigCreateRequest;
import com.chamcham.backend.dto.gig.GigResponse;
import com.chamcham.backend.service.GigService;
import com.chamcham.backend.util.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gigs")
public class GigController {

    private final GigService gigService;

    public GigController(GigService gigService) {
        this.gigService = gigService;
    }

    @PostMapping
    public ResponseEntity<GigResponse> createGig(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody GigCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gigService.createGig(authUser.userId(), authUser.seller(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteGig(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        gigService.deleteGig(id, authUser.userId());
        return ResponseEntity.ok(Map.of("error", false, "message", "Gig had been successfully deleted!"));
    }

    @GetMapping("/single/{id}")
    public ResponseEntity<GigResponse> getGig(@PathVariable UUID id) {
        return ResponseEntity.ok(gigService.getGig(id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<GigResponse>> getGigs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort
    ) {
        return ResponseEntity.ok(gigService.getGigs(category, search, min, max, userId, page, size, sort));
    }
}

