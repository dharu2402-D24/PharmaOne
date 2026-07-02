package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class SQLConsolePanel extends JPanel {
    private JComboBox<String> queryBox;
    private JTextArea queryArea;
    private JTable resultTable;
    private DefaultTableModel resultModel;
    private JLabel statusLabel;

    private static final String[][] PRESET_QUERIES = {
        {"1. All Medicines", "SELECT * FROM Medicine"},
        {"2. Stock by Location (GROUP BY)", "SELECT storage_location, SUM(quantity_available) as Total_Stock FROM Inventory GROUP BY storage_location"},
        {"3. Antibiotics (WHERE)", "SELECT medicine_id, medicine_name, strength FROM Medicine WHERE category = 'Antibiotic'"},
        {"4. Medicine-Batch JOIN", "SELECT m.medicine_name, b.batch_number, b.expiry_date FROM Medicine m JOIN Batch b ON m.medicine_id = b.medicine_id"},
        {"5. 3-Table JOIN (Medicine+Batch+Inventory)", "SELECT m.medicine_name, i.quantity_available, i.storage_location FROM Medicine m JOIN Batch b ON m.medicine_id = b.medicine_id JOIN Inventory i ON b.batch_id = i.batch_id"},
        {"6. Stock > 1000 (HAVING)", "SELECT storage_location, SUM(quantity_available) as Total_Stock FROM Inventory GROUP BY storage_location HAVING SUM(quantity_available) > 1000"},
        {"7. Supplier Purchase History (4-Table JOIN)", "SELECT s.supplier_name, m.medicine_name, p.quantity_purchased FROM Supplier s JOIN Purchase_Order p ON s.supplier_id = p.supplier_id JOIN Batch b ON p.batch_id = b.batch_id JOIN Medicine m ON b.medicine_id = m.medicine_id"},
        {"8. Customer Sales History (4-Table JOIN)", "SELECT c.customer_name, m.medicine_name, so.quantity_sold FROM Customer c JOIN Sales_Order so ON c.customer_id = so.customer_id JOIN Batch b ON so.batch_id = b.batch_id JOIN Medicine m ON b.medicine_id = m.medicine_id"},
        {"9. Above-Average Price (Subquery)", "SELECT medicine_name, standard_price FROM Medicine WHERE standard_price > (SELECT AVG(standard_price) FROM Medicine)"},
        {"10. Expired Batches", "SELECT batch_number, expiry_date FROM Batch WHERE expiry_date < CURDATE()"},
        {"11. Stock Status (CASE)", "SELECT inventory_id, quantity_available, CASE WHEN quantity_available > 1000 THEN 'High Stock' WHEN quantity_available BETWEEN 500 AND 1000 THEN 'Medium Stock' ELSE 'Low Stock' END as stock_status FROM Inventory"},
        {"12. Total Revenue (Aggregate)", "SELECT SUM(quantity_sold * selling_price) as total_revenue FROM Sales_Order"},
        {"13. Profit Per Unit (Calculated)", "SELECT batch_number, (selling_price - purchase_price) as profit_per_unit FROM Batch"},
        {"14. Never Sold Medicines (NOT EXISTS)", "SELECT m.medicine_name FROM Medicine m WHERE NOT EXISTS (SELECT 1 FROM Batch b JOIN Sales_Order s ON b.batch_id = s.batch_id WHERE b.medicine_id = m.medicine_id)"},
        {"15. Customer Type Count (GROUP BY)", "SELECT customer_type, COUNT(*) as total_customers FROM Customer GROUP BY customer_type"}
    };

    public SQLConsolePanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(ThemeColors.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        // Top: query selector
        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setOpaque(false);

        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectorPanel.setOpaque(false);
        selectorPanel.add(new JLabel("Preset Queries:"));
        queryBox = new JComboBox<>();
        for (String[] q : PRESET_QUERIES) queryBox.addItem(q[0]);
        queryBox.addItem("-- Custom Query --");
        queryBox.setFont(ThemeColors.FONT_BODY);
        queryBox.setPreferredSize(new Dimension(400, ThemeColors.BUTTON_HEIGHT));
        queryBox.addActionListener(e -> {
            int idx = queryBox.getSelectedIndex();
            if (idx >= 0 && idx < PRESET_QUERIES.length) {
                queryArea.setText(PRESET_QUERIES[idx][1]);
            } else {
                queryArea.setText("");
                queryArea.requestFocus();
            }
        });
        selectorPanel.add(queryBox);
        topPanel.add(selectorPanel, BorderLayout.NORTH);

        // Query text area
        queryArea = new JTextArea(4, 60);
        queryArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        queryArea.setText(PRESET_QUERIES[0][1]);
        queryArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        JScrollPane queryScroll = new JScrollPane(queryArea);
        queryScroll.setPreferredSize(new Dimension(0, 100));
        topPanel.add(queryScroll, BorderLayout.CENTER);

        // Execute button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnPanel.setOpaque(false);
        JButton executeBtn = new JButton("\u25B6 Execute Query");
        executeBtn.setFont(ThemeColors.FONT_BUTTON);
        executeBtn.setBackground(ThemeColors.SUCCESS);
        executeBtn.setForeground(Color.WHITE);
        executeBtn.setFocusPainted(false);
        executeBtn.setBorderPainted(false);
        executeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        executeBtn.addActionListener(e -> executeQuery());
        btnPanel.add(executeBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.setFont(ThemeColors.FONT_BUTTON);
        clearBtn.setBackground(Color.WHITE);
        clearBtn.setForeground(ThemeColors.TEXT_PRIMARY);
        clearBtn.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> {
            queryArea.setText("");
            resultModel.setColumnCount(0);
            resultModel.setRowCount(0);
            statusLabel.setText("Ready");
        });
        btnPanel.add(clearBtn);
        topPanel.add(btnPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // Results table
        resultModel = new DefaultTableModel();
        resultTable = new JTable(resultModel);
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

        JScrollPane resultScroll = new JScrollPane(resultTable);
        resultScroll.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        resultScroll.getViewport().setBackground(Color.WHITE);
        add(resultScroll, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Ready - Select a query and click Execute");
        statusLabel.setFont(ThemeColors.FONT_SMALL);
        statusLabel.setForeground(ThemeColors.TEXT_SECONDARY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void executeQuery() {
        String sql = queryArea.getText().trim();
        if (sql.isEmpty()) {
            statusLabel.setText("Please enter a query");
            return;
        }

        try {
            long start = System.currentTimeMillis();
            DatabaseConnection db = DatabaseConnection.getInstance();

            // Determine if it's a SELECT or DML
            if (sql.toUpperCase().startsWith("SELECT") || sql.toUpperCase().startsWith("DESCRIBE") || sql.toUpperCase().startsWith("SHOW")) {
                ResultSet rs = db.executeQuery(sql);
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
                statusLabel.setText(String.format("\u2713 Query executed successfully | %d rows | %dms", rowCount, elapsed));
                statusLabel.setForeground(ThemeColors.SUCCESS);
            } else {
                int affected = db.executeUpdate(sql);
                long elapsed = System.currentTimeMillis() - start;
                statusLabel.setText(String.format("\u2713 Query executed | %d rows affected | %dms", affected, elapsed));
                statusLabel.setForeground(ThemeColors.SUCCESS);
            }
        } catch (SQLException ex) {
            statusLabel.setText("\u2717 Error: " + ex.getMessage());
            statusLabel.setForeground(ThemeColors.DANGER);
        }
    }
}
