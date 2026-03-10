package com.desafio.case_shortner.config;

import org.springframework.stereotype.Component;
import com.desafio.case_shortner.dataTransferObject.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String HEADER_NAME = "X-API-Key";

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${app.api-key}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only enforce API key on POST /v1/urls
        return !("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().equals("/v1/urls"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER_NAME);
        if (apiKey.equals(providedKey)) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("Unauthorized access attempt from {}", request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(401, "UNAUTHORIZED", "Valid X-API-Key header is required"));
    }
}
