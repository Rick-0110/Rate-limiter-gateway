package com.rate_limiter_gateway;


import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver(){
       return exchange -> {
           String apiKey = exchange.getRequest()
                   .getHeaders()
                   .getFirst("X-API-KEY");

           if (apiKey != null && !apiKey.isBlank()) {
               return Mono.just("api_key:" + apiKey);
           }
           String clientIp = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                   .getAddress()
                   .getHostAddress();

           return Mono.just("ip:" + clientIp);
       };
    }
}
