package jadx.plugins.jadaix;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class JadaixAIOptions extends BasePluginOptionsBuilder {
    private static final String PREFIX = JadaixPlugin.PLUGIN_ID + ".ai.";

    public enum LLMModel {
        CHATGPT("ChatGPT"),
        CLAUDE("Claude"),
        MISTRAL("Mistral"),
        MISTRAL_SELF_HOSTED("Mistral Self-Hosted");

        private final String displayName;

        LLMModel(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum PromptType {
        SECURITY("Security Analysis"),
        MALWARE("Malware Analysis"),
        CUSTOM("Custom Prompt");

        private final String displayName;

        PromptType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Model Selection
    private LLMModel selectedModel;
    private String mistralEndpoint;
    private boolean useMistralSSL;
    private String apiKey;
    
    // Analysis Options
    private PromptType promptType;
    private String customPrompt;
    private boolean hierarchyAnalysisEnabled;
    
    // Context Configuration
    private int maxTokens;
    private String packageWhitelist;
    private String packageBlacklist;

    @Override
    public void registerOptions() {
        // Model Selection
        enumOption(PREFIX + "model", LLMModel.values(), s -> LLMModel.valueOf(s))
                .description("Select AI Model")
                .defaultValue(LLMModel.CHATGPT)
                .setter(v -> selectedModel = v);

        strOption(PREFIX + "mistral.endpoint")
                .description("Mistral Self-Hosted Endpoint (e.g., localhost:8080 or example.com)")
                .defaultValue("localhost:8080")
                .setter(v -> mistralEndpoint = v);

        boolOption(PREFIX + "mistral.ssl")
                .description("Use HTTPS for Mistral Self-Hosted endpoint")
                .defaultValue(false)
                .setter(v -> useMistralSSL = v);

        strOption(PREFIX + "key")
                .description("API Key for selected model")
                .defaultValue("")
                .setter(v -> apiKey = v);

        // Analysis Options
        enumOption(PREFIX + "prompt.type", PromptType.values(), s -> PromptType.valueOf(s))
                .description("Select analysis type")
                .defaultValue(PromptType.SECURITY)
                .setter(v -> promptType = v);

        strOption(PREFIX + "prompt.custom")
                .description("Custom prompt template")
                .defaultValue("")
                .setter(v -> customPrompt = v);

        boolOption(PREFIX + "hierarchyAnalysis.enable")
                .description("Allow hierarchy analysis")
                .defaultValue(false)
                .setter(v -> hierarchyAnalysisEnabled = v);

        // Context Configuration
        intOption(PREFIX + "context.maxTokens")
                .description("Maximum tokens for context window")
                .defaultValue(8000)
                .setter(v -> maxTokens = v);

        strOption(PREFIX + "context.whitelist")
                .description("Package whitelist patterns (comma-separated)")
                .defaultValue("")
                .setter(v -> packageWhitelist = v);

        strOption(PREFIX + "context.blacklist")
                .description("Package blacklist patterns (comma-separated)")
                .defaultValue("android.*, androidx.*, com.google.*, java.*, javax.*, kotlin.*, dalvik.*, org.json.*, org.xml.*, org.w3c.*, org.apache.*, sun.*")
                .setter(v -> packageBlacklist = v);
    }

    // Existing getters
    public LLMModel getSelectedModel() {
        return selectedModel;
    }

    public String getMistralEndpoint() {
        return mistralEndpoint;
    }

    public boolean isUseMistralSSL() {
        return useMistralSSL;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getMistralFullEndpoint() {
        String protocol = useMistralSSL ? "https://" : "http://";
        return protocol + mistralEndpoint;
    }

    public PromptType getPromptType() {
        return promptType;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public boolean isHierarchyAnalysisEnabled() {
        return hierarchyAnalysisEnabled;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public String getPackageWhitelist() {
        return packageWhitelist;
    }

    public String getPackageBlacklist() {
        return packageBlacklist;
    }

    public String getPromptTemplate() {
        switch (promptType) {
            case SECURITY:
        return "You are an expert Android application security engineer analyzing code for security issues.\n\n"
                + "If you find potential vulnerabilities:\n"
                + "1. Show the exact vulnerable code in a code block\n"
                + "2. Provide a detailed technical analysis explaining why it's vulnerable\n"
                + "3. Recommend specific fixes\n\n"
                + "If no clear vulnerabilities are found in this code, identify which other classes or packages "
                + "would be valuable to analyze next and explain why.\n\n"
                + "Focus ONLY on concrete findings. Do not list theoretical vulnerabilities or repeat the analysis parameters.\n\n"
                + "List which classes you received for analysis.\n\n"
                + "Context:\n";
            case MALWARE:
                return "Analyze the provided code for potential malicious behavior:\n"
                        + "- Data exfiltration patterns\n"
                        + "- Excessive permission usage\n"
                        + "- Overlay attack techniques\n"
                        + "- Obfuscation methods\n"
                        + "- Command & control patterns\n"
                        + "- Suspicious API usage\n\n";
                        + "List which classes you received for analysis.\n\n"
            case CUSTOM:
                return customPrompt != null && !customPrompt.isEmpty() 
                    ? customPrompt + "\n\n" 
                    : "Analyze the provided code:\n\n";
            default:
                return "Analyze the provided code:\n\n";
        }
    }
}