package ui;

import util.ThemeColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Sidebar extends JPanel {
    private MainFrame mainFrame;
    private String activeItem = "Dashboard";
    private String[] navItems = {"Dashboard", "Inventory", "Orders", "Suppliers", "Customers", "Reports", "Discarded", "Transactions", "Settings"};

    public Sidebar(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setPreferredSize(new Dimension(ThemeColors.SIDEBAR_WIDTH, 0));
        setBackground(ThemeColors.BG_SIDEBAR);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeColors.BORDER));
        setLayout(new BorderLayout());
        buildSidebar();
    }

    private void buildSidebar() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        // Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(ThemeColors.SIDEBAR_WIDTH, ThemeColors.HEADER_HEIGHT));
        logoPanel.setMaximumSize(new Dimension(ThemeColors.SIDEBAR_WIDTH, ThemeColors.HEADER_HEIGHT));

        JLabel iconLabel = new JLabel(util.CustomIcons.getPharmaLogoIcon(28));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(36, 36));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        JLabel titleLabel = new JLabel("PharmaOne");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        logoPanel.add(iconLabel);
        logoPanel.add(titleLabel);
        topPanel.add(logoPanel);
        topPanel.add(Box.createVerticalStrut(16));

        // Nav items
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setOpaque(false);
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        for (String item : navItems) {
            navPanel.add(createNavItem(item));
            navPanel.add(Box.createVerticalStrut(4));
        }
        topPanel.add(navPanel);
        add(topPanel, BorderLayout.NORTH);
    }

    private Icon getNavIcon(String name, Color color) {
        return switch (name) {
            case "Dashboard" -> util.CustomIcons.getDashboardIcon(18, color);
            case "Inventory" -> util.CustomIcons.getInventoryIcon(18, color);
            case "Orders" -> util.CustomIcons.getOrdersIcon(18, color);
            case "Suppliers" -> util.CustomIcons.getSuppliersIcon(18, color);
            case "Customers" -> util.CustomIcons.getUserOutlineIcon(18, color);
            case "Reports" -> util.CustomIcons.getReportsIcon(18, color);
            case "Settings" -> util.CustomIcons.getSettingsIcon(18, color);
            case "Discarded" -> util.CustomIcons.getWarningIcon(18, color);
            case "Transactions" -> util.CustomIcons.getTransactionIcon(18, color);
            default -> null;
        };
    }

    private JPanel createNavItem(String name) {
        JPanel item = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (name.equals(activeItem)) {
                    g2.setColor(ThemeColors.PRIMARY_LIGHT);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                g2.dispose();
            }
        };
        item.setOpaque(false);
        item.setPreferredSize(new Dimension(ThemeColors.SIDEBAR_WIDTH - 24, 44));
        item.setMaximumSize(new Dimension(ThemeColors.SIDEBAR_WIDTH - 24, 44));
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        Color iconColor = name.equals(activeItem) ? ThemeColors.PRIMARY : ThemeColors.TEXT_SECONDARY;

        // Icon label — proper Graphics2D icon, no emoji
        JLabel iconLbl = new JLabel(getNavIcon(name, iconColor));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setPreferredSize(new Dimension(24, 44));

        // Text label — plain text, no emoji
        JLabel textLbl = new JLabel("  " + name);
        textLbl.setFont(name.equals(activeItem) ? ThemeColors.FONT_NAV_ACTIVE : ThemeColors.FONT_NAV);
        textLbl.setForeground(name.equals(activeItem) ? ThemeColors.PRIMARY : ThemeColors.TEXT_SECONDARY);

        item.add(iconLbl, BorderLayout.WEST);
        item.add(textLbl, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setActive(name);
                mainFrame.showPanel(name);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!name.equals(activeItem)) {
                    item.setBackground(new Color(248, 250, 252));
                    item.setOpaque(true);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                item.setOpaque(false);
                item.repaint();
            }
        });

        return item;
    }

    public void setActive(String name) {
        activeItem = name;
        removeAll();
        buildSidebar();
        revalidate();
        repaint();
    }
}
