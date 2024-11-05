package jadx.plugins.jadaix.context;

import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.JadxDecompiler;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.plugins.jadaix.JadaixOptions;
import jadx.plugins.jadaix.JadaixAIOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContextBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ContextBuilder.class);

    private final JadxDecompiler decompiler;
    private final JadaixOptions options;
    private final JadaixAIOptions aiOptions;
    private final Set<String> processedClasses;
    private final List<Pattern> whitelistPatterns;
    private final List<Pattern> blacklistPatterns;
    private final Map<String, JavaClass> classCache;
    private final Map<String, Boolean> patternCache;

    public ContextBuilder(JadxDecompiler decompiler, JadaixOptions options, JadaixAIOptions aiOptions) {
        this.decompiler = decompiler;
        this.options = options;
        this.aiOptions = aiOptions;
        this.processedClasses = new HashSet<>();
        this.whitelistPatterns = compilePatterns(aiOptions.getPackageWhitelist());
        this.blacklistPatterns = compilePatterns(aiOptions.getPackageBlacklist());
        this.classCache = new HashMap<>();
        this.patternCache = new HashMap<>();
        buildClassCache();
    }

    private void buildClassCache() {
        for (JavaClass cls : decompiler.getClasses()) {
            classCache.put(cls.getFullName(), cls);
        }
    }

    private List<Pattern> compilePatterns(String patterns) {
        if (patterns == null || patterns.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(patterns.split(","))
                .map(String::trim)
                .filter(p -> !p.isEmpty())
                .map(p -> p.replace(".", "\\.").replace("*", ".*"))
                .map(Pattern::compile)
                .collect(Collectors.toList());
    }

    public String buildSimpleContext(JavaClass javaClass) {
        StringBuilder context = new StringBuilder();
        String code = getClassCode(javaClass);
        if (code != null && !code.isEmpty()) {
            context.append("// Class: ").append(javaClass.getFullName()).append("\n");
            context.append(code);
        }
        return context.toString();
    }

    public String buildHierarchyContext(JavaClass javaClass) {
        processedClasses.clear();
        patternCache.clear();
        StringBuilder context = new StringBuilder();
        buildHierarchyContextInternal(javaClass, context, 0);
        return context.toString();
    }

    private void buildHierarchyContextInternal(JavaClass cls, StringBuilder context, int depth) {
        if (depth > 2 || !isPackageAllowed(cls.getFullName()) || processedClasses.contains(cls.getFullName())) {
            return;
        }

        processedClasses.add(cls.getFullName());
        
        // Add class code
        String code = getClassCode(cls);
        if (code != null && !code.isEmpty()) {
            context.append("\n// Class: ").append(cls.getFullName()).append("\n");
            context.append(code).append("\n");

            // Process immediate relationships first
            processDirectRelationships(cls, context, depth);
        }
    }

    private void processDirectRelationships(JavaClass cls, StringBuilder context, int depth) {
        ClassNode clsNode = cls.getClassNode();
        
        // Handle superclass
        if (clsNode.getSuperClass() != null) {
            String superClassName = clsNode.getSuperClass().toString();
            JavaClass superCls = classCache.get(superClassName);
            if (superCls != null && isPackageAllowed(superClassName)) {
                buildHierarchyContextInternal(superCls, context, depth + 1);
            }
        }

        // Handle interfaces
        clsNode.getInterfaces().stream()
            .map(ArgType::toString)
            .map(classCache::get)
            .filter(Objects::nonNull)
            .filter(iface -> isPackageAllowed(iface.getFullName()))
            .forEach(iface -> buildHierarchyContextInternal(iface, context, depth + 1));

        // Process direct references from code
        String code = cls.getCode();
        if (code != null) {
            String pkgPrefix = getPackagePrefix(cls.getFullName());
            // Quick check for possible references in same package first
            classCache.values().stream()
                .filter(c -> c != cls)
                .filter(c -> c.getFullName().startsWith(pkgPrefix) || code.contains(c.getFullName()))
                .filter(c -> isPackageAllowed(c.getFullName()))
                .forEach(c -> buildHierarchyContextInternal(c, context, depth + 1));
        }
    }

    private String getPackagePrefix(String fullName) {
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(0, lastDot + 1) : "";
    }

    private boolean isPackageAllowed(String fullName) {
        return patternCache.computeIfAbsent(fullName, name -> {
            if (blacklistPatterns.stream().anyMatch(p -> p.matcher(name).matches())) {
                return false;
            }
            return whitelistPatterns.isEmpty() || 
                   whitelistPatterns.stream().anyMatch(p -> p.matcher(name).matches());
        });
    }

    private String getClassCode(JavaClass cls) {
        try {
            String code = cls.getCode();
            if (code != null && !code.isEmpty()) {
                return code;
            }
            return getClassCodeFromNode(cls.getClassNode());
        } catch (Exception e) {
            LOG.error("Error getting code for class: " + cls.getFullName(), e);
            return "// Error getting class code";
        }
    }

    private String getClassCodeFromNode(ClassNode cls) {
        try {
            return cls.getCode().toString();
        } catch (Exception e) {
            return "// Error getting class code for: " + cls.getFullName();
        }
    }

    public void refreshSettings() {
        processedClasses.clear();
        patternCache.clear();
    }
}