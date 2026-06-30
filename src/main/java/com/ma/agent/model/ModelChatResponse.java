package com.ma.agent.model;

/**
 * 模型网关层对话响应 DTO，各 ModelGateway 实现统一的返回值。
 *
 * @param message 模型回复消息文本
 * @param model   实际使用的模型名称
 */
public record ModelChatResponse(
        String message,
        String model
) {
}
