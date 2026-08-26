package com.rate_limiter_gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class CustomRateLimitHandler implements GlobalFilter, Ordered {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String apiKey = exchange.getRequest().getHeaders().getFirst("x-API-KEY");
        if (apiKey == null || apiKey.isBlank()) {
            if (exchange.getRequest().getRemoteAddress() != null) {
                apiKey = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            } else {
                apiKey = "anonymous";
            }
        }

        boolean allowed = rateLimiterService.isAllowed(apiKey, 10, 60);

        if (!allowed) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String jsonResponse = """
                {
                    "status": 429,
                    "error": "Too Many Requests",
                    "message": "API rate limit exceeded. Please wait a moment before sending more requests."
                }
                """;

            byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

            return exchange.getResponse().writeWith(Mono.just(buffer));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}