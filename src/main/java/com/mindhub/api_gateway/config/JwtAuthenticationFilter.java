package com.mindhub.api_gateway.config;

import io.jsonwebtoken.Claims;

import jakarta.ws.rs.core.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.GrantedAuthority;

import org.springframework.stereotype.Component;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter implements GatewayFilter {

    private final JwtUtils jwtUtils;

    @Autowired
    public JwtAuthenticationFilter(JwtUtils jwtUtils){
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        if (!path.startsWith("/api/auth")) {
            return extractToken(exchange)
                    .filter(this::isValidToken)
                    .map(token -> processToken(exchange, token))
                    .orElseGet(() -> onError(exchange,
                            "Authorization header is missing or invalid",
                            HttpStatus.UNAUTHORIZED));
        }
        return chain.filter(exchange);
    }

    private Optional<String> extractToken(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return Optional.ofNullable(token).filter(t -> t.startsWith("Bearer "));
    }

    private boolean isValidToken(String token) {
        try {
            String jwtToken = token.substring(7);
            if (jwtUtils.isTokenExpired(jwtToken)) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private Mono<Void> processToken(ServerWebExchange exchange, String token) {
        try {
            Claims claims = jwtUtils.parseClaims(token);
            return Mono.empty();
        } catch (Exception e) {
            return onError(exchange, "JWT Token validation failed", HttpStatus.UNAUTHORIZED);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private List<GrantedAuthority> extractAuthorities(Claims claims) {
        return new ArrayList<>();
    }
}
