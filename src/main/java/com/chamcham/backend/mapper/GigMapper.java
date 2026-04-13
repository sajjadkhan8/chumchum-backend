package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.gig.GigResponse;
import com.chamcham.backend.entity.Gig;
import org.springframework.stereotype.Component;

@Component
public class GigMapper {

    private final UserMapper userMapper;

    public GigMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public GigResponse toResponse(Gig gig) {
        return new GigResponse(
                gig.getId(),
                userMapper.toResponse(gig.getCreator()),
                gig.getTitle(),
                gig.getDescription(),
                gig.getTotalStars(),
                gig.getStarNumber(),
                gig.getCategory(),
                gig.getPrice(),
                gig.getCover(),
                gig.getImages(),
                gig.getShortTitle(),
                gig.getShortDescription(),
                gig.getDeliveryTime(),
                gig.getRevisionNumber(),
                gig.getFeatures(),
                gig.getSales(),
                gig.getCreatedAt()
        );
    }
}

