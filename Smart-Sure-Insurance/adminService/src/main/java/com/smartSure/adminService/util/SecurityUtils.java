package com.smartSure.adminService.util;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Long.valueOf(principal.toString());
    }

    public static String getCurrentRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
        		.iterator()
                .next().getAuthority();
    }
}