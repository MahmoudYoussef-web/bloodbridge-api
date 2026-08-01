package com.bloodbridge.bloodbridge.config;

import com.bloodbridge.bloodbridge.entity.User;
import com.bloodbridge.bloodbridge.enumtype.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class EmailVerificationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User user)) {
            return true;
        }

        if (user.getRole() == UserRole.ADMIN) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.contains("/auth/") || path.contains("/verify") || path.contains("/ineligible")
                || path.contains("/pending-approval")) {
            return true;
        }

        if (!user.isEmailVerified() && !user.isPhoneVerified()) {
            response.setStatus(403);
            return false;
        }

        return true;
    }
}
