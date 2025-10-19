import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton, startScreenShareBtn, fileTransferBtn, remoteControlBtn;
    private JSlider volumeSlider;
    private boolean screenSharing = false;
    private boolean remoteControlEnabled = false;
    private ExecutorService executor;
    private JFrame screenFrame;
    private JLabel screenLabel;
    private JLabel statusLabel;
    private SimpleDateFormat timeFormat;

    public Server() {
        timeFormat = new SimpleDateFormat("HH:mm:ss");
        initializeGUI();
        executor = Executors.newFixedThreadPool(3);
    }

    private void initializeGUI() {
        frame = new JFrame("🎮 Server - Advanced Chat & Screen Share");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(240, 240, 245));

        // Create custom title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(70, 130, 180));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("🚀 Server Control Center");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        
        statusLabel = new JLabel("🔴 Offline");
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titlePanel.add(statusLabel, BorderLayout.EAST);
        
        frame.add(titlePanel, BorderLayout.NORTH);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 240, 245));

        // Chat area with styled border
        chatArea = new JTextArea(20, 40);
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(250, 250, 255));
        chatArea.setForeground(new Color(50, 50, 50));
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 210), 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 160)), 
            "💬 Chat Messages",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12),
            new Color(70, 130, 180)
        ));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        initializeScreenWindow();
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBackground(new Color(240, 240, 245));

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(new Color(240, 240, 245));
        
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 160)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        sendButton = createStyledButton("📤 Send", new Color(34, 139, 34));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        buttonPanel.setBackground(new Color(240, 240, 245));
        
        startScreenShareBtn = createStyledButton("📺 Start Screen Share", new Color(70, 130, 180));
        fileTransferBtn = createStyledButton("📁 File Transfer", new Color(186, 85, 211));
        remoteControlBtn = createStyledButton("🖱️ Remote Control", new Color(210, 105, 30));
        JButton clearChatBtn = createStyledButton("🗑️ Clear Chat", new Color(220, 20, 60));
        
        buttonPanel.add(startScreenShareBtn);
        buttonPanel.add(fileTransferBtn);
        buttonPanel.add(remoteControlBtn);
        buttonPanel.add(clearChatBtn);

        // Volume control
        JPanel volumePanel = new JPanel(new BorderLayout(5, 5));
        volumePanel.setBackground(new Color(240, 240, 245));
        volumePanel.setBorder(BorderFactory.createTitledBorder("🔊 Volume Control"));
        
        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setBackground(new Color(240, 240, 245));
        volumePanel.add(volumeSlider, BorderLayout.CENTER);

        // Event listeners
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        startScreenShareBtn.addActionListener(e -> toggleScreenSharing());
        fileTransferBtn.addActionListener(e -> initiateFileTransfer());
        remoteControlBtn.addActionListener(e -> toggleRemoteControl());
        clearChatBtn.addActionListener(e -> clearChat());
        volumeSlider.addChangeListener(e -> adjustVolume());

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(240, 240, 245));
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(volumePanel, BorderLayout.EAST);

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
        
        // Hover effect
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

    private void initializeScreenWindow() {
        screenFrame = new JFrame("🖥️ Client Screen View");
        screenFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        screenFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopScreenSharing();
            }
        });
        
        screenLabel = new JLabel();
        screenLabel.setHorizontalAlignment(SwingConstants.CENTER);
        screenLabel.setBackground(Color.BLACK);
        screenLabel.setOpaque(true);
        
        JScrollPane scrollPane = new JScrollPane(screenLabel);
        screenFrame.add(scrollPane);
        screenFrame.setSize(1024, 768);
        screenFrame.setLocation(100, 100);
    }

    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            updateStatus("🟢 Online - Port " + port, new Color(50, 205, 50));
            appendToChat("✅ Server started successfully on port " + port, "system");
            appendToChat("⏳ Waiting for client connection...", "system");

            clientSocket = serverSocket.accept();
            updateStatus("🟢 Connected to Client", new Color(50, 205, 50));
            appendToChat("🎉 Client connected: " + clientSocket.getInetAddress(), "system");

            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());

            executor.execute(this::listenForMessages);

        } catch (IOException e) {
            appendToChat("❌ Server error: " + e.getMessage(), "error");
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
            while (clientSocket.isConnected()) {
                Object received = inputStream.readObject();
                if (received instanceof String) {
                    String message = (String) received;
                    if (message.startsWith("CHAT:")) {
                        appendToChat("Client: " + message.substring(5), "client");
                    } else if (message.equals("SCREEN_SHARE_START")) {
                        screenSharing = true;
                        appendToChat("📺 Client started screen sharing", "system");
                        executor.execute(this::startScreenReceiver);
                    } else if (message.equals("SCREEN_SHARE_STOP")) {
                        stopScreenSharing();
                    } else if (message.startsWith("VOLUME:")) {
                        int volume = Integer.parseInt(message.substring(7));
                        volumeSlider.setValue(volume);
                        appendToChat("🔊 Client set volume to: " + volume + "%", "system");
                    }
                } else if (received instanceof ImageIcon) {
                    displayScreen((ImageIcon) received);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            appendToChat("🔌 Connection lost: " + e.getMessage(), "error");
            updateStatus("🔴 Offline", Color.RED);
        }
    }

    private void toggleScreenSharing() {
        try {
            if (!screenSharing) {
                outputStream.writeObject("SCREEN_SHARE_START");
                screenSharing = true;
                startScreenShareBtn.setText("🛑 Stop Screen Share");
                startScreenShareBtn.setBackground(new Color(220, 20, 60));
                appendToChat("📺 Requested screen sharing from client", "system");
            } else {
                stopScreenSharing();
            }
        } catch (IOException e) {
            appendToChat("❌ Error toggling screen share: " + e.getMessage(), "error");
        }
    }

    private void stopScreenSharing() {
        try {
            if (screenSharing) {
                outputStream.writeObject("SCREEN_SHARE_STOP");
                screenSharing = false;
                startScreenShareBtn.setText("📺 Start Screen Share");
                startScreenShareBtn.setBackground(new Color(70, 130, 180));
                appendToChat("🛑 Stopped screen sharing", "system");
                screenFrame.setVisible(false);
            }
        } catch (IOException e) {
            appendToChat("❌ Error stopping screen share: " + e.getMessage(), "error");
        }
    }

    private void initiateFileTransfer() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("📁 Select File to Transfer");
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            appendToChat("📤 Attempting to send file: " + file.getName(), "system");
            // File transfer implementation would go here
        }
    }

    private void toggleRemoteControl() {
        remoteControlEnabled = !remoteControlEnabled;
        if (remoteControlEnabled) {
            remoteControlBtn.setText("🛑 Disable Remote Control");
            remoteControlBtn.setBackground(new Color(220, 20, 60));
            appendToChat("🖱️ Remote control enabled", "system");
        } else {
            remoteControlBtn.setText("🖱️ Remote Control");
            remoteControlBtn.setBackground(new Color(210, 105, 30));
            appendToChat("🖱️ Remote control disabled", "system");
        }
    }

    private void adjustVolume() {
        int volume = volumeSlider.getValue();
        try {
            outputStream.writeObject("VOLUME:" + volume);
            outputStream.flush();
        } catch (IOException e) {
            appendToChat("❌ Error sending volume control: " + e.getMessage(), "error");
        }
    }

    private void clearChat() {
        chatArea.setText("");
        appendToChat("🗑️ Chat cleared", "system");
    }

    private void startScreenReceiver() {
        appendToChat("🔄 Ready to receive screen frames...", "system");
    }

    private void displayScreen(ImageIcon screenImage) {
        SwingUtilities.invokeLater(() -> {
            try {
                screenLabel.setIcon(screenImage);
                if (!screenFrame.isVisible()) {
                    screenFrame.setVisible(true);
                }
                screenFrame.revalidate();
                screenFrame.repaint();
            } catch (Exception e) {
                appendToChat("❌ Error displaying screen: " + e.getMessage(), "error");
            }
        });
    }

    private void appendToChat(String message, String type) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = "[" + timeFormat.format(new Date()) + "] ";
            switch (type) {
                case "self":
                    chatArea.append(timestamp + "💬 " + message + "\n");
                    break;
                case "client":
                    chatArea.append(timestamp + "👤 " + message + "\n");
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Server server = new Server();
            server.startServer(12345);
        });
    }
}