package com.ga.waslah.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits requests to selected authentication endpoints within a time window.
 * Each client address and endpoint has its own request counter.
 */
@Component
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final Map<String, RequestCounter> requests =
            new ConcurrentHashMap<>();

    private static final int LOGIN_LIMIT = 5;
    private static final int REGISTER_LIMIT = 5;
    private static final int FORGOT_PASSWORD_LIMIT = 3;
    private static final int RESET_PASSWORD_LIMIT = 3;

    private static final long WINDOW_SECONDS = 60;

    /**
     * Checks whether a request exceeds the limit for its endpoint.
     *
     * @param request incoming HTTP request
     * @param response HTTP response to update if the request is rejected
     * @param handler selected request handler
     * @return true if the request may continue; false if it is rate-limited
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        String path = request.getRequestURI();

        // Apply rate limiting only to the selected authentication endpoints.
        int limit;

        if (path.equals("/auth/login")) {
            limit = LOGIN_LIMIT;
        } else if (path.equals("/auth/register")) {
            limit = REGISTER_LIMIT;
        } else if (path.equals("/auth/forgot-password")) {
            limit = FORGOT_PASSWORD_LIMIT;
        } else if (path.equals("/auth/reset-password")) {
            limit = RESET_PASSWORD_LIMIT;
        } else {
            return true;
        }

        // Keep a separate counter for each client address and endpoint.
        String key = request.getRemoteAddr() + ":" + path;

        RequestCounter counter = requests.computeIfAbsent(
                key,
                k -> new RequestCounter()
        );

        synchronized (counter) {

            long now = Instant.now().getEpochSecond();

            // Start a new counting window after 60 seconds.
            if (now - counter.startTime >= WINDOW_SECONDS) {
                counter.startTime = now;
                counter.count = 0;
            }

            counter.count++;

            // Reject requests once the endpoint's allowed limit is exceeded.
            if (counter.count > limit) {

                log.warn(
                        "Rate limit exceeded for endpoint {}. Allowed requests: {}",
                        path,
                        limit
                );

                response.setStatus(
                        HttpStatus.TOO_MANY_REQUESTS.value()
                );

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                try {
                    response.getWriter().write(
                            """
                            {
                                "status": 429,
                                "error": "TOO_MANY_REQUESTS",
                                "message": "Too many requests. Please try again later."
                            }
                            """
                    );
                } catch (Exception exception) {
                    log.error(
                            "Failed to write rate-limit response for endpoint {}",
                            path,
                            exception
                    );
                }

                return false;
            }
        }

        return true;
    }

    /**
     * Stores the request count and the start time of the current time window.
     * Access to each counter is synchronized in preHandle.
     */
    private static class RequestCounter {

        private long startTime = Instant.now().getEpochSecond();
        private int count = 0;
    }
}
