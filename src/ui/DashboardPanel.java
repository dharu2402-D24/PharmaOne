package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class DashboardPanel extends JPanel {

    private JLabel statTotalValue, statItemsInStock, statLowStock, statExpiringSoon;
    private JLabel statOutOfStock, statCompletedOrders, statTotalOrderValue;
    private JLabel statTotalProfit, statTotalLosses;
    private DefaultTableModel ordersModel;
    private JPanel criticalStockList, expiringList;

    public DashboardPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(ThemeColors.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // ── Inventory stat cards ──
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        statsRow.setAlignmentX(LEFT_ALIGNMENT);

        statTotalValue = new JLabel("₹0");
        statsRow.add(createStatCard("Total Stock Value", statTotalValue, ThemeColors.PRIMARY,
                util.CustomIcons.getMoneyIcon(28, ThemeColors.PRIMARY)));
        statItemsInStock = new JLabel("0");
        statsRow.add(createStatCard("Items In Stock", statItemsInStock, ThemeColors.SUCCESS,
                util.CustomIcons.getInventoryIcon(28, ThemeColors.SUCCESS)));
        statLowStock = new JLabel("0");
        statsRow.add(createStatCard("Low Stock Items", statLowStock, ThemeColors.WARNING,
                util.CustomIcons.getWarningIcon(28, ThemeColors.WARNING)));
        statExpiringSoon = new JLabel("0");
        statsRow.add(createStatCard("Expiring Soon", statExpiringSoon, ThemeColors.DANGER,
                util.CustomIcons.getClockIcon(28, ThemeColors.DANGER)));
        content.add(statsRow);
        content.add(Box.createVerticalStrut(16));

        // ── Order stat cards ──
        JPanel orderStatsRow = new JPanel(new GridLayout(1, 5, 16, 0));
        orderStatsRow.setOpaque(false);
        orderStatsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        orderStatsRow.setAlignmentX(LEFT_ALIGNMENT);

        statOutOfStock = new JLabel("0");
        orderStatsRow.add(createMiniStatCard("Out of Stock", statOutOfStock, ThemeColors.DANGER));
        statCompletedOrders = new JLabel("0");
        orderStatsRow.add(createMiniStatCard("Completed Orders", statCompletedOrders, ThemeColors.SUCCESS));
        statTotalOrderValue = new JLabel("₹0");
        orderStatsRow.add(createMiniStatCard("Total Orders", statTotalOrderValue, ThemeColors.PRIMARY));
        statTotalProfit = new JLabel("₹0");
        orderStatsRow.add(createMiniStatCard("Total Profit", statTotalProfit, ThemeColors.SUCCESS));
        statTotalLosses = new JLabel("₹0");
        orderStatsRow.add(createMiniStatCard("Total Losses", statTotalLosses, ThemeColors.DANGER));
        content.add(orderStatsRow);
        content.add(Box.createVerticalStrut(20));

        // ── Main area: orders table + side panel ──
        JPanel mainArea = new JPanel(new BorderLayout(16, 0));
        mainArea.setOpaque(false);
        mainArea.setAlignmentX(LEFT_ALIGNMENT);

        // Recent orders table
        JPanel ordersPanel = new JPanel(new BorderLayout(0, 12));
        ordersPanel.setOpaque(false);
        JLabel ordersTitle = new JLabel("Recent Orders");
        ordersTitle.setFont(ThemeColors.FONT_SECTION);
        ordersTitle.setForeground(ThemeColors.TEXT_PRIMARY);
        ordersPanel.add(ordersTitle, BorderLayout.NORTH);

        String[] columns = {"Order ID", "Customer/Supplier", "Type", "Order Date", "Quantity", "Amount", "Status"};
        ordersModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStyledTable(ordersModel);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        ordersPanel.add(scrollPane, BorderLayout.CENTER);
        mainArea.add(ordersPanel, BorderLayout.CENTER);

        // Right side panel
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(320, 0));
        sidePanel.setOpaque(false);

        sidePanel.add(createCriticalStockPanel());
        sidePanel.add(Box.createVerticalStrut(16));
        sidePanel.add(createExpiringPanel());

        mainArea.add(sidePanel, BorderLayout.EAST);
        content.add(mainArea);

        JScrollPane mainScroll = new JScrollPane(content);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        mainScroll.getViewport().setBackground(ThemeColors.BG_MAIN);
        add(mainScroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(this::loadData);
    }

    public void refreshData() {
        loadData();
    }

    private JPanel createStatCard(String label, JLabel valueLabel, Color accentColor, Icon icon) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(ThemeColors.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(ThemeColors.FONT_STAT_LABEL);
        lblLabel.setForeground(ThemeColors.TEXT_SECONDARY);

        valueLabel.setFont(ThemeColors.FONT_STAT_VALUE);
        valueLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        textPanel.add(lblLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(valueLabel);
        card.add(textPanel, BorderLayout.CENTER);

        if (icon != null) {
            JLabel iconLbl = new JLabel(icon);
            iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
            card.add(iconLbl, BorderLayout.EAST);
        }
        return card;
    }

    private JPanel createMiniStatCard(String label, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(ThemeColors.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(ThemeColors.FONT_STAT_LABEL);
        lblLabel.setForeground(accentColor);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(ThemeColors.TEXT_PRIMARY);

        textPanel.add(lblLabel);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(valueLabel);
        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCriticalStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(ThemeColors.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setMaximumSize(new Dimension(320, 300));

        JLabel title = new JLabel("  Critical Stock");
        title.setIcon(util.CustomIcons.getWarningIcon(20, ThemeColors.WARNING));
        title.setFont(ThemeColors.FONT_CARD_TITLE);
        title.setForeground(ThemeColors.WARNING);
        panel.add(title, BorderLayout.NORTH);

        criticalStockList = new JPanel();
        criticalStockList.setLayout(new BoxLayout(criticalStockList, BoxLayout.Y_AXIS));
        criticalStockList.setOpaque(false);

        JScrollPane sp = new JScrollPane(criticalStockList);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createExpiringPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(ThemeColors.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setMaximumSize(new Dimension(320, 300));

        JLabel title = new JLabel("  Expiring Soon");
        title.setIcon(util.CustomIcons.getClockIcon(20, ThemeColors.DANGER));
        title.setFont(ThemeColors.FONT_CARD_TITLE);
        title.setForeground(ThemeColors.DANGER);
        panel.add(title, BorderLayout.NORTH);

        expiringList = new JPanel();
        expiringList.setLayout(new BoxLayout(expiringList, BoxLayout.Y_AXIS));
        expiringList.setOpaque(false);

        JScrollPane sp = new JScrollPane(expiringList);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);

        return panel;
    }

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(ThemeColors.FONT_TABLE);
        table.setRowHeight(ThemeColors.TABLE_ROW_HEIGHT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(ThemeColors.PRIMARY_LIGHT);
        table.setSelectionForeground(ThemeColors.TEXT_PRIMARY);
        table.getTableHeader().setFont(ThemeColors.FONT_TABLE_HEADER);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(ThemeColors.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
                return c;
            }
        });
        return table;
    }

    private void loadData() {
        new javax.swing.SwingWorker<Void, Void>() {
            // Temporary holders for DB results
            String totalVal, itemsInStock, lowStockCnt, expiringSoonCnt, outOfStockCnt;
            String completedCnt, totalOrderVal, totalProfitVal, totalLossesVal;
            java.util.List<Object[]> recentOrders = new java.util.ArrayList<>();
            java.util.List<Object[]> criticalItems = new java.util.ArrayList<>();
            java.util.List<Object[]> expiringItems = new java.util.ArrayList<>();

            @Override protected Void doInBackground() {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();

                    ResultSet rs = db.executeQuery(
                        "SELECT COALESCE(SUM(i.quantity_available * b.selling_price), 0) as total " +
                        "FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id WHERE b.status = 'Active'");
                    totalVal = rs.next() ? String.format("₹%,.2f", rs.getDouble("total")) : "₹0";
                    rs.close();

                    rs = db.executeQuery(
                        "SELECT COALESCE(SUM(i.quantity_available), 0) as total " +
                        "FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id WHERE b.status = 'Active'");
                    itemsInStock = rs.next() ? String.format("%,d", rs.getInt("total")) : "0";
                    rs.close();

                    rs = db.executeQuery(
                        "SELECT COUNT(*) as cnt FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id " +
                        "WHERE b.status = 'Active' AND i.quantity_available < 500 AND i.quantity_available > 0");
                    lowStockCnt = rs.next() ? String.valueOf(rs.getInt("cnt")) : "0";
                    rs.close();

                    // Count medicines with at least one batch expiring within 90 days (per-medicine, not per-batch)
                    rs = db.executeQuery(
                        "SELECT COUNT(DISTINCT b.medicine_id) as cnt FROM Batch b " +
                        "WHERE b.status = 'Active' AND b.expiry_date < DATE_ADD(CURDATE(), INTERVAL 90 DAY) AND b.expiry_date >= CURDATE()");
                    expiringSoonCnt = rs.next() ? String.valueOf(rs.getInt("cnt")) : "0";
                    rs.close();

                    // Out of stock: medicines that either have no active batches, or all active batches have total qty = 0
                    rs = db.executeQuery(
                        "SELECT COUNT(*) as cnt FROM Medicine m WHERE (" +
                        "SELECT COALESCE(SUM(i.quantity_available), 0) FROM Batch b " +
                        "JOIN Inventory i ON b.batch_id = i.batch_id " +
                        "WHERE b.medicine_id = m.medicine_id AND b.status = 'Active' ) = 0");
                    outOfStockCnt = rs.next() ? String.valueOf(rs.getInt("cnt")) : "0";
                    rs.close();

                    rs = db.executeQuery(
                        "SELECT (SELECT COUNT(*) FROM Purchase_Order WHERE status = 'Completed') + " +
                        "(SELECT COUNT(*) FROM Sales_Order WHERE status = 'Completed') as completed");
                    completedCnt = rs.next() ? String.valueOf(rs.getInt("completed")) : "0";
                    rs.close();

                    rs = db.executeQuery(
                        "SELECT COALESCE((SELECT SUM(total_amount) FROM Purchase_Order), 0) + " +
                        "COALESCE((SELECT SUM(total_amount) FROM Sales_Order), 0) as total_val");
                    totalOrderVal = rs.next() ? String.format("₹%,.0f", rs.getDouble("total_val")) : "₹0";
                    rs.close();

                    // Total Profit: (selling_price - purchase_price) * quantity_sold from Sales_Order where status = 'Completed'
                    rs = db.executeQuery(
                        "SELECT COALESCE(SUM((s.selling_price - b.purchase_price) * s.quantity_sold), 0) as profit " +
                        "FROM Sales_Order s JOIN Batch b ON s.batch_id = b.batch_id WHERE s.status = 'Completed'");
                    totalProfitVal = rs.next() ? String.format("₹%,.0f", rs.getDouble("profit")) : "₹0";
                    rs.close();

                    // Total Losses: purchase_price * quantity_discarded from Discarded_Batch (price from Batch table)
                    rs = db.executeQuery("SELECT COALESCE(SUM(b.purchase_price * d.quantity_discarded), 0) as losses FROM Discarded_Batch d JOIN Batch b ON d.batch_id = b.batch_id");
                    totalLossesVal = rs.next() ? String.format("₹%,.0f", rs.getDouble("losses")) : "₹0";
                    rs.close();

                    // Recent orders
                    rs = db.executeQuery(
                        "SELECT s.sales_order_id, c.customer_name, 'Sale' as type, s.order_date, " +
                        "s.quantity_sold, s.total_amount, s.status " +
                        "FROM Sales_Order s JOIN Customer c ON s.customer_id = c.customer_id " +
                        "UNION ALL " +
                        "SELECT p.purchase_order_id, sup.supplier_name, 'Purchase' as type, p.order_date, " +
                        "p.quantity_purchased, p.total_amount, p.status " +
                        "FROM Purchase_Order p JOIN Supplier sup ON p.supplier_id = sup.supplier_id " +
                        "ORDER BY order_date DESC LIMIT 10");
                    while (rs.next()) {
                        recentOrders.add(new Object[]{
                            "#" + rs.getInt(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getInt(5),
                            String.format("₹%,.2f", rs.getDouble(6)), rs.getString(7)
                        });
                    }
                    rs.close();

                    // Critical stock
                    rs = db.executeQuery(
                        "SELECT m.medicine_name, b.batch_number, i.quantity_available " +
                        "FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id " +
                        "JOIN Medicine m ON b.medicine_id = m.medicine_id " +
                        "WHERE b.status = 'Active' AND i.quantity_available < 500 ORDER BY i.quantity_available ASC LIMIT 20");
                    while (rs.next()) {
                        criticalItems.add(new Object[]{rs.getString(1), rs.getString(2), rs.getInt(3)});
                    }
                    rs.close();

                    // Expiring soon — ONE ROW PER MEDICINE (nearest batch only)
                    rs = db.executeQuery(
                        "SELECT m.medicine_name, b.batch_number, MIN(b.expiry_date) as nearest_expiry, " +
                        "MIN(DATEDIFF(b.expiry_date, CURDATE())) as days_left " +
                        "FROM Batch b JOIN Medicine m ON b.medicine_id = m.medicine_id " +
                        "WHERE b.status = 'Active' AND b.expiry_date >= CURDATE() AND b.expiry_date < DATE_ADD(CURDATE(), INTERVAL 90 DAY) " +
                        "GROUP BY m.medicine_id, m.medicine_name, b.batch_number " +
                        "HAVING days_left = (SELECT MIN(DATEDIFF(b2.expiry_date, CURDATE())) " +
                        "FROM Batch b2 WHERE b2.medicine_id = m.medicine_id AND b2.status = 'Active' AND b2.expiry_date >= CURDATE()) " +
                        "ORDER BY nearest_expiry ASC LIMIT 20");
                    while (rs.next()) {
                        expiringItems.add(new Object[]{rs.getString(1), rs.getString(2), rs.getInt(4)});
                    }
                    rs.close();

                } catch (SQLException e) { e.printStackTrace(); }
                return null;
            }

            @Override protected void done() {
                statTotalValue.setText(totalVal);
                statItemsInStock.setText(itemsInStock);
                statLowStock.setText(lowStockCnt);
                statExpiringSoon.setText(expiringSoonCnt);
                statOutOfStock.setText(outOfStockCnt);
                statCompletedOrders.setText(completedCnt);
                statTotalOrderValue.setText(totalOrderVal);
                statTotalProfit.setText(totalProfitVal);
                statTotalLosses.setText(totalLossesVal);

                ordersModel.setRowCount(0);
                for (Object[] row : recentOrders) ordersModel.addRow(row);

                criticalStockList.removeAll();
                if (criticalItems.isEmpty()) {
                    JLabel noData = new JLabel("All stock levels healthy");
                    noData.setFont(ThemeColors.FONT_SMALL);
                    noData.setForeground(ThemeColors.SUCCESS);
                    criticalStockList.add(noData);
                } else {
                    for (Object[] item : criticalItems) {
                        String medName = (String) item[0];
                        String batchNum = (String) item[1];
                        int qty = (int) item[2];
                        JPanel row = new JPanel(new BorderLayout());
                        row.setOpaque(false);
                        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                        JLabel nameLabel = new JLabel(medName + " (" + batchNum + ")");
                        nameLabel.setFont(ThemeColors.FONT_SMALL);
                        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);
                        JLabel qtyLabel = new JLabel(qty + " units");
                        qtyLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                        qtyLabel.setForeground(qty < 100 ? ThemeColors.DANGER : ThemeColors.WARNING);
                        row.add(nameLabel, BorderLayout.CENTER);
                        row.add(qtyLabel, BorderLayout.EAST);
                        criticalStockList.add(row);
                    }
                }
                criticalStockList.revalidate();
                criticalStockList.repaint();

                expiringList.removeAll();
                if (expiringItems.isEmpty()) {
                    JLabel noData = new JLabel("No items expiring within 90 days");
                    noData.setFont(ThemeColors.FONT_SMALL);
                    noData.setForeground(ThemeColors.SUCCESS);
                    expiringList.add(noData);
                } else {
                    for (Object[] item : expiringItems) {
                        String medName = (String) item[0];
                        String batchNum = (String) item[1];
                        int daysLeft = (int) item[2];
                        JPanel row = new JPanel(new BorderLayout());
                        row.setOpaque(false);
                        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                        JLabel nameLabel = new JLabel(medName + " (" + batchNum + ")");
                        nameLabel.setFont(ThemeColors.FONT_SMALL);
                        nameLabel.setForeground(ThemeColors.TEXT_PRIMARY);
                        JLabel daysLabel = new JLabel(daysLeft + " days");
                        daysLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                        daysLabel.setForeground(daysLeft < 30 ? ThemeColors.DANGER : ThemeColors.ORANGE);
                        row.add(nameLabel, BorderLayout.CENTER);
                        row.add(daysLabel, BorderLayout.EAST);
                        expiringList.add(row);
                    }
                }
                expiringList.revalidate();
                expiringList.repaint();
            }
        }.execute();
    }
}
