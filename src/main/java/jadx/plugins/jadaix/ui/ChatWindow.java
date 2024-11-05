package jadx.plugins.jadaix.ui;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JadxDecompiler;
import jadx.plugins.jadaix.JadaixOptions;
import jadx.plugins.jadaix.JadaixAIOptions;
import jadx.plugins.jadaix.processor.MessageProcessor;
import jadx.plugins.jadaix.processor.MessageProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ChatWindow extends JFrame {
    private static final Logger LOG = LoggerFactory.getLogger(ChatWindow.class);
    private static ChatWindow instance;

    private final JPanel chatPanel;
    private final JTextArea inputField;
    private final JButton sendButton;
    private final JScrollPane scrollPane;
    private final JadaixOptions options;
    private final JadaixAIOptions aiOptions;
    private final MessageProcessor messageProcessor;
    private boolean messageInProgress = false;

    // Tokyo Night theme colors
    private static final Color BACKGROUND = new Color(26, 27, 38);
    private static final Color INPUT_BG = new Color(31, 32, 43);
    private static final Color JADAIX_MSG_BG = new Color(95, 126, 151);
    private static final Color USER_MSG_BG = new Color(86, 95, 137);
    private static final Color TEXT = new Color(169, 177, 214);
    private static final Color INPUT_TEXT = Color.WHITE;
    private static final Color TRANSPARENT_BUTTON_BG = new Color(0, 0, 0, 0);

    // Layout constants
    private static final double MESSAGE_WIDTH_RATIO = 0.60;
    private static final int MIN_MESSAGE_WIDTH = 400;
    private static final int MAX_MESSAGE_WIDTH = 1200;
    private static final int SIDE_PADDING = 20;
    private static final int CORNER_RADIUS = 15;

    public static synchronized ChatWindow getInstance(JadxDecompiler decompiler, 
            JadaixOptions options, JadaixAIOptions aiOptions) {
        if (instance == null) {
            instance = new ChatWindow(decompiler, options, aiOptions);
        } else {
            instance.refreshProcessor(decompiler);
            instance.setVisible(true);
            instance.toFront();
        }
        return instance;
    }

    private ChatWindow(JadxDecompiler decompiler, JadaixOptions options, JadaixAIOptions aiOptions) {
        super("Jadaix Analysis");
        this.options = options;
        this.aiOptions = aiOptions;
        this.messageProcessor = MessageProcessorFactory.getProcessor(decompiler, options, aiOptions);

        setSize(900, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(BACKGROUND);
        chatPanel.setBorder(new EmptyBorder(10, SIDE_PADDING, 10, SIDE_PADDING));

        scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBackground(BACKGROUND);
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);

        inputField = createInputField();
        sendButton = createSendButton();
        JPanel inputPanel = createInputPanel();

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }

    private JTextArea createInputField() {
        JTextArea field = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS * 2, CORNER_RADIUS * 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setRows(1);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        field.setBackground(INPUT_BG);
        field.setForeground(INPUT_TEXT);
        field.setCaretColor(INPUT_TEXT);
        field.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        field.setOpaque(false);

        field.addKeyListener(new KeyListener() {
            @Override public void keyTyped(KeyEvent e) {}
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        field.append("\n");
                    } else {
                        e.consume();
                        sendMessage();
                    }
                }
            }
            @Override public void keyReleased(KeyEvent e) {}
        });
        return field;
    }

    private JButton createSendButton() {
        JButton button = new JButton("Send") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(JADAIX_MSG_BG.darker());
                } else if (messageInProgress) {
                    g2.setColor(TRANSPARENT_BUTTON_BG);
                } else {
                    g2.setColor(JADAIX_MSG_BG);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS * 2, CORNER_RADIUS * 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setBackground(JADAIX_MSG_BG);
        button.setForeground(INPUT_TEXT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(e -> sendMessage());
        button.setPreferredSize(new Dimension(100, 36));
        return button;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setBorder(new EmptyBorder(10, SIDE_PADDING, 10, SIDE_PADDING));
        panel.setBackground(BACKGROUND);
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        return panel;
    }

    public void analyzeClass(JavaClass javaClass, boolean isHierarchyMode) {
        appendMessage("System", "Analyzing class: " + javaClass.getFullName() + 
                     (isHierarchyMode ? " (with hierarchy analysis)" : ""));

        setInputEnabled(false);
        messageInProgress = true;
        sendButton.repaint();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                messageProcessor.processClass(javaClass, isHierarchyMode, response -> {
                    SwingUtilities.invokeLater(() -> {
                        appendMessage("jadaix", response);
                        setInputEnabled(true);
                        messageInProgress = false;
                        sendButton.repaint();
                    });
                });
                return null;
            }
        };
        worker.execute();
    }

    public void analyzeMethod(JavaMethod javaMethod, String methodCode, boolean isHierarchyMode) {
        appendMessage("System", "Analyzing method: " + javaMethod.getFullName() + 
                     (isHierarchyMode ? " (with hierarchy analysis)" : ""));

        setInputEnabled(false);
        messageInProgress = true;
        sendButton.repaint();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                messageProcessor.processMethod(javaMethod, methodCode, isHierarchyMode, response -> {
                    SwingUtilities.invokeLater(() -> {
                        appendMessage("jadaix", response);
                        setInputEnabled(true);
                        messageInProgress = false;
                        sendButton.repaint();
                    });
                });
                return null;
            }
        };
        worker.execute();
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            appendMessage("You", message);
            inputField.setText("");

            setInputEnabled(false);
            messageInProgress = true;
            sendButton.repaint();

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    messageProcessor.sendMessage(message, response -> {
                        SwingUtilities.invokeLater(() -> {
                            appendMessage("jadaix", response);
                            setInputEnabled(true);
                            messageInProgress = false;
                            sendButton.repaint();
                        });
                    });
                    return null;
                }
            };
            worker.execute();
        }
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
    }

    public void appendMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            MessagePanel messagePanel = new MessagePanel(message, sender.equals("You"), calculateMessageWidth());
            chatPanel.add(messagePanel);
            chatPanel.revalidate();
            chatPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
        });
    }

    private int calculateMessageWidth() {
        int windowWidth = getWidth();
        int calculatedWidth = (int)(windowWidth * MESSAGE_WIDTH_RATIO);
        return Math.max(MIN_MESSAGE_WIDTH, Math.min(calculatedWidth, MAX_MESSAGE_WIDTH));
    }

    @Override
    public void dispose() {
        setVisible(false);
    }

    public void refreshProcessor(JadxDecompiler decompiler) {
        MessageProcessorFactory.refreshProcessor(decompiler, options, aiOptions);
    }
}