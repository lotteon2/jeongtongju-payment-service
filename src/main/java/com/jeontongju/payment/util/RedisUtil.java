package com.jeontongju.payment.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeontongju.payment.exception.RedisConnectionException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisUtil {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveRedis(String key, Object data){
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), Duration.ofMinutes(5));
        } catch (JsonProcessingException e) {
            throw new RedisConnectionException("레디스에 들어갈 데이터를 만드는 과정에서 문제가 발생했습니다.");
        }
    }

    public <T> T commonApproveLogin(String partnerOrderId, Class<T> valueType) {
        try {
            String jsonValue = redisTemplate.opsForValue().get(partnerOrderId);
            return objectMapper.readValue(jsonValue, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("파싱 에러", e);
        }
    }

}
