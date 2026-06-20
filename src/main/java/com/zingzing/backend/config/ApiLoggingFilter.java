package com.zingzing.backend.config;

import com.zingzing.backend.entity.ApiLog;
import com.zingzing.backend.repository.ApiLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiLoggingFilter extends OncePerRequestFilter {

    private final ApiLogRepository apiLogRepository;

    public ApiLoggingFilter(ApiLogRepository apiLogRepository) {
        this.apiLogRepository = apiLogRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        Exception thrown = null;
        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            thrown = ex;
            throw ex;
        } finally {
            String path = request.getRequestURI();
            if (path != null && path.startsWith("/api/")) {
                int duration = Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start));
                String error = thrown == null ? null : thrown.getMessage();
                apiLogRepository.save(ApiLog.builder()
                        .method(truncate(request.getMethod(), 10))
                        .path(truncate(path, 500))
                        .statusCode(response.getStatus())
                        .durationMs(duration)
                        .service(serviceBucket(path))
                        .errorMessage(truncate(error, 1000))
                        .build());
            }
        }
    }

    private String serviceBucket(String path) {
        if (path == null) return "api";
        String[] parts = path.split("/");
        if (parts.length >= 4 && "api".equals(parts[1]) && parts[2].startsWith("v")) {
            return parts[3].isBlank() ? "api" : parts[3];
        }
        return "api";
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
