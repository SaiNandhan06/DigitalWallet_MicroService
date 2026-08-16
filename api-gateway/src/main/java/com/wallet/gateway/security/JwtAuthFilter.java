package com.wallet.gateway.security;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Skip JWT validation for auth endpoints (/auth/signup, /auth/login)
        if (path.startsWith("/auth/")) {
            return chain.filter(exchange);
        }

        // 2. Read Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7).trim();

        // 3. Validate JWT Token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT token for path: {}", path);
            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
        }

        // 4. Role-based Access Control (RBAC)
        String role = jwtUtil.extractRole(token);
        boolean isAdminRequired = isAdminOnlyEndpoint(path);

        if (isAdminRequired && !"ADMIN".equalsIgnoreCase(role)) {
            log.warn("Access denied for role '{}' requesting admin endpoint: {}", role, path);
            return onError(exchange, HttpStatus.FORBIDDEN, "Access denied. Admin role required.");
        }

        return chain.filter(exchange);
    }

    private boolean isAdminOnlyEndpoint(String path) {
        if ("/transactions/all".equalsIgnoreCase(path)) {
            return true;
        }
        if (path.startsWith("/wallets/") && (path.endsWith("/freeze") || path.endsWith("/unfreeze"))) {
            return true;
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // Execute early in filter chain
    }
}
