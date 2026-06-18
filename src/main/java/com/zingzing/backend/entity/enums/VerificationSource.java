package com.zingzing.backend.entity.enums;

/**
 * Indicates how a creator's social account stats (followers, engagement rate) were verified.
 * SELF = creator-entered, no external check performed.
 * PLATFORM_REVIEWED = manually reviewed by ZingZing team.
 * API_CONNECTED = pulled directly via platform OAuth at connect time.
 */
public enum VerificationSource {
    SELF,
    PLATFORM_REVIEWED,
    API_CONNECTED
}
