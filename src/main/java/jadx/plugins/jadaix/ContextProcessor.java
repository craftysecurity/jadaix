package jadx.plugins.jadaix;

import jadx.api.ICodeInfo;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.core.dex.nodes.MethodNode;
import jadx.plugins.jadaix.ui.ChatWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ContextProcessor implements Consumer<ICodeNodeRef> {
    private static final Logger LOG = LoggerFactory.getLogger(ContextProcessor.class);
    
    private final JadxGuiContext guiContext;
    private final JadxDecompiler decompiler;
    private final JadaixOptions options;
    private final JadaixAIOptions aiOptions;
    private final boolean isHierarchyMode;
    private ChatWindow chatWindow;
    private boolean isProcessing = false;

    public ContextProcessor(JadxGuiContext guiContext, JadxDecompiler decompiler, 
            JadaixOptions options, JadaixAIOptions aiOptions, boolean isHierarchyMode) {
        this.guiContext = guiContext;
        this.decompiler = decompiler;
        this.options = options;
        this.aiOptions = aiOptions;
        this.isHierarchyMode = isHierarchyMode;
    }

    @Override
    public void accept(ICodeNodeRef nodeRef) {
        if (isProcessing) {
            return;
        }
        
        try {
            isProcessing = true;
            JavaNode node = decompiler.getJavaNodeByRef(nodeRef);
            
            if (node instanceof JavaClass) {
                processClass((JavaClass) node);
            } else if (node instanceof JavaMethod) {
                processMethod((JavaMethod) node);
            } else {
                LOG.warn("Unsupported node type: {}", node.getClass());
            }
        } catch (Exception e) {
            LOG.error("Error processing node", e);
        } finally {
            isProcessing = false;
        }
    }

    private void processClass(JavaClass javaClass) {
        ChatWindow window = ChatWindow.getInstance(decompiler, options, aiOptions);
        window.setVisible(true);
        window.requestFocus();
        window.analyzeClass(javaClass, isHierarchyMode);
    }

    private void processMethod(JavaMethod javaMethod) {
        try {
            String methodCode = extractMethodCode(javaMethod);
            ChatWindow window = ChatWindow.getInstance(decompiler, options, aiOptions);
            window.setVisible(true);
            window.requestFocus();
            window.analyzeMethod(javaMethod, methodCode, isHierarchyMode);
        } catch (Exception e) {
            LOG.error("Error processing method", e);
        }
    }

    private String extractMethodCode(JavaMethod javaMethod) {
        MethodNode mth = javaMethod.getMethodNode();
        ICodeInfo codeInfo = javaMethod.getDeclaringClass().getClassNode().getCode();
        if (codeInfo == null) {
            return "// Error: Could not get method code";
        }
        
        int startPos = getCommentStartPos(codeInfo, mth.getDefPosition());
        int stopPos = getMethodEnd(mth, codeInfo);
        
        return codeInfo.getCodeStr().substring(startPos, stopPos);
    }

    private int getCommentStartPos(ICodeInfo codeInfo, int pos) {
        String emptyLine = "\n\n";
        int emptyLinePos = codeInfo.getCodeStr().lastIndexOf(emptyLine, pos);
        return emptyLinePos == -1 ? pos : emptyLinePos + emptyLine.length();
    }

    private int getMethodEnd(MethodNode mth, ICodeInfo codeInfo) {
        Integer end = codeInfo.getCodeMetadata().searchDown(mth.getDefPosition() + 1, new BiFunction<>() {
            int nested = 0;

            @Override
            public Integer apply(Integer pos, ICodeAnnotation ann) {
                switch (ann.getAnnType()) {
                    case DECLARATION:
                        ICodeNodeRef node = ((NodeDeclareRef) ann).getNode();
                        switch (node.getAnnType()) {
                            case CLASS:
                            case METHOD:
                                nested++;
                                break;
                        }
                        break;

                    case END:
                        if (nested == 0) {
                            return pos;
                        }
                        nested--;
                        break;
                }
                return null;
            }
        });
        return end != null ? end : codeInfo.getCodeStr().length();
    }
}