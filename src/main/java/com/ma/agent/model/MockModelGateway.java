package com.ma.agent.model;

import com.ma.agent.model.ModelChatRequest;
import com.ma.agent.model.ModelChatResponse;
import com.ma.agent.model.ModelGateway;
import com.ma.agent.model.ModelStreamChunk;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@ConditionalOnProperty(prefix = "agent.model", name = "provider", havingValue = "mock", matchIfMissing = true)
class MockModelGateway implements ModelGateway {

    private static final Logger log = LoggerFactory.getLogger(MockModelGateway.class);
    private static final String PROVIDER = "mock";
    private static final String MOCK_MODEL = "mock-agent";

    @Override
    public ModelChatResponse chat(ModelChatRequest request) {
        long start = System.currentTimeMillis();
        log.info(LogMarkers.DATA, "[{}] model={} conv={} action=chat status=start", PROVIDER, MOCK_MODEL, request.conversationId());
        try {
            var response = new ModelChatResponse(
                    "Agent platform scaffold is ready. Next step: connect a real model gateway.",
                    MOCK_MODEL
            );
            long elapsed = System.currentTimeMillis() - start;
            log.info(LogMarkers.DATA, "[{}] model={} conv={} action=chat status=ok elapsed={}ms", PROVIDER, MOCK_MODEL, request.conversationId(), elapsed);
            return response;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error(LogMarkers.DATA, "[{}] model={} conv={} action=chat status=error elapsed={}ms", PROVIDER, MOCK_MODEL, request.conversationId(), elapsed, e);
            throw e;
        }
    }

    @Override
    public void stream(ModelChatRequest request, Consumer<ModelStreamChunk> chunkConsumer) {
        long start = System.currentTimeMillis();
        log.info(LogMarkers.DATA, "[{}] model={} conv={} action=stream status=start", PROVIDER, MOCK_MODEL, request.conversationId());
        String[] tokens = {
                "Agent ",
                "platform ",
                "scaffold ",
                "is ",
                "ready."
        };

        try {
            for (String token : tokens) {
                chunkConsumer.accept(new ModelStreamChunk(token));
                sleep();
            }
            long elapsed = System.currentTimeMillis() - start;
            log.info(LogMarkers.DATA, "[{}] model={} conv={} action=stream status=ok elapsed={}ms", PROVIDER, MOCK_MODEL, request.conversationId(), elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error(LogMarkers.DATA, "[{}] model={} conv={} action=stream status=error elapsed={}ms", PROVIDER, MOCK_MODEL, request.conversationId(), elapsed, e);
            throw e;
        }
    }

    private void sleep() {
        try {
            Thread.sleep(120L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
