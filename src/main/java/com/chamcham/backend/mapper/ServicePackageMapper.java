package com.chamcham.backend.mapper;

import com.chamcham.backend.dto.servicepackage.ServicePackageResponse;
import com.chamcham.backend.dto.servicepackage.ServicePackageTierResponse;
import com.chamcham.backend.entity.ServicePackage;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ServicePackageMapper {

    public ServicePackageResponse toResponse(ServicePackage servicePackage) {
        return new ServicePackageResponse(
                servicePackage.getId(),
                servicePackage.getCreator().getId(),
                servicePackage.getName(),
                servicePackage.getTitle(),
                servicePackage.getDescription(),
                servicePackage.getPlatform(),
                servicePackage.getCategory(),
                servicePackage.getType(),
                servicePackage.getPricingType(),
                servicePackage.getBarterDetails(),
                servicePackage.getPrice(),
                servicePackage.getCurrency(),
                servicePackage.getDeliverables(),
                servicePackage.getDeliveryDays(),
                servicePackage.getDurationDays(),
                servicePackage.getRevisions(),
                servicePackage.isActive(),
                servicePackage.isFeatured(),
                servicePackage.getCoverImage(),
                arrayToList(servicePackage.getMediaUrls()),
                arrayToList(servicePackage.getTags()),
                servicePackage.getTiers().stream()
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
                servicePackage.getCreatedAt(),
                servicePackage.getUpdatedAt()
        );
    }

    private List<String> arrayToList(String[] values) {
        return values == null ? List.of() : Arrays.asList(values);
    }
}



