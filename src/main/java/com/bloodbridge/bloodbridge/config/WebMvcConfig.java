package com.bloodbridge.bloodbridge.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final DonorIneligibilityInterceptor donorIneligibilityInterceptor;
    private final OrganizationApprovalInterceptor organizationApprovalInterceptor;
    private final SyncUserLocaleInterceptor syncUserLocaleInterceptor;
    private final EmailVerificationInterceptor emailVerificationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(syncUserLocaleInterceptor)
                .addPathPatterns("/v1/**");

        registry.addInterceptor(emailVerificationInterceptor)
                .addPathPatterns("/v1/**");

        registry.addInterceptor(donorIneligibilityInterceptor)
                .addPathPatterns("/v1/donor/**");

        registry.addInterceptor(organizationApprovalInterceptor)
                .addPathPatterns("/v1/org/**");
    }
}
