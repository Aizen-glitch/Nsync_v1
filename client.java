import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, startScreenShareBtn, fileTransferBtn, systemInfoBtn;
    private JSlider volumeSlider;
    private JProgressBar cpuUsageBar;
    private boolean screenSharing = false;
    private ExecutorService executor;
    private Robot robot;
    private JLabel statusLabel;
    private SimpleDateFormat timeFormat;

    public Client() {
        timeFormat = new SimpleDateFormat("HH:mm:ss");
        try {
            robot = new Robot();
        } catch (AWTException e) {
            showError("Cannot create screen capture robot: " + e.getMessage());
        }
        initializeGUI();
        executor = Executors.newFixedThreadPool(3);
    }

    private void initializeGUI() {
        frame = new JFrame("🎮 Client - Advanced Chat & Screen Share");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(245, 240, 240));

        // Custom title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(178, 34, 34));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("🚀 Client Terminal");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        statusLabel = new JLabel("🔴 Disconnected");
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titlePanel.add(statusLabel, BorderLayout.EAST);
        
        frame.add(titlePanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(245, 240, 240));

        // Chat area
        chatArea = new JTextArea(20, 40);
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(255, 250, 250));
        chatArea.setForeground(new Color(50, 50, 50));
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 200, 200), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(160, 150, 150)), 
            "💬 Chat Messages",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(178, 34, 34)
        ));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBackground(new Color(245, 240, 240));

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(new Color(245, 240, 240));
        
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(160, 150, 150)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        sendButton = createStyledButton("📤 Send", new Color(34, 139, 34));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        buttonPanel.setBackground(new Color(245, 240, 240));
        
        startScreenShareBtn = createStyledButton("📺 Start Screen Share", new Color(178, 34, 34));
        fileTransferBtn = createStyledButton("📁 Send File", new Color(186, 85, 211));
        systemInfoBtn = createStyledButton("💻 System Info", new Color(65, 105, 225));
        JButton clearChatBtn = createStyledButton("🗑️ Clear Chat", new Color(220, 20, 60));
        
        buttonPanel.add(startScreenShareBtn);
        buttonPanel.add(fileTransferBtn);
        buttonPanel.add(systemInfoBtn);
        buttonPanel.add(clearChatBtn);

        // System monitor panel
        JPanel systemPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        systemPanel.setBackground(new Color(245, 240, 240));
        systemPanel.setBorder(BorderFactory.createTitledBorder("📊 System Monitor"));
        
        // Volume control
        JPanel volumePanel = new JPanel(new BorderLayout(5, 5));
        volumePanel.setBackground(new Color(245, 240, 240));
        
        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setBackground(new Color(245, 240, 240));
        volumePanel.add(new JLabel("🔊 Volume:"), BorderLayout.WEST);
        volumePanel.add(volumeSlider, BorderLayout.CENTER);

        // CPU usage simulation
        JPanel cpuPanel = new JPanel(new BorderLayout(5, 5));
        cpuPanel.setBackground(new Color(245, 240, 240));
        
        cpuUsageBar = new JProgressBar(0, 100);
        cpuUsageBar.setValue(25);
        cpuUsageBar.setStringPainted(true);
        cpuUsageBar.setForeground(new Color(65, 105, 225));
        cpuPanel.add(new JLabel("💾 CPU:"), BorderLayout.WEST);
        cpuPanel.add(cpuUsageBar, BorderLayout.CENTER);

        systemPanel.add(volumePanel);
        systemPanel.add(cpuPanel);

        // Event listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        startScreenShareBtn.addActionListener(e -> toggleScreenSharing());
        fileTransferBtn.addActionListener(e -> sendFile());
        systemInfoBtn.addActionListener(e -> showSystemInfo());
        clearChatBtn.addActionListener(e -> clearChat());
        volumeSlider.addChangeListener(e -> sendVolume());

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(245, 240, 240));
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(systemPanel, BorderLayout.EAST);

        controlPanel.add(topPanel, BorderLayout.NORTH);
        controlPanel.add(buttonPanel, BorderLayout.CENTER);

        return controlPanel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });
        
        return button;
    }

    public void connectToServer(String host, int port) {
        try {
            socket = new Socket(host, port);
            updateStatus("🟢 Connected to Server", new Color(50, 205, 50));
            appendToChat("✅ Connected to server: " + host + ":" + port, "system");

            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            executor.execute(this::listenForMessages);
            executor.execute(this::simulateSystemMetrics);

        } catch (IOException e) {
            appendToChat("❌ Connection error: " + e.getMessage(), "error");
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                outputStream.writeObject("CHAT:" + message);
                outputStream.flush();
                appendToChat("You: " + message, "self");
                messageField.setText("");
            } catch (IOException e) {
                appendToChat("❌ Error sending message: " + e.getMessage(), "error");
            }
        }
    }

    private void listenForMessages() {
        try {
            while (socket.isConnected()) {
                Object received = inputStream.readObject();
                if (received instanceof String) {
                    String message = (String) received;
                    if (message.startsWith("CHAT:")) {
                        appendToChat("Server: " + message.substring(5), "server");
                    } else if (message.equals("SCREEN_SHARE_START")) {
                        if (!screenSharing) {
                            screenSharing = true;
                            startScreenShareBtn.setText("🛑 Stop Screen Share");
                            startScreenShareBtn.setBackground(new Color(220, 20, 60));
                            appendToChat("📺 Server requested screen sharing - Starting...", "system");
                            executor.execute(this::startScreenSharing);
                        }
                    } else if (message.equals("SCREEN_SHARE_STOP")) {
                        screenSharing = false;
                        startScreenShareBtn.setText("📺 Start Screen Share");
                        startScreenShareBtn.setBackground(new Color(178, 34, 34));
                        appendToChat("🛑 Server stopped screen sharing", "system");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            appendToChat("🔌 Connection lost: " + e.getMessage(), "error");
            updateStatus("🔴 Disconnected", Color.RED);
        }
    }

    private void toggleScreenSharing() {
        try {
            if (!screenSharing) {
                outputStream.writeObject("SCREEN_SHARE_START");
                screenSharing = true;
                startScreenShareBtn.setText("🛑 Stop Screen Share");
                startScreenShareBtn.setBackground(new Color(220, 20, 60));
                appendToChat("📺 Started screen sharing with server", "system");
                executor.execute(this::startScreenSharing);
            } else {
                outputStream.writeObject("SCREEN_SHARE_STOP");
                screenSharing = false;
                startScreenShareBtn.setText("📺 Start Screen Share");
                startScreenShareBtn.setBackground(new Color(178, 34, 34));
                appendToChat("🛑 Stopped screen sharing", "system");
            }
        } catch (IOException e) {
            appendToChat("❌ Error toggling screen share: " + e.getMessage(), "error");
        }
    }

    private void startScreenSharing() {
        try {
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            appendToChat("🔄 Screen sharing started...", "system");
            
            while (screenSharing && socket.isConnected()) {
                BufferedImage screenImage = robot.createScreenCapture(screenRect);
                
                // Scale down for performance
                int newWidth = screenImage.getWidth() / 2;
                int newHeight = screenImage.getHeight() / 2;
                Image scaledImage = screenImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                BufferedImage scaledBuffered = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = scaledBuffered.createGraphics();
                g2d.drawImage(scaledImage, 0, 0, null);
                g2d.dispose();
                
                ImageIcon imageIcon = new ImageIcon(scaledBuffered);
                outputStream.writeObject(imageIcon);
                outputStream.flush();
                outputStream.reset();
                
                Thread.sleep(200); // 5 FPS
            }
            appendToChat("📺 Screen sharing stopped", "system");
        } catch (IOException | InterruptedException e) {
            appendToChat("❌ Screen sharing error: " + e.getMessage(), "error");
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("📁 Select File to Send");
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            appendToChat("📤 Preparing to send file: " + file.getName(), "system");
            // File transfer implementation
        }
    }

    private void showSystemInfo() {
        String systemInfo = String.format(
            "💻 System Information:\n" +
            "OS: %s %s\n" +
            "Java: %s\n" +
            "Cores: %d\n" +
            "Memory: %dMB",
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("java.version"),
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().maxMemory() / (1024 * 1024)
        );
        appendToChat(systemInfo, "system");
    }

    private void sendVolume() {
        int volume = volumeSlider.getValue();
        try {
            outputStream.writeObject("VOLUME:" + volume);
            outputStream.flush();
        } catch (IOException e) {
            appendToChat("❌ Error sending volume: " + e.getMessage(), "error");
        }
    }

    private void clearChat() {
        chatArea.setText("");
        appendToChat("🗑️ Chat cleared", "system");
    }

    private void simulateSystemMetrics() {
        while (true) {
            try {
                // Simulate CPU usage changes
                int cpuUsage = 20 + (int)(Math.random() * 60);
                cpuUsageBar.setValue(cpuUsage);
                cpuUsageBar.setString(cpuUsage + "%");
                
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void appendToChat(String message, String type) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = "[" + timeFormat.format(new Date()) + "] ";
            switch (type) {
                case "self":
                    chatArea.append(timestamp + "💬 " + message + "\n");
                    break;
                case "server":
                    chatArea.append(timestamp + "👑 " + message + "\n");
                    break;
                case "system":
                    chatArea.append(timestamp + "⚡ " + message + "\n");
                    break;
                case "error":
                    chatArea.append(timestamp + "❌ " + message + "\n");
                    break;
                default:
                    chatArea.append(timestamp + message + "\n");
            }
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setForeground(color);
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            
            // Fixed the JOptionPane call - removed the frame reference and properly handle the return type
            String serverHost = (String) JOptionPane.showInputDialog(
                null, // Use null instead of frame for static context
                "Enter server host:", 
                "Server Connection", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                "localhost"
            );
            
            if (serverHost != null && !serverHost.trim().isEmpty()) {
                client.connectToServer(serverHost, 12345);
            } else {
                // Use default if user cancels or enters nothing
                client.connectToServer("localhost", 12345);
            }
        });
    }
}