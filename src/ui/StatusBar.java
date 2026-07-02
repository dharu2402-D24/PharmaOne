package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StatusBar extends JPanel {
    private JLabel connectionLabel;
    private JLabel userLabel;
    private JLabel infoLabel;
    private Timer timer;

    public StatusBar() {
        setPreferredSize(new Dimension(0, ThemeColors.STATUSBAR_HEIGHT));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeColors.BORDER));
        setLayout(new BorderLayout());

        // Left: connection status
        connectionLabel = new JLabel("  \u25CF Connecting...");
        connectionLabel.setFont(ThemeColors.FONT_SMALL);
        connectionLabel.setForeground(ThemeColors.WARNING);
        add(connectionLabel, BorderLayout.WEST);

        // Center: user info
        userLabel = new JLabel("User | Admin", SwingConstants.CENTER);
        userLabel.setFont(ThemeColors.FONT_SMALL);
        userLabel.setForeground(ThemeColors.TEXT_SECONDARY);
        add(userLabel, BorderLayout.CENTER);

        // Right: info
        infoLabel = new JLabel("7 Tables | " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) + "  ");
        infoLabel.setFont(ThemeColors.FONT_SMALL);
        infoLabel.setForeground(ThemeColors.TEXT_SECONDARY);
        add(infoLabel, BorderLayout.EAST);

        // Update timer
        timer = new Timer(5000, e -> updateStatus());
        timer.start();
    }

    public void updateStatus() {
        boolean connected = DatabaseConnection.getInstance().isConnected();
        if (connected) {
            connectionLabel.setText("  \u25CF Connected to pharma_db");
            connectionLabel.setForeground(ThemeColors.SUCCESS);
        } else {
            connectionLabel.setText("  \u25CF Disconnected");
            connectionLabel.setForeground(ThemeColors.DANGER);
        }
        infoLabel.setText("7 Tables | " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) + "  ");
    }
}
