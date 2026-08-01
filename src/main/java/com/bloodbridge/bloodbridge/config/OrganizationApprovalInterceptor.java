package com.bloodbridge.bloodbridge.config;

import com.bloodbridge.bloodbridge.entity.Organization;
import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.OrganizationStatus;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import com.bloodbridge.bloodbridge.repository.OrganizationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class OrganizationApprovalInterceptor implements HandlerInterceptor {

    private final OrganizationRepository organizationRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            return true;
        }

        if (user.getRole() != UserRole.ORGANIZATION) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.contains("/pending-approval") || path.contains("/auth/")) {
            return true;
        }

        return organizationRepository.findByUserId(user.getId())
                .map(org -> {
                    if (org.getApprovalStatus() == OrganizationStatus.PENDING
                            || org.getApprovalStatus() == OrganizationStatus.REJECTED) {
                        response.setStatus(403);
                        return false;
                    }
                    return true;
                })
                .orElse(true);
    }
}
