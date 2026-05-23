package com.ecommerce.product.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

    private final StringRedisTemplate redisTemplate;

    @Value("${product.cache.ttl-seconds:600}")
    private long ttlSeconds;

    public Optional<String> getProduct(UUID id) {
        String value = redisTemplate.opsForValue().get("product:" + id);
        return Optional.ofNullable(value);
    }

    public void setProduct(UUID id, String json) {
        redisTemplate.opsForValue().set("product:" + id, json, ttlSeconds, TimeUnit.SECONDS);
        log.debug("Cached product id={}", id);
    }

    public void evictProduct(UUID id) {
        redisTemplate.delete("product:" + id);
        log.debug("Evicted product id={} from cache", id);
    }

    public Optional<String> getCategoryTree() {
        return Optional.ofNullable(redisTemplate.opsForValue().get("category:tree"));
    }

    public void setCategoryTree(String json) {
        redisTemplate.opsForValue().set("category:tree", json, 3600, TimeUnit.SECONDS);
    }
}
