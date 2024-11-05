package jadx.plugins.jadaix.context;

import jadx.api.JavaClass;
import jadx.api.JadxDecompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(ClassIndexer.class);
    
    private final Map<String, JavaClass> classIndex = new HashMap<>();
    private final Map<String, Set<String>> classReferences = new HashMap<>();
    private final JadxDecompiler decompiler;

    public ClassIndexer(JadxDecompiler decompiler) {
        this.decompiler = decompiler;
        buildIndex();
    }

    private void buildIndex() {
        LOG.debug("Building class index...");
        for (JavaClass cls : decompiler.getClasses()) {
            String fullName = cls.getFullName();
            classIndex.put(fullName, cls);
            classReferences.put(fullName, new HashSet<>());
            
            // Index references in code
            String code = cls.getCode();
            for (JavaClass otherClass : decompiler.getClasses()) {
                if (code.contains(otherClass.getFullName())) {
                    classReferences.get(fullName).add(otherClass.getFullName());
                }
            }
        }
        LOG.debug("Indexed {} classes", classIndex.size());
    }

    public JavaClass findClass(String className) {
        return classIndex.get(className);
    }

    public Set<String> getReferences(String className) {
        return classReferences.getOrDefault(className, new HashSet<>());
    }

    public Set<String> traceUsages(String className) {
        Set<String> usages = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : classReferences.entrySet()) {
            if (entry.getValue().contains(className)) {
                usages.add(entry.getKey());
            }
        }
        return usages;
    }
}