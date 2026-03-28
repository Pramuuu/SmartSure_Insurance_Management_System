package com.smartSure.adminService.util;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;

public class HeaderUtils {

    public static void copyHeaders(HttpServletRequest request, RequestTemplate template) {

        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        String secret = request.getHeader("X-Internal-Secret");

        if (userId != null) {
            template.header("X-User-Id", userId);
        }

        if (role != null) {
            template.header("X-User-Role", role);
        }

        if (secret != null) {
            template.header("X-Internal-Secret", secret);
        }
    }
}