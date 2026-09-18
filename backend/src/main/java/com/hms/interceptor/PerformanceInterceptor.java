package com.hms.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "hms.requestStartNanos";

    @Value("${hms.performance.slow-request-threshold-ms:1000}")
    private long slowRequestThresholdMs;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startNanos = request.getAttribute(START_TIME_ATTRIBUTE);
        if (startNanos == null) {
            return;
        }

        long durationMs = (System.nanoTime() - (Long) startNanos) / 1_000_000;
        if (durationMs < slowRequestThresholdMs) {
            return;
        }

        log.warn("Slow request: {} {} took {}ms (threshold {}ms) | handler: {}",
                request.getMethod(),
                request.getRequestURI(),
                durationMs,
                slowRequestThresholdMs,
                describeHandler(handler));
    }

    private String describeHandler(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName();
        }
        return String.valueOf(handler);
    }
}
