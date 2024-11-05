package jadx.plugins.jadaix.ui;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessagePanel extends JPanel {
    private static final Logger LOG = LoggerFactory.getLogger(MessagePanel.class);

    // Tokyo Night theme colors
    private static final Color BACKGROUND = new Color(26, 27, 38);
    private static final Color USER_MSG_BG = new Color(95, 126, 151);
    private static final Color JADAIX_MSG_BG = new Color(86, 95, 137);
    private static final Color TEXT_COLOR = new Color(169, 177, 214);
    private static final Color CODE_BG = new Color(31, 32, 43);

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");
    private static final Parser PARSER = Parser.builder(new MutableDataSet()).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(new MutableDataSet()).build();

    private final String message;
    private final boolean isUser;
    private final int width;

    public MessagePanel(String message, boolean isUser, int width) {
        this.message = message;
        this.isUser = isUser;
        this.width = width;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(2, 5, 2, 5));

        // Create bubble panel with proper coloring
        JPanel bubblePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.setBackground(isUser ? USER_MSG_BG : JADAIX_MSG_BG);
        bubblePanel.setOpaque(false);
        bubblePanel.setBorder(new RoundedBorder(30, isUser ? USER_MSG_BG : JADAIX_MSG_BG));

        // Process message for code blocks first
        processMessage(message, bubblePanel);

        // Add copy button at the bottom for LLM responses
        if (!isUser) {
            JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            footerPanel.setOpaque(false);
            footerPanel.add(createCopyButton());
            bubblePanel.add(footerPanel);
        }

        // Wrap bubble panel for alignment
        JPanel wrapperPanel = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        wrapperPanel.setBackground(BACKGROUND);
        wrapperPanel.add(bubblePanel);
        add(wrapperPanel);
    }

    
    private JButton createCopyButton() {
        JButton copyButton = new JButton() {
            private final String COPY_ICON = "⎘"; // Unicode copy symbol

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Draw icon
                g2.setColor(TEXT_COLOR);
                g2.setFont(new Font("Dialog", Font.PLAIN, 16));
                FontMetrics fm = g2.getFontMetrics();
                Rectangle2D bounds = fm.getStringBounds(COPY_ICON, g2);
                int x = (getWidth() - (int)bounds.getWidth()) / 2;
                int y = ((getHeight() - (int)bounds.getHeight()) / 2) + fm.getAscent();
                g2.drawString(COPY_ICON, x, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(24, 24);
            }
        };

        copyButton.setOpaque(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setBorderPainted(false);
        copyButton.setFocusPainted(false);
        copyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyButton.setToolTipText("Copy to clipboard");
        copyButton.addActionListener(e -> copyMessageToClipboard());
        
        return copyButton;
    }

    private void copyMessageToClipboard() {
        try {
            StringSelection selection = new StringSelection(message);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            LOG.debug("Message copied to clipboard");
        } catch (Exception e) {
            LOG.error("Failed to copy message to clipboard", e);
        }
    }

    private void processMessage(String message, JPanel bubblePanel) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(message);
        int lastEnd = 0;

        while (matcher.find()) {
            // Add text before code block
            String textBefore = message.substring(lastEnd, matcher.start());
            if (!textBefore.trim().isEmpty()) {
                addMarkdownText(textBefore, bubblePanel);
            }

            // Add code block
            String language = matcher.group(1);
            String code = matcher.group(2);
            addCodeBlock(code, language, bubblePanel);

            lastEnd = matcher.end();
        }

        // Add remaining text
        if (lastEnd < message.length()) {
            String remainingText = message.substring(lastEnd);
            if (!remainingText.trim().isEmpty()) {
                addMarkdownText(remainingText, bubblePanel);
            }
        }
    }

    private void addMarkdownText(String text, JPanel bubblePanel) {
        JEditorPane textPane = new JEditorPane("text/html", RENDERER.render(PARSER.parse(text)));
        textPane.setEditable(false);
        textPane.setBackground(bubblePanel.getBackground());
        textPane.setForeground(TEXT_COLOR);
        textPane.setBorder(null);

        // Calculate size with increased initial capacity
        int textWidth = width - 80;
        textPane.setSize(textWidth, 9999);

        // Get the actual preferred height
        int heightNeeded = textPane.getPreferredSize().height;

        // Set final size with extra padding
        textPane.setPreferredSize(new Dimension(textWidth, heightNeeded + 20));

        // Wrap textPane in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        // Add padding
        JPanel paddingPanel = new JPanel(new BorderLayout());
        paddingPanel.setBackground(bubblePanel.getBackground());
        paddingPanel.setBorder(new EmptyBorder(8, 15, 8, 15));
        paddingPanel.add(scrollPane);

        bubblePanel.add(paddingPanel);
    }

    private void addCodeBlock(String code, String language, JPanel bubblePanel) {
        RSyntaxTextArea codeArea = new RSyntaxTextArea();

        if (language != null) {
            setSyntaxStyle(codeArea, language);
        }

        try {
            Theme theme = Theme.load(getClass().getResourceAsStream("/themes/tokyo-night.xml"));
            theme.apply(codeArea);
        } catch (Exception e) {
            LOG.error("Failed to load theme", e);
        }

        codeArea.setText(code);
        codeArea.setEditable(false);
        codeArea.setBackground(CODE_BG);
        codeArea.setForeground(TEXT_COLOR);

        // Calculate proper size for code blocks
        int textWidth = width - 80;
        codeArea.setSize(textWidth, 9999);
        int heightNeeded = codeArea.getPreferredSize().height;
        codeArea.setPreferredSize(new Dimension(textWidth, heightNeeded + 10));

        // Wrap codeArea in a JScrollPane
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        JPanel paddingPanel = new JPanel(new BorderLayout());
        paddingPanel.setBackground(bubblePanel.getBackground());
        paddingPanel.setBorder(new EmptyBorder(6, 12, 6, 12));
        paddingPanel.add(scrollPane);

        bubblePanel.add(paddingPanel);
    }

    private static void setSyntaxStyle(RSyntaxTextArea codeArea, String language) {
        switch (language.toLowerCase()) {
            case "java":
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                break;
            case "python":
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                break;
            case "javascript":
            case "js":
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            case "kotlin":
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_KOTLIN);
                break;
            case "xml":
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                break;
            case "json":
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                break;
            default:
                codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }
    }

    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, width - 1, height - 1, radius * 2, radius * 2);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(radius, radius, radius, radius);
            return insets;
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }
}