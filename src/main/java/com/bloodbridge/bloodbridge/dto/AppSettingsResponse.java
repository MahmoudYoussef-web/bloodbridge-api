package com.bloodbridge.bloodbridge.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record AppSettingsResponse(
    Map<String, String> siteName,
    Map<String, String> siteSlogan,
    String siteLogo,
    String siteFavicon,
    String supportEmail,
    String supportPhone,
    Map<String, String> address,
    Map<String, String> socialLinks,
    int minDonorAge,
    int maxDonorAge,
    int minDonorWeight,
    int minDonorHeight,
    int minDaysBetweenDonations,
    int minDaysAfterSurgery,
    int orgMaxRequestsPerDay,
    boolean maintenanceMode,
    boolean enableContactMessages,
    Map<String, String> loginTitle,
    Map<String, String> loginSubtitle,
    Map<String, String> signupTitle,
    Map<String, String> signupSubtitle,
    Map<String, String> heroTitle,
    Map<String, String> heroSubtitle
) {}
