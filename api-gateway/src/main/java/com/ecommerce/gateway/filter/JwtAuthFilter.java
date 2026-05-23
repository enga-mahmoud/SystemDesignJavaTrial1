package com.ecommerce.gateway.filter;

import com.ecommerce.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final List<String> IDENTITY_HEADERS = List.of("X-User-Id", "X-User-Role");

    // Fully public for all methods
    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/"
    );

    // Public for GET only
    private static final List<String> PUBLIC_GET_PATHS = List.of(
        "/api/products",
        "/api/search"
    );

    // Actuator health only — never expose the full /actuator/ tree externally
    private static final String ACTUATOR_HEALTH = "/actuator/health";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        if (isPublicPath(path, method)) {
            // Strip identity headers even on public paths — never trust client-supplied values
            return chain.filter(exchange.mutate().request(stripIdentityHeaders(exchange)).build());
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange);
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.validateAndParse(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            // Use set() via headers() to replace any client-supplied values — never append
            ServerHttpRequest mutatedRequest = stripIdentityHeaders(exchange).mutate()
                .headers(h -> {
                    h.set("X-User-Id", userId);
                    h.set("X-User-Role", role != null ? role : "USER");
                })
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed for path={} reason={}", path, e.getMessage());
            return unauthorizedResponse(exchange);
        }
    }

    private ServerHttpRequest stripIdentityHeaders(ServerWebExchange exchange) {
        return exchange.getRequest().mutate()
            .headers(h -> IDENTITY_HEADERS.forEach(h::remove))
            .build();
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        if (path.equals(ACTUATOR_HEALTH)) return true;
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) return true;
        }
        if (HttpMethod.GET.equals(method)) {
            for (String getPath : PUBLIC_GET_PATHS) {
                if (path.startsWith(getPath)) return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
