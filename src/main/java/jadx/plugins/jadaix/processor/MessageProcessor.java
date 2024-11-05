package jadx.plugins.jadaix.processor;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.plugins.jadaix.JadaixOptions;
import jadx.plugins.jadaix.JadaixAIOptions;
import jadx.plugins.jadaix.context.ContextBuilder;
import jadx.plugins.jadaix.llm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class MessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);
    private static final int MAX_CONTEXT_TOKENS = 8000;
    
    private final JadaixOptions options;
    private final JadaixAIOptions aiOptions;
    private final ContextBuilder contextBuilder;
    private final LLMClient llmClient;
    private String currentContext;
    private final StringBuilder conversationHistory;

    public MessageProcessor(JadaixOptions options, JadaixAIOptions aiOptions, ContextBuilder contextBuilder) {
        this.options = options;
        this.aiOptions = aiOptions;
        this.contextBuilder = contextBuilder;
        this.llmClient = createLLMClient();
        this.conversationHistory = new StringBuilder();
    }

    private LLMClient createLLMClient() {
        switch (aiOptions.getSelectedModel()) {
            case CHATGPT:
                return new OpenAIClient(aiOptions.getApiKey());
            case CLAUDE:
                return new AnthropicClient(aiOptions.getApiKey());
            case MISTRAL:
                return new MistralClient(aiOptions.getApiKey());
            case MISTRAL_SELF_HOSTED:
                return new MistralClient(aiOptions.getApiKey(), aiOptions.getMistralFullEndpoint());
            default:
                throw new IllegalStateException("Unsupported LLM model: " + aiOptions.getSelectedModel());
        }
    }

    public void processClass(JavaClass javaClass, boolean isHierarchyMode, Consumer<String> messageHandler) {
        try {
            String context;
            if (isHierarchyMode) {
                context = contextBuilder.buildHierarchyContext(javaClass);
            } else {
                context = contextBuilder.buildSimpleContext(javaClass);
            }
            
            currentContext = optimizeContext(context);
            if (estimateTokens(currentContext) > MAX_CONTEXT_TOKENS) {
                currentContext = truncateContext(currentContext, MAX_CONTEXT_TOKENS);
            }
            
            // Build new message
            String userMessage = "For this NEW analysis:\n" + aiOptions.getPromptTemplate() + "\n\n" + currentContext;
            
            // Add new message to history
            conversationHistory.append("Human: ").append(userMessage).append("\n\n");
            
            // Send full conversation
            String response = llmClient.sendMessage(conversationHistory.toString(), MAX_CONTEXT_TOKENS);
            
            // Add response to history
            conversationHistory.append("Assistant: ").append(response).append("\n\n");
            
            messageHandler.accept(response);

        } catch (LLMException e) {
            LOG.error("Error processing class", e);
            messageHandler.accept("Error: " + e.getMessage());
        }
    }

    public void processMethod(JavaMethod javaMethod, String methodCode, boolean isHierarchyMode, Consumer<String> messageHandler) {
        try {
            String context;
            if (isHierarchyMode) {
                context = contextBuilder.buildHierarchyContext(javaMethod.getDeclaringClass()) + 
                         "\n\nAnalyzed Method:\n" + methodCode;
            } else {
                context = "Method code:\n" + methodCode;
            }
            
            currentContext = optimizeContext(context);
            if (estimateTokens(currentContext) > MAX_CONTEXT_TOKENS) {
                currentContext = truncateContext(currentContext, MAX_CONTEXT_TOKENS);
            }
            
            // Build new message
            String userMessage = "For this NEW analysis:\n" + aiOptions.getPromptTemplate() + "\n\n" + currentContext;
            
            // Add new message to history
            conversationHistory.append("Human: ").append(userMessage).append("\n\n");
            
            // Send full conversation
            String response = llmClient.sendMessage(conversationHistory.toString(), MAX_CONTEXT_TOKENS);
            
            // Add response to history
            conversationHistory.append("Assistant: ").append(response).append("\n\n");
            
            messageHandler.accept(response);

        } catch (LLMException e) {
            LOG.error("Error processing method", e);
            messageHandler.accept("Error: " + e.getMessage());
        }
    }

    public void sendMessage(String userMessage, Consumer<String> messageHandler) {
        try {
            // Add new message with context
            String fullMessage = userMessage + "\n\nPrevious Context:\n" + currentContext;
            conversationHistory.append("Human: ").append(fullMessage).append("\n\n");
            
            // Send full conversation
            String response = llmClient.sendMessage(conversationHistory.toString(), MAX_CONTEXT_TOKENS);
            
            // Add response to history
            conversationHistory.append("Assistant: ").append(response).append("\n\n");
            
            messageHandler.accept(response);
        } catch (LLMException e) {
            LOG.error("Error processing message", e);
            messageHandler.accept("Error: " + e.getMessage());
        }
    }

    private String optimizeContext(String context) {
        if (context == null || context.isEmpty()) {
            return "";
        }
        
        // Remove unnecessary whitespace while preserving code structure
        context = context.replaceAll("(?m)^[ \\t]*\\r?\\n", "\n"); // Remove empty lines
        context = context.replaceAll("[ \\t]+", " "); // Collapse multiple spaces
        context = context.replaceAll("(?m)^[ \\t]+", ""); // Remove leading whitespace
        context = Pattern.compile("\\{\\s+").matcher(context).replaceAll("{ "); // Optimize braces
        context = Pattern.compile("\\s+}").matcher(context).replaceAll(" }");
        context = Pattern.compile(";\\s+").matcher(context).replaceAll("; "); // Optimize semicolons
        context = context.replaceAll("\n{3,}", "\n\n"); // Remove multiple newlines
        
        return context.trim();
    }

    private int estimateTokens(String text) {
        return text == null ? 0 : text.length() / 4;
    }

    private String truncateContext(String context, int maxTokens) {
        if (context == null || estimateTokens(context) <= maxTokens) {
            return context;
        }

        String[] lines = context.split("\n");
        StringBuilder sb = new StringBuilder();
        int tokens = 0;
        boolean inClass = false;

        for (String line : lines) {
            int lineTokens = estimateTokens(line);
            if (tokens + lineTokens > maxTokens) {
                if (!inClass) {
                    break;
                }
                if (line.contains("}") && !line.contains("{")) {
                    sb.append(line).append("\n");
                    break;
                }
            }
            
            sb.append(line).append("\n");
            tokens += lineTokens;
            
            if (line.contains("class") && line.contains("{")) {
                inClass = true;
            } else if (line.contains("}") && !line.contains("{")) {
                inClass = false;
            }
        }

        return sb.toString().trim();
    }
}