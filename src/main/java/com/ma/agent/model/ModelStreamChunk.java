package com.ma.agent.model;

/**
 * 模型网关层流式响应块 DTO。
 *
 * @param delta 增量文本内容
 */
public record ModelStreamChunk(
        String delta
) {
}
