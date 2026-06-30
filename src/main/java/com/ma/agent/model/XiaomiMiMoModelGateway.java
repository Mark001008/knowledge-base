package com.ma.agent.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ma.agent.model.ModelChatRequest;
import com.ma.agent.model.ModelChatResponse;
import com.ma.agent.model.ModelGateway;
import com.ma.agent.model.ModelProviderException;
import com.ma.agent.model.ModelStreamChunk;
import com.ma.agent.shared.LogMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(prefix = "agent.model", name = "provider", havingValue = "xiaomi-mimo")
class XiaomiMiMoModelGateway implements ModelGateway {

    private static final Logger log = LoggerFactory.getLogger(XiaomiMiMoModelGateway.class);
    private static final String PROVIDER = "xiaomi-mimo";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final ModelProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final HttpClient httpClient;

    XiaomiMiMoModelGateway(ModelProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        // 配置代理（优先从环境变量读取）
        ProxyConfig proxyConfig = resolveProxyConfig();

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(trimTrailingSlash(properties.getBaseUrl()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout());

        // 如果配置了代理，添加代理支持
        if (proxyConfig != null) {
            log.info(LogMarkers.BIZ, "Using proxy: {}:{}", proxyConfig.host, proxyConfig.port);
            ProxySelector proxySelector = ProxySelector.of(
                    new InetSocketAddress(proxyConfig.host, proxyConfig.port)
            );
            httpClientBuilder.proxy(proxySelector);

            // RestClient 使用 SimpleClientHttpRequestFactory 配置代理
            org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
            requestFactory.setProxy(new java.net.Proxy(
                    java.net.Proxy.Type.HTTP,
                    new InetSocketAddress(proxyConfig.host, proxyConfig.port)
            ));
            restClientBuilder.requestFactory(requestFactory);
        }

        this.restClient = restClientBuilder.build();
        this.httpClient = httpClientBuilder.build();
    }

    /**
     * 从环境变量解析代理配置。
     * 支持 HTTPS_PROXY、HTTP_PROXY、ALL_PROXY 格式：http://host:port 或 socks5://host:port
     */
    private ProxyConfig resolveProxyConfig() {
        // 优先读取 HTTPS_PROXY
        String proxyUrl = System.getenv("HTTPS_PROXY");
        if (proxyUrl == null || proxyUrl.isBlank()) {
            proxyUrl = System.getenv("HTTP_PROXY");
        }
        if (proxyUrl == null || proxyUrl.isBlank()) {
            proxyUrl = System.getenv("ALL_PROXY");
        }
        if (proxyUrl == null || proxyUrl.isBlank()) {
            return null;
        }

        try {
            // 解析 proxy URL，格式：http://host:port 或 socks5://host:port
            String withoutProtocol = proxyUrl.replaceAll("^https?://", "").replaceAll("^socks5?://", "");
            String[] parts = withoutProtocol.split(":");
            if (parts.length == 2) {
                return new ProxyConfig(parts[0], Integer.parseInt(parts[1]));
            }
        } catch (Exception e) {
            log.warn("Failed to parse proxy URL: {}", proxyUrl, e);
        }
        return null;
    }

    private record ProxyConfig(String host, int port) {
    }

    @Override
    public ModelChatResponse chat(ModelChatRequest request) {
        requireApiKey();
        String model = properties.getModel();
        long start = System.currentTimeMillis();
        log.info(LogMarkers.DATA, "[{}] model={} conv={} action=chat status=start", PROVIDER, model, request.conversationId());

        try {
            Map<String, Object> body = requestBody(request, false);
            JsonNode response = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                long elapsed = System.currentTimeMillis() - start;
                log.error(LogMarkers.DATA, "[{}] model={} conv={} action=chat status=error http_status=null elapsed={}ms", PROVIDER, model, request.conversationId(), elapsed);
                throw new ModelProviderException("Xiaomi MiMo returned an empty response.");
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info(LogMarkers.DATA, "[{}] model={} conv={} action=chat status=ok http_status=200 elapsed={}ms", PROVIDER, model, request.conversationId(), elapsed);
            return new ModelChatResponse(extractMessage(response), model);
        } catch (RestClientResponseException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error(LogMarkers.DATA, "[{}] model={} conv={} action=chat status=error http_status={} elapsed={}ms", PROVIDER, model, request.conversationId(), e.getStatusCode().value(), elapsed);
            throw new ModelProviderException("Xiaomi MiMo returned HTTP " + e.getStatusCode().value(), e);
        } catch (ModelProviderException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error(LogMarkers.DATA, "[{}] model={} conv={} action=chat status=error elapsed={}ms", PROVIDER, model, request.conversationId(), elapsed, e);
            throw e;
        }
    }

    @Override
    public void stream(ModelChatRequest request, Consumer<ModelStreamChunk> chunkConsumer) {
        requireApiKey();
        String model = properties.getModel();
        long start = System.currentTimeMillis();
        log.info(LogMarkers.DATA, "[{}] model={} conv={} action=stream status=start", PROVIDER, model, request.conversationId());

        HttpRequest httpRequest = HttpRequest.newBuilder(streamUri())
                .timeout(properties.getTimeout())
                .header(HttpHeaders.AUTHORIZATION, bearerToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody(request, true))))
                .build();

