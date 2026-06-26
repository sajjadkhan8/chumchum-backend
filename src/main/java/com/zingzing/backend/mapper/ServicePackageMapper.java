package com.zingzing.backend.mapper;

import com.zingzing.backend.dto.servicepackage.ServicePackageResponse;
import com.zingzing.backend.entity.ServicePackage;
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
                servicePackage.getShortDescription(),
                servicePackage.getDescription(),
                servicePackage.getFullDescription(),
                servicePackage.getPlatform(),
                servicePackage.getCategory(),
                servicePackage.getDealType(),
                servicePackage.getBarterDetails(),
                servicePackage.getBarterDescription(),
                servicePackage.getHybridCashAmount(),
                servicePackage.getCreatorExpectations(),
                servicePackage.getPrice(),
                servicePackage.getCurrency(),
                servicePackage.getDeliverables(),
                servicePackage.getDeliveryDays(),
                servicePackage.getRevisions(),
                servicePackage.isActive(),
                servicePackage.isFeatured(),
                servicePackage.getStatus(),
                servicePackage.getVisibility(),
                servicePackage.isPopular(),
                servicePackage.getOrdersCompleted(),
                servicePackage.getCoverImage(),
                arrayToList(servicePackage.getMediaUrls()),
                servicePackage.getTags() == null ? List.of() : servicePackage.getTags(),
                servicePackage.getCreatedAt(),
                servicePackage.getUpdatedAt()
        );
    }

    private List<String> arrayToList(String[] values) {
        return values == null ? List.of() : Arrays.asList(values);
    }
}
