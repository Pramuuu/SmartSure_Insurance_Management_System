package com.smartSure.PolicyService.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility to extract authenticated user info from the SecurityContext.
 *
 * The HeaderAuthenticationFilter sets the Authentication principal as a String
 * (the X-User-Id header value). This utility safely converts it to Long.
 *
 * FIX: Prior version cast directly to Long causing ClassCastException.
 */
public class SecurityUtils {

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    public static String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) {
            return null;
        }
        return auth.getAuthorities().iterator().next().getAuthority();
    }
}