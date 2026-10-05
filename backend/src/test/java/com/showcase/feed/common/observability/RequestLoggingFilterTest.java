package com.showcase.feed.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestLoggingFilterTest {

    @Test
    void setsRequestIdHeaderAndClearsMdcAfterwards() throws Exception {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/feed");

        filter.doFilter(request, response, chain);

        verify(response).setHeader(eq("X-Request-Id"), anyString());
        verify(chain).doFilter(request, response);
        // MDC must not leak into the next request handled by the same thread
        assertNull(MDC.get("requestId"));
    }

    @Test
    void clearsMdcEvenWhenDownstreamThrows() {
        RequestLoggingFilter filter = new RequestLoggingFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/feed");

        try {
            org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(chain).doFilter(request, response);
            filter.doFilter(request, response, chain);
        } catch (Exception ignored) {
            // expected to propagate
        }

        assertTrue(MDC.get("requestId") == null);
    }
}
