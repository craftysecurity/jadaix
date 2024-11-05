package jadx.plugins.jadaix.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MistralClient implements LLMClient {
    private static final Logger LOG = LoggerFactory.getLogger(MistralClient.class);
    private static final String HOSTED_API_URL = "https://api.mistral.ai/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json");
    
    private final String apiKey;
    private final String endpoint;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public MistralClient(String apiKey) {
        this(apiKey, null);
    }

    public MistralClient(String apiKey, String customEndpoint) {
        this.apiKey = apiKey;
        this.endpoint = customEndpoint != null ? customEndpoint : HOSTED_API_URL;
        this.mapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String sendMessage(String message, int maxTokens) throws LLMException {
        if (!isConfigured()) {
            throw new LLMException("Mistral client not properly configured");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", "mistral-large-latest");
            body.put("messages", Collections.singletonList(
                    Map.of("role", "user", "content", message)
            ));
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.7);

            RequestBody requestBody = RequestBody.create(
                    mapper.writeValueAsString(body), JSON
            );

            Request.Builder requestBuilder = new Request.Builder()
                    .url(endpoint)
                    .post(requestBody);

            // Add API key header if using hosted API
            if (endpoint.equals(HOSTED_API_URL)) {
                requestBuilder.addHeader("Authorization", "Bearer " + apiKey);
            }

            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new LLMException("API request failed: " + response.code());
                }

                Map<String, Object> jsonResponse = mapper.readValue(
                        response.body().string(), Map.class
                );

                if (!jsonResponse.containsKey("choices")) {
                    throw new LLMException("Invalid response format from Mistral API");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> choice = (Map<String, Object>) 
                        ((java.util.List<?>) jsonResponse.get("choices")).get(0);
                
                @SuppressWarnings("unchecked")
                Map<String, String> message_obj = (Map<String, String>) choice.get("message");
                return message_obj.get("content");
            }

        } catch (IOException e) {
            throw new LLMException("Failed to communicate with Mistral API", e);
        }
    }

    @Override
    public boolean isConfigured() {
        return (endpoint.equals(HOSTED_API_URL) && apiKey != null && !apiKey.trim().isEmpty()) ||
               (!endpoint.equals(HOSTED_API_URL) && endpoint != null && !endpoint.trim().isEmpty());
    }

    @Override
    public int estimateTokens(String message) {
        return message.length() / 4;
    }
}