package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.servicepackage.ServicePackageResponse;
import com.chamcham.backend.dto.servicepackage.ServicePackageTierResponse;
import com.chamcham.backend.entity.Package;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ServicePackageMapper {

    public ServicePackageResponse toResponse(Package aPackage) {
        return new ServicePackageResponse(
                aPackage.getId(),
                aPackage.getCreator().getId(),
                aPackage.getName(),
                aPackage.getTitle(),
                aPackage.getShortDescription(),
                aPackage.getDescription(),
                aPackage.getFullDescription(),
                aPackage.getPlatform(),
                aPackage.getCategory(),
                aPackage.getType(),
                aPackage.getDealType(),
                aPackage.getBarterDetails(),
                aPackage.getBarterDescription(),
                aPackage.getBarterCategory(),
                aPackage.getEstimatedBarterValue(),
                aPackage.getHybridCashAmount(),
                aPackage.getHybridBarterValue(),
                aPackage.getCreatorExpectations(),
                aPackage.getPrice(),
                aPackage.getCurrency(),
                aPackage.getDeliverables(),
                aPackage.getDeliveryDays(),
                aPackage.getRevisions(),
                aPackage.isActive(),
                aPackage.isFeatured(),
                aPackage.getStatus(),
                aPackage.getVisibility(),
                aPackage.isPopular(),
                aPackage.getOrdersCompleted(),
                aPackage.getResponseTime(),
                aPackage.getCoverImage(),
                arrayToList(aPackage.getMediaUrls()),
                aPackage.getTags() == null ? List.of() : aPackage.getTags(),
                aPackage.getTiers().stream()
                        .map(tier -> new ServicePackageTierResponse(
                                tier.getId(),
                                tier.getName(),
                                tier.getPrice(),
                                tier.getDeliverables(),
                                tier.getDeliveryDays(),
                                tier.getRevisions(),
                                tier.getCreatedAt()
                        ))
                        .toList(),
                aPackage.getCreatedAt(),
                aPackage.getUpdatedAt()
        );
    }

    private List<String> arrayToList(String[] values) {
        return values == null ? List.of() : Arrays.asList(values);
    }
}



