package com.ma.agent.knowledge.document;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.oss")
public record OssProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {
}
