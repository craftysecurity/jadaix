package jadx.plugins.jadaix;

import jadx.api.JadxDecompiler;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.JadxGuiContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadaixPlugin implements JadxPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(JadaixPlugin.class);
    public static final String PLUGIN_ID = "jadaix";
    
    private final JadaixOptions options = new JadaixOptions();
    private final JadaixAIOptions aiOptions = new JadaixAIOptions();

    @Override
    public JadxPluginInfo getPluginInfo() {
        return new JadxPluginInfo(
                PLUGIN_ID,
                "Jadaix Analysis",
                "Interactive code analysis using LLMs"
        );
    }

    private Boolean canProcess(ICodeNodeRef nodeRef) {
        return options.isEnabled();
    }

    private Boolean canProcessHierarchy(ICodeNodeRef nodeRef) {
        return options.isEnabled() && aiOptions.isHierarchyAnalysisEnabled();
    }

    @Override
    public void init(JadxPluginContext context) {
        context.registerOptions(options);
        context.registerOptions(aiOptions);

        if (!options.isEnabled()) {
            LOG.info("Jadaix plugin is disabled");
            return;
        }

        JadxDecompiler decompiler = context.getDecompiler();
        JadxGuiContext guiContext = context.getGuiContext();

        if (guiContext != null) {
            ContextProcessor processor = new ContextProcessor(guiContext, decompiler, options, aiOptions, false);
            ContextProcessor hierarchyProcessor = new ContextProcessor(guiContext, decompiler, options, aiOptions, true);
            
            guiContext.addPopupMenuAction(
                    "Analyze with jadaix",
                    this::canProcess,
                    null,
                    processor
            );
            
            guiContext.addPopupMenuAction(
                    "Hierarchy analysis with jadaix",
                    this::canProcessHierarchy,
                    null,
                    hierarchyProcessor
            );
        }
    }

    public JadaixOptions getOptions() {
        return options;
    }

    public JadaixAIOptions getAIOptions() {
        return aiOptions;
    }
}