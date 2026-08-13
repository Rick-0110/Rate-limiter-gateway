package com.rate_limiter_gateway; // 👈 Ajuste para o pacote do seu projeto

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@Order(-2)
public class CustomRateLimitHandler implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {


        return chain.filter(exchange).then(Mono.defer(() -> {

            ServerHttpResponse response = exchange.getResponse();


            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {


                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);


                String jsonResponse = String.format("""
                    {
                      "timestamp": "%s",
                      "status": 429,
                      "error": "Too Many Requests",
                      "message": "Limite de requisições excedido. Aguarde alguns instantes."
                    }
                    """, Instant.now().toString());


                byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = response.bufferFactory().wrap(bytes);


                return response.writeWith(Mono.just(buffer));
            }

            return Mono.empty();
        }));
    }
}