package com.smartSure.PolicyService.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) return;

        HttpServletRequest request = attributes.getRequest();

        // Only forward required headers
        copyHeaderIfPresent(request, template, "X-User-Id");
        copyHeaderIfPresent(request, template, "X-User-Role");
        copyHeaderIfPresent(request, template, "X-Request-Id");
    }

    //  MOVE THIS OUTSIDE apply()
    private void copyHeaderIfPresent(HttpServletRequest request,
                                     RequestTemplate template,
                                     String headerName) {

        String value = request.getHeader(headerName);
        if (value != null && !value.isBlank()) {
            template.header(headerName, value);
        }
    }
}