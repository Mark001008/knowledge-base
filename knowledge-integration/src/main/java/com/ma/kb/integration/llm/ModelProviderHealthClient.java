package com.ma.kb.integration.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModelProviderHealthClient {

    private final String provider;

    public ModelProviderHealthClient(@Value("${agent.model.provider:mock}") String provider) {
        this.provider = provider;
    }

    public String currentProvider() {
        return provider;
    }
}
