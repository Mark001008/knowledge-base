package com.ma.agent.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务 - 用于会话缓存和热点数据缓存
 */
@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 设置缓存
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Redis SET key={}", key);
        } catch (Exception e) {
            log.error("Redis SET failed key={}", key, e);
        }
    }

    /**
     * 设置对象缓存（JSON 序列化）
     */
    public <T> void setObject(String key, T value, long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, timeout, unit);
            log.debug("Redis SET_OBJECT key={}", key);
        } catch (JsonProcessingException e) {
            log.error("Redis SET_OBJECT serialization failed key={}", key, e);
        } catch (Exception e) {
            log.error("Redis SET_OBJECT failed key={}", key, e);
        }
    }

    /**
     * 获取缓存
     */
    public String get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            log.debug("Redis GET key={} found={}", key, value != null);
            return value;
        } catch (Exception e) {
            log.error("Redis GET failed key={}", key, e);
            return null;
        }
    }

    /**
     * 获取对象缓存（JSON 反序列化）
     */
    public <T> T getObject(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Redis GET_OBJECT failed key={}", key, e);
            return null;
        }
    }

    /**
     * 获取列表缓存
     */
    public <T> List<T> getList(String key, Class<T> elementClass) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (Exception e) {
            log.error("Redis GET_LIST failed key={}", key, e);
            return null;
        }
    }

    /**
     * 设置列表缓存
     */
    public <T> void setList(String key, List<T> value, long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, timeout, unit);
            log.debug("Redis SET_LIST key={}", key);
        } catch (JsonProcessingException e) {
            log.error("Redis SET_LIST serialization failed key={}", key, e);
        } catch (Exception e) {
            log.error("Redis SET_LIST failed key={}", key, e);
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis DELETE key={}", key);
        } catch (Exception e) {
            log.error("Redis DELETE failed key={}", key, e);
        }
    }

    /**
     * 检查 key 是否存在
     */
    public boolean hasKey(String key) {
        try {
	        return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis HAS_KEY failed key={}", key, e);
            return false;
        }
    }

    /**
     * 设置过期时间
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis EXPIRE failed key={}", key, e);
        }
    }

    /**
     * 递增
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis INCREMENT failed key={}", key, e);
            return null;
        }
    }

    /**
     * 递增指定值
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(key, delta);
        } catch (Exception e) {
            log.error("Redis INCREMENT failed key={}", key, e);
            return null;
        }
    }
}
