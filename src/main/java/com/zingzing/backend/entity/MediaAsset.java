package com.zingzing.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "media_assets", schema = "core")
public class MediaAsset extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "scope", nullable = false, length = 40)
    private String scope;

    @Column(name = "entity_type", length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "public_id", nullable = false, length = 300)
    private String publicId;

    @Column(name = "asset_id", length = 100)
    private String assetId;

    @Column(nullable = false, length = 20)
    private String resourceType;

    @Column(length = 20)
    private String format;

    @Column(name = "secure_url", nullable = false, length = 800)
    private String secureUrl;

    @Column(name = "app_path", length = 800)
    private String appPath;

    @Column(name = "thumbnail_url", length = 800)
    private String thumbnailUrl;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(nullable = false)
    private long bytes;

    private Integer width;
    private Integer height;
    private Double duration;

    @Column(nullable = false)
    private boolean deleted = false;
}
