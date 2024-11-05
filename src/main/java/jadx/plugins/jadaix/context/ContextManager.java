package jadx.plugins.jadaix.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ContextManager {
    private static final Logger LOG = LoggerFactory.getLogger(ContextManager.class);
    private static final int AVERAGE_TOKEN_LENGTH = 4; // Approximate characters per token

    private final int maxTokens;
    private final List<String> contextChunks;
    private int currentTokenCount;

    public ContextManager(int maxTokens) {
        this.maxTokens = maxTokens;
        this.contextChunks = new ArrayList<>();
        this.currentTokenCount = 0;
    }

    public void addContext(String context) {
        int estimatedTokens = estimateTokens(context);
        
        if (estimatedTokens > maxTokens) {
            // Split context if it's too large
            List<String> chunks = splitContext(context);
            for (String chunk : chunks) {
                addContextChunk(chunk);
            }
        } else {
            addContextChunk(context);
        }
    }

    private void addContextChunk(String chunk) {
        int tokens = estimateTokens(chunk);
        if (currentTokenCount + tokens > maxTokens) {
            LOG.debug("Context chunk would exceed token limit, creating new chunk");
            contextChunks.add(chunk);
            currentTokenCount = tokens;
        } else {
            if (!contextChunks.isEmpty()) {
                contextChunks.set(contextChunks.size() - 1, 
                    contextChunks.get(contextChunks.size() - 1) + "\n" + chunk);
            } else {
                contextChunks.add(chunk);
            }
            currentTokenCount += tokens;
        }
    }

    private List<String> splitContext(String context) {
        List<String> chunks = new ArrayList<>();
        String[] lines = context.split("\n");
        StringBuilder currentChunk = new StringBuilder();
        int currentChunkTokens = 0;

        for (String line : lines) {
            int lineTokens = estimateTokens(line);
            
            if (currentChunkTokens + lineTokens > maxTokens) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                    currentChunkTokens = 0;
                }
            }
            
            currentChunk.append(line).append("\n");
            currentChunkTokens += lineTokens;
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    public List<String> getContextChunks() {
        return new ArrayList<>(contextChunks);
    }

    private int estimateTokens(String text) {
        return text.length() / AVERAGE_TOKEN_LENGTH;
    }

    public int getCurrentTokenCount() {
        return currentTokenCount;
    }

    public void clear() {
        contextChunks.clear();
        currentTokenCount = 0;
    }
}