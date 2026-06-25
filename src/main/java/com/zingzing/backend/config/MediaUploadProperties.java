package com.zingzing.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.media")
public class MediaUploadProperties {

    private String rootFolder = "chumchum";
    private long userStorageLimitMb = 2_048;
    private long packageStorageLimitMb = 250;
    private long campaignStorageLimitMb = 250;
    private int userUploadCountLimit = 1_000;
    private int packageUploadCountLimit = 50;
    private int campaignUploadCountLimit = 50;
    private Map<String, UploadRule> uploads = Map.of(
            "avatar", new UploadRule(5, List.of("image/jpeg", "image/png", "image/webp"), "image"),
            "cover-image", new UploadRule(10, List.of("image/jpeg", "image/png", "image/webp"), "image"),
            "content-preview", new UploadRule(100, List.of("image/jpeg", "image/png", "image/webp", "video/mp4", "video/quicktime"), "auto"),
            "package-thumbnail", new UploadRule(5, List.of("image/jpeg", "image/png", "image/webp"), "image"),
            "campaign-cover", new UploadRule(10, List.of("image/jpeg", "image/png", "image/webp"), "image"),
            "brand-logo", new UploadRule(5, List.of("image/jpeg", "image/png", "image/webp"), "image"),
            "message-attachment", new UploadRule(100, List.of("image/jpeg", "image/png", "image/webp", "video/mp4", "video/quicktime", "application/pdf", "application/zip"), "auto"),
            "deliverable", new UploadRule(500, List.of("image/jpeg", "image/png", "image/webp", "video/mp4", "video/quicktime", "application/pdf", "application/zip"), "auto"),
            "verification-document", new UploadRule(25, List.of("image/jpeg", "image/png", "image/webp", "application/pdf"), "auto")
    );

    public String getRootFolder() { return rootFolder; }
    public void setRootFolder(String rootFolder) { this.rootFolder = rootFolder; }
    public long getUserStorageLimitMb() { return userStorageLimitMb; }
    public void setUserStorageLimitMb(long userStorageLimitMb) { this.userStorageLimitMb = userStorageLimitMb; }
    public long getPackageStorageLimitMb() { return packageStorageLimitMb; }
    public void setPackageStorageLimitMb(long packageStorageLimitMb) { this.packageStorageLimitMb = packageStorageLimitMb; }
    public long getCampaignStorageLimitMb() { return campaignStorageLimitMb; }
    public void setCampaignStorageLimitMb(long campaignStorageLimitMb) { this.campaignStorageLimitMb = campaignStorageLimitMb; }
    public int getUserUploadCountLimit() { return userUploadCountLimit; }
    public void setUserUploadCountLimit(int userUploadCountLimit) { this.userUploadCountLimit = userUploadCountLimit; }
    public int getPackageUploadCountLimit() { return packageUploadCountLimit; }
    public void setPackageUploadCountLimit(int packageUploadCountLimit) { this.packageUploadCountLimit = packageUploadCountLimit; }
    public int getCampaignUploadCountLimit() { return campaignUploadCountLimit; }
    public void setCampaignUploadCountLimit(int campaignUploadCountLimit) { this.campaignUploadCountLimit = campaignUploadCountLimit; }
    public Map<String, UploadRule> getUploads() { return uploads; }
    public void setUploads(Map<String, UploadRule> uploads) { this.uploads = uploads; }

    public UploadRule rule(String kind) {
        UploadRule rule = uploads.get(kind);
        if (rule == null) throw new IllegalArgumentException("Unsupported upload kind: " + kind);
        return rule;
    }

    public record UploadRule(long maxMb, List<String> allowedTypes, String resourceType) {}
}
