package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import java.awt.*;

public class SettingsPanel extends JPanel {

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeColors.BG_MAIN);
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Left tabs
        JPanel tabPanel = new JPanel();
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.Y_AXIS));
        tabPanel.setPreferredSize(new Dimension(200, 0));
        tabPanel.setBackground(Color.WHITE);
        tabPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(8, 0, 8, 0)));

        String[] tabs = {"Database Connection", "Preferences & About", "Schema Viewer"};
        CardLayout cards = new CardLayout();
        JPanel contentPanel = new JPanel(cards);
        contentPanel.setOpaque(false);

        for (String tab : tabs) {
            JButton btn = new JButton(tab);
            btn.setFont(ThemeColors.FONT_NAV);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(200, 40));
            btn.setBackground(Color.WHITE);
            btn.setForeground(ThemeColors.TEXT_SECONDARY);
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                cards.show(contentPanel, tab);
                for (Component c : tabPanel.getComponents()) {
                    if (c instanceof JButton) {
                        c.setForeground(ThemeColors.TEXT_SECONDARY);
                        ((JButton)c).setBackground(Color.WHITE);
                    }
                }
                btn.setForeground(ThemeColors.PRIMARY);
                btn.setBackground(ThemeColors.PRIMARY_LIGHT);
            });
            tabPanel.add(btn);
        }
        ((JButton)tabPanel.getComponent(0)).setForeground(ThemeColors.PRIMARY);
        ((JButton)tabPanel.getComponent(0)).setBackground(ThemeColors.PRIMARY_LIGHT);

        // Tab 1: Database Connection
        JPanel dbPanel = createCardPanel();
        dbPanel.setLayout(new BoxLayout(dbPanel, BoxLayout.Y_AXIS));
        addSectionTitle(dbPanel, "Database Connection Settings");
        DatabaseConnection db = DatabaseConnection.getInstance();

        JPanel form = new JPanel(new GridLayout(4, 2, 12, 12));
        form.setOpaque(false);
        form.setMaximumSize(new Dimension(600, 200));
        form.setAlignmentX(LEFT_ALIGNMENT);
        JTextField hostF = new JTextField(db.getHost());
        JTextField portF = new JTextField(String.valueOf(db.getPort()));
        JTextField dbF = new JTextField(db.getDatabase());
        JTextField userF = new JTextField(db.getUsername());
        form.add(createLabel("Host:")); form.add(hostF);
        form.add(createLabel("Port:")); form.add(portF);
        form.add(createLabel("Database:")); form.add(dbF);
        form.add(createLabel("Username:")); form.add(userF);
        dbPanel.add(form);
        dbPanel.add(Box.createVerticalStrut(16));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);
        JButton testBtn = new JButton("Test Connection");
        testBtn.setFont(ThemeColors.FONT_BUTTON);
        testBtn.setBackground(ThemeColors.PRIMARY);
        testBtn.setForeground(Color.WHITE);
        testBtn.setFocusPainted(false);
        testBtn.setBorderPainted(false);
        testBtn.addActionListener(e -> {
            boolean connected = db.isConnected();
            JOptionPane.showMessageDialog(this,
                connected ? "Connection successful!" : "Connection failed. Check settings.",
                "Connection Test", connected ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        });
        btnPanel.add(testBtn);
        dbPanel.add(btnPanel);
        contentPanel.add(dbPanel, "Database Connection");

        // Tab 2: Preferences & About
        JPanel infoPanel = createCardPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        addSectionTitle(infoPanel, "About Application");
        addInfoRow(infoPanel, "Application:", "PharmaOne Inventory Management System");
        addInfoRow(infoPanel, "Version:", "1.0.0");
        addInfoRow(infoPanel, "Project:", "Pharma Wholesaler Inventory Management");
        addInfoRow(infoPanel, "Technology:", "Java Swing + JDBC + MySQL");
        addInfoRow(infoPanel, "Database:", "pharma_db (7 tables)");
        addInfoRow(infoPanel, "Course:", "DBMS - Semester 4");
        contentPanel.add(infoPanel, "Preferences & About");

        // Tab 3: Schema Viewer
        JPanel schemaPanel = createCardPanel();
        schemaPanel.setLayout(new BoxLayout(schemaPanel, BoxLayout.Y_AXIS));
        addSectionTitle(schemaPanel, "Database Schema Overview");
        String[] tables = {"Medicine", "Supplier", "Customer", "Batch", "Inventory", "Purchase_Order", "Sales_Order"};
        for (String tbl : tables) {
            JLabel lbl = new JLabel("\u25CF " + tbl);
            lbl.setFont(ThemeColors.FONT_BODY);
            lbl.setForeground(ThemeColors.TEXT_PRIMARY);
            lbl.setAlignmentX(LEFT_ALIGNMENT);
            lbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            schemaPanel.add(lbl);
        }
        schemaPanel.add(Box.createVerticalStrut(16));
        JButton viewSchemaBtn = new JButton("View Full Schema (DESC)");
        viewSchemaBtn.setFont(ThemeColors.FONT_BUTTON);
        viewSchemaBtn.setBackground(ThemeColors.PRIMARY);
        viewSchemaBtn.setForeground(Color.WHITE);
        viewSchemaBtn.setFocusPainted(false);
        viewSchemaBtn.setBorderPainted(false);
        viewSchemaBtn.setAlignmentX(LEFT_ALIGNMENT);
        viewSchemaBtn.addActionListener(e -> showSchemaDetails());
        schemaPanel.add(viewSchemaBtn);
        contentPanel.add(schemaPanel, "Schema Viewer");

        wrapper.add(tabPanel, BorderLayout.WEST);
        wrapper.add(contentPanel, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private void showSchemaDetails() {
        StringBuilder sb = new StringBuilder();
        String[] tables = {"Medicine", "Supplier", "Customer", "Batch", "Inventory", "Purchase_Order", "Sales_Order"};
        try {
            for (String tbl : tables) {
                sb.append("=== ").append(tbl).append(" ===\n");
                var rs = DatabaseConnection.getInstance().executeQuery("DESCRIBE " + tbl);
                while (rs.next()) {
                    sb.append(String.format("  %-25s %-15s %s\n", rs.getString("Field"), rs.getString("Type"), rs.getString("Key")));
                }
                rs.close();
                sb.append("\n");
            }
        } catch (Exception ex) {
            sb.append("Error: ").append(ex.getMessage());
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(600, 500));
        JOptionPane.showMessageDialog(this, scroll, "Database Schema", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel createCardPanel() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        return p;
    }

    private void addSectionTitle(JPanel p, String title) {
        JLabel l = new JLabel(title);
        l.setFont(ThemeColors.FONT_SECTION);
        l.setForeground(ThemeColors.TEXT_PRIMARY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(16));
    }

    private void addInfoRow(JPanel p, String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(600, 30));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(ThemeColors.TEXT_SECONDARY);
        JLabel v = new JLabel(value);
        v.setFont(ThemeColors.FONT_BODY);
        v.setForeground(ThemeColors.TEXT_PRIMARY);
        row.add(l); row.add(v);
        p.add(row);
    }

    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(ThemeColors.TEXT_SECONDARY);
        return l;
    }
}
