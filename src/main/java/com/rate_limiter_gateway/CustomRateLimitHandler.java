package com.rate_limiter_gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class CustomRateLimitHandler implements WebExceptionHandler {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {

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
        return Mono.error(ex);
    }
    public boolean handleRequest(String apiKey) {
        // Exemplo: Limite de 10 requisições a cada 60 segundos por apiKey
        boolean allowed = rateLimiterService.isAllowed(apiKey, 10, 60);

        if (!allowed) {
            // Lógica para retornar HTTP Status 429 Too Many Requests
            return false;
        }

        return true;
    }
}