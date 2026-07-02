package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class DiscardedBatchesPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;

    public DiscardedBatchesPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeColors.BG_MAIN);
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 16));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(ThemeColors.FONT_BUTTON);
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setForeground(ThemeColors.TEXT_PRIMARY);
        refreshBtn.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadData());
        toolbar.add(refreshBtn);
        wrapper.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Discarded ID", "Batch#", "Medicine", "Mfg Date", "Expiry Date",
                "Purchase Price", "Selling Price", "Qty Discarded", "Reason", "Discarded Date"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = createStyledTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(7).setPreferredWidth(90);
        table.getColumnModel().getColumn(8).setPreferredWidth(90);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        // Summary footer
        JLabel footerLabel = new JLabel("Showing archived expired batches from Discarded_Batch table");
        footerLabel.setFont(ThemeColors.FONT_SMALL);
        footerLabel.setForeground(ThemeColors.TEXT_SECONDARY);
        footerLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        wrapper.add(footerLabel, BorderLayout.SOUTH);

        add(wrapper, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::loadData);
    }

    public void refreshData() {
        loadData();
    }

    private void loadData() {
        new SwingWorker<java.util.List<Object[]>, Void>() {
            @Override
            protected java.util.List<Object[]> doInBackground() {
                java.util.List<Object[]> rows = new java.util.ArrayList<>();
                try {
                    ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                        "SELECT d.discarded_id, b.batch_number, m.medicine_name, " +
                        "b.manufacture_date, b.expiry_date, b.purchase_price, b.selling_price, " +
                        "d.quantity_discarded, d.reason, d.discarded_date " +
                        "FROM Discarded_Batch d " +
                        "JOIN Batch b ON d.batch_id = b.batch_id " +
                        "JOIN Medicine m ON d.medicine_id = m.medicine_id " +
                        "ORDER BY d.discarded_date DESC");
                    while (rs.next()) {
                        rows.add(new Object[]{
                            rs.getInt("discarded_id"),
                            rs.getString("batch_number"),
                            rs.getString("medicine_name"),
                            rs.getString("manufacture_date"),
                            rs.getString("expiry_date"),
                            String.format("₹%.2f", rs.getDouble("purchase_price")),
                            String.format("₹%.2f", rs.getDouble("selling_price")),
                            rs.getInt("quantity_discarded"),
                            rs.getString("reason"),
                            rs.getString("discarded_date")
                        });
                    }
                    rs.close();
                } catch (SQLException e) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(DiscardedBatchesPanel.this,
                            "Error loading discarded batches: " + e.getMessage()));
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    java.util.List<Object[]> rows = get();
                    model.setRowCount(0);
                    for (Object[] row : rows) model.addRow(row);
                } catch (Exception ignored) {}
            }
        }.execute();
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
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return comp;
            }
        });
        return table;
    }
}
