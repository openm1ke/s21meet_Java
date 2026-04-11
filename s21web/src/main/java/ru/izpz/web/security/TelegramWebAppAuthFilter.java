package ru.izpz.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TelegramWebAppAuthFilter extends OncePerRequestFilter {

    private final TelegramInitDataValidator validator;

    @Value("${telegram.webapp.auth.enabled:true}")
    private boolean enabled;

    @Value("${telegram.webapp.auth.header-name:X-Telegram-Init-Data}")
    private String headerName;

    @Value("${telegram.webapp.auth.path-prefix:/api/projects}")
    private String pathPrefix;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        return !request.getRequestURI().startsWith(pathPrefix);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String initData = request.getHeader(headerName);
        if (validator.isValid(initData)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Unauthorized Telegram Web App request\"}");
    }
}
