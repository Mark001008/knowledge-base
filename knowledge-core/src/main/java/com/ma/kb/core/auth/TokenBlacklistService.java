package com.ma.kb.core.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Token 黑名单服务
 * 用于实现登出功能，将已登出的 Token 加入黑名单
 * 使用内存存储，应用重启后黑名单清空（可接受，因为 Token 有效期仅 2 小时）
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    // key: token, value: 过期时间（epoch second）
    private final ConcurrentMap<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * 将 Token 加入黑名单
     */
    public void blacklist(String token, long expiresAtEpochSecond) {
        blacklist.put(token, expiresAtEpochSecond);
        log.debug("Token 已加入黑名单，过期时间: {}", Instant.ofEpochSecond(expiresAtEpochSecond));
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        Long expiresAt = blacklist.get(token);
        if (expiresAt == null) {
            return false;
        }
        // 如果已过期，移除并返回 false
        if (Instant.now().getEpochSecond() > expiresAt) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    /**
     * 定时清理过期的黑名单条目（每 10 分钟执行一次）
     */
    @Scheduled(fixedRate = 600000)
    public void cleanup() {
        long now = Instant.now().getEpochSecond();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < now);
        int after = blacklist.size();
        if (before != after) {
            log.info("清理过期黑名单条目: {} -> {}", before, after);
        }
    }
}
