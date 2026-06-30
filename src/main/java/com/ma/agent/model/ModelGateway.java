package com.ma.agent.model;

import java.util.function.Consumer;

public interface ModelGateway {

    ModelChatResponse chat(ModelChatRequest request);

    void stream(ModelChatRequest request, Consumer<ModelStreamChunk> chunkConsumer);
}
