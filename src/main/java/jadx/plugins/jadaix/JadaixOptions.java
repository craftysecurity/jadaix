package jadx.plugins.jadaix;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class JadaixOptions extends BasePluginOptionsBuilder {
    private boolean enable;
    private boolean hierarchyEnable;
    private boolean debugEnable;

    @Override
    public void registerOptions() {
        boolOption(JadaixPlugin.PLUGIN_ID + ".enable")
                .description("Enable Jadaix plugin")
                .defaultValue(true)
                .setter(v -> enable = v);

        boolOption(JadaixPlugin.PLUGIN_ID + ".debug.enable")
                .description("Debug mode")
                .defaultValue(false)
                .setter(v -> debugEnable = v);

        boolOption(JadaixPlugin.PLUGIN_ID + ".hierarchy.enable")
                .description("Allow hierarchy analysis")
                .defaultValue(false)
                .setter(v -> hierarchyEnable = v);
    }

    public boolean isEnabled() {
        return enable;
    }

    public boolean isHierarchyAnalysisEnabled() {
        return hierarchyEnable;
    }

    public boolean isDebugEnabled() {
        return debugEnable;
    }
}