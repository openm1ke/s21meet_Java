package ru.izpz.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.LongSupplier;

@Component
public class ProjectExecutorsRateLimitFilter extends OncePerRequestFilter {

    private static final String DEFAULT_TARGET_PATH = "/api/projects/executors";
    private static final String TARGET_METHOD = "POST";
    private static final int CLEANUP_THRESHOLD = 200;

    private final boolean enabled;
    private final String targetPath;
    private final int limitForPeriod;
    private final long refreshPeriodMillis;
    private final LongSupplier nowMillisSupplier;
    private final Map<String, ClientWindow> windows = new HashMap<>();
    private final Object lock = new Object();
    private int requestCounter;

    @Autowired
    public ProjectExecutorsRateLimitFilter(
        @Value("${rate-limit.project-executors.enabled:true}") boolean enabled,
        @Value("${rate-limit.project-executors.path:" + DEFAULT_TARGET_PATH + "}") String targetPath,
        @Value("${rate-limit.project-executors.limit-for-period:60}") int limitForPeriod,
        @Value("${rate-limit.project-executors.refresh-period:PT1M}") Duration refreshPeriod
    ) {
        this(enabled, targetPath, limitForPeriod, refreshPeriod, System::currentTimeMillis);
    }

    ProjectExecutorsRateLimitFilter(
        boolean enabled,
        String targetPath,
        int limitForPeriod,
        Duration refreshPeriod,
        LongSupplier nowMillisSupplier
    ) {
        this.enabled = enabled;
        this.targetPath = (targetPath == null || targetPath.isBlank()) ? DEFAULT_TARGET_PATH : targetPath;
        this.limitForPeriod = Math.max(1, limitForPeriod);
        this.refreshPeriodMillis = Math.max(1L, refreshPeriod.toMillis());
        this.nowMillisSupplier = nowMillisSupplier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        return !TARGET_METHOD.equalsIgnoreCase(request.getMethod()) || !targetPath.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String key = resolveClientKey(request);
        if (allowRequest(key)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Too many requests\"}");
    }

    private boolean allowRequest(String key) {
        long now = nowMillisSupplier.getAsLong();
        synchronized (lock) {
            ClientWindow window = windows.get(key);
            if (window == null || isExpired(window.windowStartMillis, now)) {
                windows.put(key, new ClientWindow(now, 1));
                cleanupIfNeeded(now);
                return true;
            }
            if (window.requestCount >= limitForPeriod) {
                cleanupIfNeeded(now);
                return false;
            }
            window.requestCount++;
            cleanupIfNeeded(now);
            return true;
        }
    }

    private boolean isExpired(long startedAtMillis, long nowMillis) {
        return nowMillis - startedAtMillis >= refreshPeriodMillis;
    }

    private void cleanupIfNeeded(long now) {
        requestCounter++;
        if (requestCounter < CLEANUP_THRESHOLD) {
            return;
        }
        requestCounter = 0;
        Iterator<Map.Entry<String, ClientWindow>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ClientWindow> entry = iterator.next();
            if (now - entry.getValue().windowStartMillis >= refreshPeriodMillis * 2) {
                iterator.remove();
            }
        }
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            String first = commaIndex >= 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor;
            String trimmed = first.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static final class ClientWindow {
        private final long windowStartMillis;
        private int requestCount;

        private ClientWindow(long windowStartMillis, int requestCount) {
            this.windowStartMillis = windowStartMillis;
            this.requestCount = requestCount;
        }
    }
}
