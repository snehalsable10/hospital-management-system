package com.hms.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Generate correlation ID for request tracing
        MDC.put("requestId", UUID.randomUUID().toString());

        // Wrap request and response to capture body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Log incoming request
            logIncomingRequest(wrappedRequest);

            // Continue with filter chain
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            // Log outgoing response
            long duration = System.currentTimeMillis() - startTime;
            logOutgoingResponse(wrappedRequest, wrappedResponse, duration);

        } finally {
            // Copy response body back to response
            wrappedResponse.copyBodyToResponse();
            // Clear MDC
            MDC.remove("requestId");
        }
    }

    private void logIncomingRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullUri = query != null ? uri + "?" + query : uri;

        StringBuilder message = new StringBuilder();
        message.append("HTTP Request: ").append(method).append(" ").append(fullUri);

        String contentType = request.getContentType();
        if (contentType != null) {
            message.append(" | Content-Type: ").append(contentType);
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !userAgent.isEmpty()) {
            message.append(" | User-Agent: ").append(userAgent);
        }

        message.append(" | RemoteAddr: ").append(request.getRemoteAddr());

        log.info(message.toString());

        // Log request body for POST/PUT/PATCH (if present)
        if (isBodyRequest(method)) {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content);
                if (body.length() > 1000) {
                    log.debug("Request Body (truncated): {}", body.substring(0, 1000));
                } else {
                    log.debug("Request Body: {}", body);
                }
            }
        }
    }

    private void logOutgoingResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
                                     long duration) {
        int status = response.getStatus();
        String method = request.getMethod();
        String uri = request.getRequestURI();

        StringBuilder message = new StringBuilder();
        message.append("HTTP Response: ").append(method).append(" ").append(uri)
          .append(" | Status: ").append(status)
          .append(" | Duration: ").append(duration).append("ms");

        long contentLength = response.getContentSize();
        if (contentLength > 0) {
            message.append(" | Size: ").append(contentLength).append(" bytes");
        }

        // Log based on status code
        if (status >= 500) {
            log.error(message.toString());
        } else if (status >= 400) {
            log.warn(message.toString());
        } else {
            log.info(message.toString());
        }

        // Log response body for errors or debug mode
        if (status >= 400) {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                String body = new String(content);
                if (body.length() > 500) {
                    log.debug("Response Body (truncated): {}", body.substring(0, 500));
                } else {
                    log.debug("Response Body: {}", body);
                }
            }
        }
    }

    private boolean isBodyRequest(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String uri = request.getRequestURI();
        // Skip logging for health checks and static resources
        return uri.startsWith("/actuator/health")
            || uri.startsWith("/swagger-ui")
            || uri.startsWith("/v3/api-docs")
            || uri.endsWith(".js")
            || uri.endsWith(".css")
            || uri.endsWith(".png")
            || uri.endsWith(".jpg")
            || uri.endsWith(".gif");
    }
}
