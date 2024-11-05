package jadx.plugins.jadaix.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class OpenAIClient implements LLMClient {
    private static final Logger LOG = LoggerFactory.getLogger(OpenAIClient.class);
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json");
    
    private final String apiKey;
    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public OpenAIClient(String apiKey) {
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
            throw new LLMException("OpenAI client not properly configured");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-4");
            body.put("messages", Collections.singletonList(
                    Map.of("role", "user", "content", message)
            ));
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.7);

            RequestBody requestBody = RequestBody.create(
                    mapper.writeValueAsString(body), JSON
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    throw new LLMException("API request failed: " + response.code());
                }

                Map<String, Object> jsonResponse = mapper.readValue(
                        response.body().string(), Map.class
                );

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) jsonResponse.get("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new LLMException("No response from API");
                }

                @SuppressWarnings("unchecked")
                Map<String, String> message_obj = (Map<String, String>) choices.get(0).get("message");
                return message_obj.get("content");
            }

        } catch (IOException e) {
            throw new LLMException("Failed to communicate with OpenAI API", e);
        }
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public int estimateTokens(String message)
        return message.length() / 4;
    }
}