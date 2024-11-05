package jadx.plugins.jadaix.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AnthropicClient implements LLMClient {
    private static final Logger LOG = LoggerFactory.getLogger(AnthropicClient.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final MediaType JSON = MediaType.parse("application/json");
    
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public AnthropicClient(String apiKey) {
        this.apiKey = apiKey;
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
            throw new LLMException("Anthropic client not properly configured");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", "claude-3-opus-20240229");
            body.put("max_tokens", maxTokens);
            body.put("messages", new Object[]{
                Map.of("role", "user", "content", message)
            });

            RequestBody requestBody = RequestBody.create(
                    mapper.writeValueAsString(body), JSON
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new LLMException("API request failed: " + response.code());
                }

                Map<String, Object> jsonResponse = mapper.readValue(
                        response.body().string(), Map.class
                );

                if (!jsonResponse.containsKey("content")) {
                    throw new LLMException("Invalid response format from Anthropic API");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> content = (Map<String, Object>) jsonResponse.get("content");
                return content.get("text").toString();
            }

        } catch (IOException e) {
            throw new LLMException("Failed to communicate with Anthropic API", e);
        }
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public int estimateTokens(String message) {
        // Claude uses a similar tokenization to GPT, roughly 4 chars per token
        return message.length() / 4;
    }
}