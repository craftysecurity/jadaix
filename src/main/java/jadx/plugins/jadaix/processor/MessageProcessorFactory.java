package jadx.plugins.jadaix.processor;

import jadx.api.JadxDecompiler;
import jadx.plugins.jadaix.JadaixOptions;
import jadx.plugins.jadaix.JadaixAIOptions;
import jadx.plugins.jadaix.context.ContextBuilder;

public class MessageProcessorFactory {
    private static MessageProcessor instance;

    public static MessageProcessor getProcessor(JadxDecompiler decompiler, JadaixOptions options, JadaixAIOptions aiOptions) {
        if (instance == null) {
            ContextBuilder contextBuilder = new ContextBuilder(decompiler, options, aiOptions);
            instance = new MessageProcessor(options, aiOptions, contextBuilder);
        }
        return instance;
    }

    public static void refreshProcessor(JadxDecompiler decompiler, JadaixOptions options, JadaixAIOptions aiOptions) {
        ContextBuilder contextBuilder = new ContextBuilder(decompiler, options, aiOptions);
        instance = new MessageProcessor(options, aiOptions, contextBuilder);
    }
}