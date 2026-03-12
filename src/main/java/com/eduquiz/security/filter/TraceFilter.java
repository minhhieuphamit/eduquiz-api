package com.eduquiz.security.filter;

/**
 * Request Trace ID Filter (extends OncePerRequestFilter).
 * <p>
 * Flow:
 * 1. Generate UUID → traceId
 * 2. MDC.put("traceId", traceId)
 * 3. MDC.put("userId", extractFromSecurityContext nếu có)
 * 4. response.setHeader("X-Trace-Id", traceId)
 * 5. finally → MDC.clear()
 * <p>
 * → Logback sẽ tự gắn traceId vào mọi log entry (cấu hình trong logback-spring.xml)
 * TODO: Implement
 */
public class TraceFilter {
}
