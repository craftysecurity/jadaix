package jadx.plugins.jadaix.llm;

public interface LLMClient {
    /**
     * Send a message to the LLM and get a response
     * @param message The message to send
     * @param maxTokens Maximum tokens to use for response
     * @return The LLM's response
     */
    String sendMessage(String message, int maxTokens) throws LLMException;

    /**
     * Check if the client is properly configured
     * @return true if the client is ready to use
     */
    boolean isConfigured();

    /**
     * Get estimated token count for a message
     * @param message The message to count tokens for
     * @return Estimated number of tokens
     */
    int estimateTokens(String message);
}