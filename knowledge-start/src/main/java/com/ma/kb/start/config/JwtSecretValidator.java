package com.ma.kb.start.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * JWT Secret 启动校验器
 * 确保应用不会使用不安全的默认密钥启动
 */
@Component
@Order(1)
public class JwtSecretValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretValidator.class);
    private static final int MIN_SECRET_BYTES = 32;
    private static final String DEFAULT_PLACEHOLDER = "CHANGE_ME";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${agent.jwt.secret:}")
    private String agentJwtSecret;

    @Override
    public void run(ApplicationArguments args) {
        validateSecret(jwtSecret, "jwt.secret");
        if (agentJwtSecret != null && !agentJwtSecret.isEmpty()) {
            validateSecret(agentJwtSecret, "agent.jwt.secret");
        }
        log.info("JWT secret 校验通过");
    }

    private void validateSecret(String secret, String propertyName) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                propertyName + " 未配置。请通过环境变量 " + toEnvVarName(propertyName) + " 设置安全密钥（至少32字节）");
        }

        if (DEFAULT_PLACEHOLDER.equals(secret)) {
            throw new IllegalStateException(
                propertyName + " 使用了默认占位符 '" + DEFAULT_PLACEHOLDER + "'。"
                + "请通过环境变量 " + toEnvVarName(propertyName) + " 设置安全密钥（至少32字节）");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                    propertyName + " 解码后长度为 " + decoded.length + " 字节，"
                    + "要求至少 " + MIN_SECRET_BYTES + " 字节。请设置更强的密钥");
            }
        } catch (IllegalArgumentException e) {
            // Not valid Base64, check raw length
            if (secret.length() < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                    propertyName + " 长度不足，要求至少 " + MIN_SECRET_BYTES + " 字节。"
                    + "请通过环境变量 " + toEnvVarName(propertyName) + " 设置安全密钥");
            }
        }
    }

    private String toEnvVarName(String propertyName) {
        return propertyName.replace('.', '_').toUpperCase();
    }
}
