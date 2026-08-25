package com.rate_limiter_gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
public class RateLimiterService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean isAllowed(String apiKey, int maxRequests, long windowInSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = now - (windowInSeconds * 1000);
        String redisKey = "rate_limit:" + apiKey;

        // Script Lua atômico para evitar Race Conditions no Redis
        String luaScript =
                "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1]) " +
                        "local currentRequests = redis.call('ZCARD', KEYS[1]) " +
                        "if currentRequests < tonumber(ARGV[2]) then " +
                        "    redis.call('ZADD', KEYS[1], ARGV[3], ARGV[4]) " +
                        "    redis.call('EXPIRE', KEYS[1], ARGV[5]) " +
                        "    return 1 " +
                        "else " +
                        "    return 0 " +
                        "end";

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(redisKey),
                String.valueOf(windowStart),
                String.valueOf(maxRequests),
                String.valueOf(now),
                now + "-" + UUID.randomUUID(),
                String.valueOf(windowInSeconds)
        );

        return result != null && result == 1;
    }
}

