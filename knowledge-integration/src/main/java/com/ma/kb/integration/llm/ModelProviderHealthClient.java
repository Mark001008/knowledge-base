package com.ma.kb.integration.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 模型配置健康检查
 */
@Component
public class ModelProviderHealthClient {

    private final String provider;
    private final String baseUrl;
    private final String modelName;

    public ModelProviderHealthClient(
            @Value("${agent.model.provider:mock}") String provider,
            @Value("${agent.model.base-url:}") String baseUrl,
            @Value("${agent.model.chat-model:MiLM}") String modelName) {
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
    }

    public String currentProvider() {
        return provider;
    }

    public String currentModel() {
        return modelName;
    }

    public boolean isConfigured() {
        return !"mock".equals(provider) && !baseUrl.isBlank();
    }
}
