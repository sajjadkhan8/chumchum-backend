package com.chamcham.backend.service;

import com.chamcham.backend.dto.gig.GigCreateRequest;
import com.chamcham.backend.dto.gig.GigResponse;
import com.chamcham.backend.entity.Gig;
import com.chamcham.backend.entity.User;
import com.chamcham.backend.exception.ApiException;
import com.chamcham.backend.mapper.GigMapper;
import com.chamcham.backend.repository.GigRepository;
import com.chamcham.backend.repository.UserRepository;
import com.chamcham.backend.util.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class GigService {

    private final GigRepository gigRepository;
    private final UserRepository userRepository;
    private final GigMapper gigMapper;

    public GigService(GigRepository gigRepository, UserRepository userRepository, GigMapper gigMapper) {
        this.gigRepository = gigRepository;
        this.userRepository = userRepository;
        this.gigMapper = gigMapper;
    }

    public GigResponse createGig(UUID userId, boolean isSeller, GigCreateRequest request) {
        if (!isSeller) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only sellers can create new Gigs!");
        }

        User seller = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Gig gig = Gig.builder()
                .id(UUID.randomUUID())
                .seller(seller)
                .title(request.title())
                .description(request.description())
                .totalStars(0)
                .starNumber(0)
                .category(request.category())
                .price(request.price())
                .cover(request.cover())
                .images(request.images() == null ? List.of() : request.images())
                .shortTitle(request.shortTitle())
                .shortDescription(request.shortDescription())
                .deliveryTime(request.deliveryTime())
                .revisionNumber(request.revisionNumber())
                .features(request.features() == null ? List.of() : request.features())
                .sales(0)
                .build();

        return gigMapper.toResponse(gigRepository.save(gig));
    }

    public void deleteGig(UUID gigId, UUID userId) {
        Gig gig = gigRepository.findById(gigId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gig not found"));

        if (!gig.getSeller().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid request! Cannot delete other user gigs!");
        }

        gigRepository.delete(gig);
    }

    public GigResponse getGig(UUID gigId) {
        Gig gig = gigRepository.findById(gigId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gig not found!"));
        return gigMapper.toResponse(gig);
    }

    public PageResponse<GigResponse> getGigs(
            String category,
            String search,
            BigDecimal min,
            BigDecimal max,
            UUID userId,
            int page,
            int size,
            String sortBy
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));
        Page<Gig> gigs;

        if (userId != null) {
            User seller = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
            gigs = gigRepository.findBySeller(seller, pageable);
        } else {
            gigs = gigRepository.findByCategoryContainingIgnoreCaseAndTitleContainingIgnoreCaseAndPriceBetween(
                    category == null ? "" : category,
                    search == null ? "" : search,
                    min == null ? BigDecimal.ZERO : min,
                    max == null ? new BigDecimal("999999999") : max,
                    pageable
            );
        }

        return PageResponse.from(gigs.map(gigMapper::toResponse));
    }
}

