package com.ga.waslah.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingInterceptorTest {

    private RateLimitingInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitingInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void loginShouldAllowFirstFiveRequests() throws Exception {

        when(request.getRequestURI()).thenReturn("/auth/login");

        for (int i = 0; i < 5; i++) {
            boolean result =
                    interceptor.preHandle(request, response, null);

            assertTrue(result);
        }
    }

    @Test
    void loginShouldBlockAfterFiveRequests() throws Exception {

        when(request.getRequestURI()).thenReturn("/auth/login");

        // First 5 requests are allowed
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }

        // 6th request should be blocked
        boolean result =
                interceptor.preHandle(request, response, null);

        assertFalse(result);

        verify(response).setStatus(429);
    }

    @Test
    void registerShouldBlockAfterFiveRequests() throws Exception {

        when(request.getRequestURI()).thenReturn("/auth/register");

        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }

        boolean result =
                interceptor.preHandle(request, response, null);

        assertFalse(result);

        verify(response).setStatus(429);
    }

    @Test
    void forgotPasswordShouldBlockAfterThreeRequests() throws Exception {

        when(request.getRequestURI())
                .thenReturn("/auth/forgot-password");

        for (int i = 0; i < 3; i++) {
            interceptor.preHandle(request, response, null);
        }

        boolean result =
                interceptor.preHandle(request, response, null);

        assertFalse(result);

        verify(response).setStatus(429);
    }

    @Test
    void resetPasswordShouldBlockAfterThreeRequests() throws Exception {

        when(request.getRequestURI())
                .thenReturn("/auth/reset-password");

        for (int i = 0; i < 3; i++) {
            interceptor.preHandle(request, response, null);
        }

        boolean result =
                interceptor.preHandle(request, response, null);

        assertFalse(result);

        verify(response).setStatus(429);
    }

    @Test
    void unrelatedEndpointShouldNotBeRateLimited() throws Exception {

        when(request.getRequestURI()).thenReturn("/users");

        for (int i = 0; i < 10; i++) {

            boolean result =
                    interceptor.preHandle(request, response, null);

            assertTrue(result);
        }
    }
}