        try {
            HttpResponse<Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            int httpStatus = response.statusCode();
            if (httpStatus < 200 || httpStatus >= 300) {
                long elapsed = System.currentTimeMillis() - start;
                log.error(LogMarkers.DATA, "[{}] model={} conv={} action=stream status=error http_status={} elapsed={}ms", PROVIDER, model, request.conversationId(), httpStatus, elapsed);
                throw new ModelProviderException("Xiaomi MiMo stream request failed with HTTP " + httpStatus);
            }

            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> handleStreamLine(line, chunkConsumer));
            }
            long elapsed = System.currentTimeMillis() - start;
            log.info(LogMarkers.DATA, "[{}] model={} conv={} action=stream status=ok http_status={} elapsed={}ms", PROVIDER, model, request.conversationId(), httpStatus, elapsed);
        } catch (ModelProviderException e) {
            throw e;
        } catch (IOException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error(LogMarkers.DATA, "[{}] model={} conv={} action=stream status=error elapsed={}ms", PROVIDER, model, request.conversationId(), elapsed, ex);
            throw new ModelProviderException("Xiaomi MiMo stream request failed.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            long elapsed = System.currentTimeMillis() - start;
            log.error(LogMarkers.DATA, "[{}] model={} conv={} action=stream status=error elapsed={}ms", PROVIDER, model, request.conversationId(), elapsed, ex);
            throw new ModelProviderException("Xiaomi MiMo stream request was interrupted.", ex);
        }
    }

    private Map<String, Object> requestBody(ModelChatRequest request, boolean stream) {
        return Map.of(
                "model", properties.getModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", request.message()
                )),
                "stream", stream
        );
    }

    private String extractMessage(JsonNode response) {
        JsonNode content = response.at("/choices/0/message/content");
        if (!content.isTextual()) {
            throw new ModelProviderException("Xiaomi MiMo response does not contain choices[0].message.content.");
        }
        return content.asText();
    }

    private void handleStreamLine(String line, Consumer<ModelStreamChunk> chunkConsumer) {
        if (line == null || line.isBlank() || !line.startsWith("data:")) {
            return;
        }

        String data = line.substring("data:".length()).trim();
        if ("[DONE]".equals(data)) {
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(data);
            JsonNode content = node.at("/choices/0/delta/content");
            if (content.isTextual() && !content.asText().isEmpty()) {
                chunkConsumer.accept(new ModelStreamChunk(content.asText()));
            }
        } catch (JsonProcessingException ex) {
            throw new ModelProviderException("Failed to parse Xiaomi MiMo stream chunk.", ex);
        }
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException ex) {
            throw new ModelProviderException("Failed to serialize Xiaomi MiMo request.", ex);
        }
    }

    private URI streamUri() {
        return URI.create(trimTrailingSlash(properties.getBaseUrl()) + CHAT_COMPLETIONS_PATH);
    }

    private void requireApiKey() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ModelProviderException("Xiaomi MiMo API key is missing. Set XIAOMI_MIMO_API_KEY.");
        }
    }

    private String bearerToken() {
        return "Bearer " + properties.getApiKey();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
