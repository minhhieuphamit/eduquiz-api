package com.eduquiz.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Slf4j
public class TraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Request-ID";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String MDC_TRACE_ID = "traceId";

    @org.springframework.beans.factory.annotation.Value("${api.security.api-key}")
    private String configuredApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        
        // Exclude public paths from header requirements
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Check X-API-Key (Mandatory)
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || !apiKey.equals(configuredApiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Missing or invalid X-API-Key\"}");
            return;
        }

        // 2. Check X-Request-ID (Mandatory)
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Missing X-Request-ID\"}");
            return;
        }

        MDC.put(MDC_TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/swagger-ui") || 
               path.startsWith("/v3/api-docs") || 
               path.startsWith("/actuator");
    }
}
