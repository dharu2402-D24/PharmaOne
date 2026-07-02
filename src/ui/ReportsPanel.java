package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class ReportsPanel extends JPanel {

    public ReportsPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeColors.BG_MAIN);
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 16));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Report selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("Select Report:"));

        String[] reports = {
            "Stock by Storage Location",
            "Profit Per Unit (by Batch)",
            "Total Revenue Summary",
            "Customer Type Distribution",
            "Expired / Discarded Batches",
            "Supplier Purchase History",
        };
        JComboBox<String> reportBox = new JComboBox<>(reports);
        reportBox.setFont(ThemeColors.FONT_BODY);
        reportBox.setPreferredSize(new Dimension(300, ThemeColors.BUTTON_HEIGHT));
        topPanel.add(reportBox);

        JButton runBtn = new JButton("Generate Report");
        runBtn.setFont(ThemeColors.FONT_BUTTON);
        runBtn.setBackground(ThemeColors.PRIMARY);
        runBtn.setForeground(Color.WHITE);
        runBtn.setFocusPainted(false);
        runBtn.setBorderPainted(false);
        topPanel.add(runBtn);

        wrapper.add(topPanel, BorderLayout.NORTH);

        // Results area
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setOpaque(false);

        DefaultTableModel resultModel = new DefaultTableModel();
        JTable resultTable = new JTable(resultModel);
        resultTable.setFont(ThemeColors.FONT_TABLE);
        resultTable.setRowHeight(ThemeColors.TABLE_ROW_HEIGHT);
        resultTable.setShowGrid(false);
        resultTable.setSelectionBackground(ThemeColors.PRIMARY_LIGHT);
        resultTable.setSelectionForeground(ThemeColors.TEXT_PRIMARY);
        resultTable.getTableHeader().setFont(ThemeColors.FONT_TABLE_HEADER);
        resultTable.getTableHeader().setBackground(new Color(248, 250, 252));
        resultTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        resultTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return comp;
            }
        });

        JScrollPane scroll = new JScrollPane(resultTable);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        resultsPanel.add(scroll, BorderLayout.CENTER);

        JLabel statusLabel = new JLabel("Select a report and click 'Generate Report'");
        statusLabel.setFont(ThemeColors.FONT_SMALL);
        statusLabel.setForeground(ThemeColors.TEXT_SECONDARY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        resultsPanel.add(statusLabel, BorderLayout.SOUTH);

        wrapper.add(resultsPanel, BorderLayout.CENTER);

        runBtn.addActionListener(e -> {
            String selected = (String) reportBox.getSelectedItem();
            String sql = getQueryForReport(selected);
            if (sql == null) return;

            try {
                long start = System.currentTimeMillis();
                ResultSet rs = DatabaseConnection.getInstance().executeQuery(sql);
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                resultModel.setColumnCount(0);
                resultModel.setRowCount(0);
                for (int i = 1; i <= colCount; i++) {
                    resultModel.addColumn(meta.getColumnLabel(i));
                }
                int rowCount = 0;
                while (rs.next()) {
                    Object[] row = new Object[colCount];
                    for (int i = 0; i < colCount; i++) row[i] = rs.getObject(i + 1);
                    resultModel.addRow(row);
                    rowCount++;
                }
                rs.close();
                long elapsed = System.currentTimeMillis() - start;
                statusLabel.setText(String.format("Report: %s | %d rows returned | %dms", selected, rowCount, elapsed));
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Query Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(wrapper, BorderLayout.CENTER);
    }

    public void refreshData() {
        // Reports are generated on demand, no data to pre-load
    }

    private String getQueryForReport(String report) {
        return switch (report) {
            case "Stock by Storage Location" ->
                "SELECT storage_location, SUM(quantity_available) as Total_Stock FROM Inventory GROUP BY storage_location";
            case "Profit Per Unit (by Batch)" ->
                "SELECT batch_number, (selling_price - purchase_price) as profit_per_unit FROM Batch WHERE status = 'Active'";
            case "Total Revenue Summary" ->
                "SELECT SUM(quantity_sold * selling_price) as total_revenue FROM Sales_Order";
            case "Customer Type Distribution" ->
                "SELECT customer_type, COUNT(*) as total_customers FROM Customer GROUP BY customer_type";
            case "Expired / Discarded Batches" ->
                "SELECT b.batch_number, m.medicine_name, b.expiry_date, d.quantity_discarded, d.reason, d.discarded_date " +
                "FROM Discarded_Batch d JOIN Batch b ON d.batch_id = b.batch_id " +
                "JOIN Medicine m ON d.medicine_id = m.medicine_id ORDER BY d.discarded_date DESC";
            case "Supplier Purchase History" ->
                "SELECT s.supplier_name, m.medicine_name, p.quantity_purchased FROM Supplier s " +
                "JOIN Purchase_Order p ON s.supplier_id = p.supplier_id " +
                "JOIN Batch b ON p.batch_id = b.batch_id JOIN Medicine m ON b.medicine_id = m.medicine_id";
            default -> null;
        };
    }
}
