package com.digiteen.userservice.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "traceId";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        String traceId = incoming != null && SAFE_ID.matcher(incoming).matches()
                ? incoming
                : UUID.randomUUID().toString();
        long started = System.nanoTime();

        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, traceId)) {
            response.setHeader(HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            log.atInfo()
                    .addKeyValue("event", "http_request_completed")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("path", request.getRequestURI())
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("durationMs", durationMs)
                    .log("HTTP request completed");
        }
    }
}
