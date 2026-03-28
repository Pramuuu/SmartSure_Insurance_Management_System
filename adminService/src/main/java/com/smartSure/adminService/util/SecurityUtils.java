package com.smartSure.adminService.util;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static String getCurrentRole() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
        		.iterator()
                .next().getAuthority();
    }
}