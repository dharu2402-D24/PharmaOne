package ui;

import util.ThemeColors;
import javax.swing.*;
import java.awt.*;

public class HeaderPanel extends JPanel {
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    public HeaderPanel(MainFrame mainFrame) {
        setPreferredSize(new Dimension(0, ThemeColors.HEADER_HEIGHT));
        setBackground(ThemeColors.BG_HEADER);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER));
        setLayout(new BorderLayout());

        // Left: title
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 0));

        titleLabel = new JLabel("Dashboard");
        titleLabel.setFont(ThemeColors.FONT_TITLE);
        titleLabel.setForeground(ThemeColors.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleLabel = new JLabel("Modern pharma inventory management dashboard");
        subtitleLabel.setFont(ThemeColors.FONT_SMALL);
        subtitleLabel.setForeground(ThemeColors.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        add(titlePanel, BorderLayout.WEST);

        // Right: refresh + profile
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        rightPanel.setOpaque(false);

        // Refresh button
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(ThemeColors.FONT_BUTTON);
        refreshBtn.setBackground(ThemeColors.PRIMARY);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.setToolTipText("Refresh all data (F5)");
        refreshBtn.addActionListener(e -> mainFrame.refreshAllPanels());
        rightPanel.add(refreshBtn);

        // Profile
        JPanel profilePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        profilePanel.setOpaque(false);
        JLabel avatar = new JLabel(util.CustomIcons.getUserOutlineIcon(28, ThemeColors.TEXT_PRIMARY));
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);
        JLabel nameLbl = new JLabel("User");
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(ThemeColors.TEXT_PRIMARY);
        JLabel roleLbl = new JLabel("Admin");
        roleLbl.setFont(ThemeColors.FONT_SMALL);
        roleLbl.setForeground(ThemeColors.TEXT_SECONDARY);
        namePanel.add(nameLbl);
        namePanel.add(roleLbl);
        profilePanel.add(avatar);
        profilePanel.add(namePanel);
        rightPanel.add(profilePanel);

        add(rightPanel, BorderLayout.EAST);
    }

    public void setTitle(String title, String subtitle) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
    }
}
