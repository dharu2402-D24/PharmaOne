package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.*;

public class OrdersPanel extends JPanel {
    private JTable purchaseTable, salesTable;
    private DefaultTableModel purchaseModel, salesModel;
    private JLabel statTodayOrders, statCompleted, statTotalValue;
    private Runnable dashboardRefreshCallback;

    public OrdersPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeColors.BG_MAIN);
        buildUI();
    }

    public void setDashboardRefreshCallback(Runnable callback) {
        this.dashboardRefreshCallback = callback;
    }

    private void notifyDashboard() {
        if (dashboardRefreshCallback != null) {
            SwingUtilities.invokeLater(dashboardRefreshCallback);
        }
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 16));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Stat cards
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 100));
        statTodayOrders = new JLabel("0");
        statsRow.add(createStatCard("Today's Orders", statTodayOrders, ThemeColors.PRIMARY));
        statCompleted = new JLabel("0");
        statsRow.add(createStatCard("Completed", statCompleted, ThemeColors.SUCCESS));
        statTotalValue = new JLabel("₹0");
        statsRow.add(createStatCard("Total Value", statTotalValue, ThemeColors.PRIMARY));
        wrapper.add(statsRow, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(ThemeColors.FONT_BODY);

        // Purchase Orders tab
        JPanel purchasePanel = new JPanel(new BorderLayout(0, 8));
        purchasePanel.setOpaque(false);
        JPanel poToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        poToolbar.setOpaque(false);
        JButton newPO = createPrimaryButton("+ New Purchase Order");
        newPO.addActionListener(e -> showNewPurchaseOrderDialog());
        poToolbar.add(newPO);
        JButton exportPO = createSecondaryButton("Export CSV");
        exportPO.addActionListener(e -> exportCSV(purchaseModel, "purchase_orders"));
        poToolbar.add(exportPO);
        purchasePanel.add(poToolbar, BorderLayout.NORTH);

        String[] poCols = { "PO ID", "Supplier", "Batch", "Order Date", "Quantity", "Unit Cost", "Total", "Status" };
        purchaseModel = new DefaultTableModel(poCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        purchaseTable = createStyledTable(purchaseModel);
        TableRowSorter<DefaultTableModel> pSorter = new TableRowSorter<>(purchaseModel);
        purchaseTable.setRowSorter(pSorter);

        JPopupMenu poPopup = new JPopupMenu();
        JMenuItem poEdit = new JMenuItem("Edit Order");
        poEdit.addActionListener(e -> showEditPurchaseOrderDialog());
        poPopup.add(poEdit);
        purchaseTable.setComponentPopupMenu(poPopup);
        purchaseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    showEditPurchaseOrderDialog();
            }
        });

        purchasePanel.add(new JScrollPane(purchaseTable) {
            {
                setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
                getViewport().setBackground(Color.WHITE);
            }
        }, BorderLayout.CENTER);
        tabs.addTab("Purchase Orders", purchasePanel);

        // Sales Orders tab
        JPanel salesPanel = new JPanel(new BorderLayout(0, 8));
        salesPanel.setOpaque(false);
        JPanel soToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        soToolbar.setOpaque(false);
        JButton newSO = createPrimaryButton("+ New Sales Order");
        newSO.addActionListener(e -> showNewSalesOrderDialog());
        soToolbar.add(newSO);
        JButton exportSO = createSecondaryButton("Export CSV");
        exportSO.addActionListener(e -> exportCSV(salesModel, "sales_orders"));
        soToolbar.add(exportSO);
        salesPanel.add(soToolbar, BorderLayout.NORTH);

        String[] soCols = { "SO ID", "Customer", "Batch", "Order Date", "Quantity", "Price", "Total", "Status" };
        salesModel = new DefaultTableModel(soCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        salesTable = createStyledTable(salesModel);
        TableRowSorter<DefaultTableModel> sSorter = new TableRowSorter<>(salesModel);
        salesTable.setRowSorter(sSorter);

        JPopupMenu soPopup = new JPopupMenu();
        JMenuItem soEdit = new JMenuItem("Edit Order");
        soEdit.addActionListener(e -> showEditSalesOrderDialog());
        soPopup.add(soEdit);
        salesTable.setComponentPopupMenu(soPopup);
        salesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    showEditSalesOrderDialog();
            }
        });

        salesPanel.add(new JScrollPane(salesTable) {
            {
                setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
                getViewport().setBackground(Color.WHITE);
            }
        }, BorderLayout.CENTER);
        tabs.addTab("Sales Orders", salesPanel);

        wrapper.add(tabs, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::loadData);
    }

    public void refreshData() {
        loadData();
    }

    private void loadData() {
        new javax.swing.SwingWorker<Void, Void>() {
            java.util.List<Object[]> poRows = new java.util.ArrayList<>();
            java.util.List<Object[]> soRows = new java.util.ArrayList<>();
            String todayOrders, completed, totalVal;

            @Override
            protected Void doInBackground() {
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();

                    ResultSet rs = db.executeQuery(
                            "SELECT p.purchase_order_id, s.supplier_name, COALESCE(b.batch_number, 'N/A'), p.order_date, p.quantity_purchased, " +
                                    "p.unit_cost, p.total_amount, p.status " +
                                    "FROM Purchase_Order p JOIN Supplier s ON p.supplier_id = s.supplier_id " +
                                    "LEFT JOIN Batch b ON p.batch_id = b.batch_id ORDER BY p.order_date DESC");
                    while (rs.next()) {
                        poRows.add(new Object[] {
                                "#PO-" + rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                                rs.getInt(5), String.format("₹%.2f", rs.getDouble(6)),
                                String.format("₹%,.2f", rs.getDouble(7)), rs.getString(8)
                        });
                    }
                    rs.close();

                    rs = db.executeQuery(
                            "SELECT s.sales_order_id, c.customer_name, COALESCE(b.batch_number, 'N/A'), s.order_date, s.quantity_sold, " +
                                    "s.selling_price, s.total_amount, s.status " +
                                    "FROM Sales_Order s JOIN Customer c ON s.customer_id = c.customer_id " +
                                    "LEFT JOIN Batch b ON s.batch_id = b.batch_id ORDER BY s.order_date DESC");
                    while (rs.next()) {
                        soRows.add(new Object[] {
                                "#SO-" + rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                                rs.getInt(5), String.format("₹%.2f", rs.getDouble(6)),
                                String.format("₹%,.2f", rs.getDouble(7)), rs.getString(8)
                        });
                    }
                    rs.close();

                    // Today's orders count
                    rs = db.executeQuery(
                            "SELECT (SELECT COUNT(*) FROM Purchase_Order WHERE order_date = CURDATE()) + " +
                                    "(SELECT COUNT(*) FROM Sales_Order WHERE order_date = CURDATE()) as today_count");
                    todayOrders = rs.next() ? String.valueOf(rs.getInt("today_count")) : "0";
                    rs.close();

                    rs = db.executeQuery(
                            "SELECT (SELECT COUNT(*) FROM Purchase_Order WHERE status = 'Completed') + " +
                                    "(SELECT COUNT(*) FROM Sales_Order WHERE status = 'Completed') as completed");
                    completed = rs.next() ? String.valueOf(rs.getInt("completed")) : "0";
                    rs.close();

                    rs = db.executeQuery(
                            "SELECT COALESCE((SELECT SUM(total_amount) FROM Purchase_Order), 0) + " +
                                    "COALESCE((SELECT SUM(total_amount) FROM Sales_Order), 0) as total_val");
                    totalVal = rs.next() ? String.format("₹%,.0f", rs.getDouble("total_val")) : "₹0";
                    rs.close();

                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(OrdersPanel.this, "Error: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                purchaseModel.setRowCount(0);
                for (Object[] row : poRows)
                    purchaseModel.addRow(row);
                salesModel.setRowCount(0);
                for (Object[] row : soRows)
                    salesModel.addRow(row);
                statTodayOrders.setText(todayOrders);
                statCompleted.setText(completed);
                statTotalValue.setText(totalVal);
            }
        }.execute();
    }

    private void showNewPurchaseOrderDialog() {
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        JTextField idF = new JTextField();
        JTextField dateF = new JTextField(java.time.LocalDate.now().toString());
        JTextField qtyF = new JTextField();
        JTextField costF = new JTextField();
        JComboBox<String> supplierBox = new JComboBox<>();
        JComboBox<String> batchBox = new JComboBox<>();
        try {
            ResultSet rs = DatabaseConnection.getInstance()
                    .executeQuery("SELECT supplier_id, supplier_name FROM Supplier");
            while (rs.next())
                supplierBox.addItem(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
            rs = DatabaseConnection.getInstance().executeQuery("SELECT batch_id, batch_number FROM Batch WHERE status = 'Active'");
            while (rs.next())
                batchBox.addItem(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
        } catch (Exception ex) {
        }

        form.add(new JLabel("PO ID (Auto if blank):"));
        form.add(idF);
        form.add(new JLabel("Supplier:"));
        form.add(supplierBox);
        form.add(new JLabel("Batch:"));
        form.add(batchBox);
        form.add(new JLabel("Quantity:"));
        form.add(qtyF);
        form.add(new JLabel("Unit Cost:"));
        form.add(costF);

        if (JOptionPane.showConfirmDialog(this, form, "New Purchase Order",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                db.getConnection().setAutoCommit(false);

                int id;
                if (idF.getText().trim().isEmpty()) {
                    ResultSet rs = db
                            .executeQuery("SELECT COALESCE(MAX(purchase_order_id), 0) + 1 FROM Purchase_Order");
                    rs.next();
                    id = rs.getInt(1);
                    rs.close();
                } else {
                    id = Integer.parseInt(idF.getText().trim());
                }

                int suppId = Integer.parseInt(((String) supplierBox.getSelectedItem()).split(" - ")[0]);
                int batchId = Integer.parseInt(((String) batchBox.getSelectedItem()).split(" - ")[0]);
                int qty = Integer.parseInt(qtyF.getText().trim());
                double cost = Double.parseDouble(costF.getText().trim());

                PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO Purchase_Order (purchase_order_id, order_date, quantity_purchased, unit_cost, total_amount, status, supplier_id, batch_id) "
                                +
                                "VALUES (?, ?, ?, ?, ?, 'Completed', ?, ?)");
                ps.setInt(1, id);
                ps.setString(2, dateF.getText().trim());
                ps.setInt(3, qty);
                ps.setDouble(4, cost);
                ps.setDouble(5, qty * cost);
                ps.setInt(6, suppId);
                ps.setInt(7, batchId);
                ps.executeUpdate();

                // AUTO-UPDATE INVENTORY: Increase quantity for this batch
                db.executeUpdate(
                        "UPDATE Inventory SET quantity_available = quantity_available + " + qty +
                                ", last_updated = NOW() WHERE batch_id = " + batchId);

                db.getConnection().commit();
                db.getConnection().setAutoCommit(true);
                JOptionPane.showMessageDialog(this, "Purchase Order created! Inventory updated (+" + qty + " units)");
                loadData();
                notifyDashboard();
            } catch (Exception ex) {
                try {
                    DatabaseConnection.getInstance().getConnection().rollback();
                } catch (Exception ignored) {
                }
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showNewSalesOrderDialog() {
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        JTextField idF = new JTextField();
        JTextField qtyF = new JTextField();
        JTextField priceF = new JTextField();
        JComboBox<String> custBox = new JComboBox<>();
        JComboBox<String> batchBox = new JComboBox<>();
        // Store batch IDs and available quantities for validation
        java.util.List<Integer> batchIds = new java.util.ArrayList<>();
        java.util.List<Integer> batchAvailableQty = new java.util.ArrayList<>();
        try {
            ResultSet rs = DatabaseConnection.getInstance()
                    .executeQuery("SELECT customer_id, customer_name FROM Customer");
            while (rs.next())
                custBox.addItem(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
            // Populate batches — only non-expired with available stock
            rs = DatabaseConnection.getInstance().executeQuery(
                    "SELECT b.batch_id, b.batch_number, m.medicine_name, b.expiry_date, " +
                    "COALESCE(i.quantity_available, 0) as qty_available " +
                    "FROM Batch b " +
                    "JOIN Medicine m ON b.medicine_id = m.medicine_id " +
                    "LEFT JOIN Inventory i ON b.batch_id = i.batch_id " +
                    "WHERE b.status = 'Active' AND b.expiry_date >= CURDATE() " +
                    "AND COALESCE(i.quantity_available, 0) > 0 " +
                    "ORDER BY m.medicine_name, b.expiry_date ASC");
            while (rs.next()) {
                int bId = rs.getInt("batch_id");
                String bNum = rs.getString("batch_number");
                String medName = rs.getString("medicine_name");
                String expiry = rs.getString("expiry_date");
                int avail = rs.getInt("qty_available");
                batchIds.add(bId);
                batchAvailableQty.add(avail);
                batchBox.addItem(bId + " - " + medName + " | " + bNum + " | Exp: " + expiry + " | Qty: " + avail);
            }
            rs.close();
        } catch (Exception ex) {
        }

        form.add(new JLabel("SO ID (Auto if blank):"));
        form.add(idF);
        form.add(new JLabel("Customer:"));
        form.add(custBox);
        form.add(new JLabel("Batch:"));
        form.add(batchBox);
        form.add(new JLabel("Quantity:"));
        form.add(qtyF);
        form.add(new JLabel("Selling Price:"));
        form.add(priceF);

        if (JOptionPane.showConfirmDialog(this, form, "New Sales Order",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                if (batchBox.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(this,
                            "No non-expired batch with stock available.",
                            "No Stock", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                DatabaseConnection db = DatabaseConnection.getInstance();
                db.getConnection().setAutoCommit(false);

                int selectedIndex = batchBox.getSelectedIndex();
                int batchId = batchIds.get(selectedIndex);
                int available = batchAvailableQty.get(selectedIndex);
                int qty = Integer.parseInt(qtyF.getText().trim());

                if (qty > available) {
                    db.getConnection().setAutoCommit(true);
                    JOptionPane.showMessageDialog(this,
                            "Insufficient stock for this batch!\n" +
                                    "Available: " + available + ", Requested: " + qty,
                            "Stock Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int id;
                if (idF.getText().trim().isEmpty()) {
                    ResultSet rs = db.executeQuery("SELECT COALESCE(MAX(sales_order_id), 0) + 1 FROM Sales_Order");
                    rs.next();
                    id = rs.getInt(1);
                    rs.close();
                } else {
                    id = Integer.parseInt(idF.getText().trim());
                }

                int custId = Integer.parseInt(((String) custBox.getSelectedItem()).split(" - ")[0]);
                double price = Double.parseDouble(priceF.getText().trim());

                PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO Sales_Order (sales_order_id, order_date, quantity_sold, selling_price, total_amount, status, customer_id, batch_id) "
                                +
                                "VALUES (?, ?, ?, ?, ?, 'Completed', ?, ?)");
                ps.setInt(1, id);
                ps.setString(2, java.time.LocalDate.now().toString());
                ps.setInt(3, qty);
                ps.setDouble(4, price);
                ps.setDouble(5, qty * price);
                ps.setInt(6, custId);
                ps.setInt(7, batchId);
                ps.executeUpdate();

                // Deduct inventory for the selected batch
                db.executeUpdate(
                        "UPDATE Inventory SET quantity_available = quantity_available - " + qty +
                                ", last_updated = NOW() WHERE batch_id = " + batchId);

                db.getConnection().commit();
                db.getConnection().setAutoCommit(true);
                JOptionPane.showMessageDialog(this,
                        "Sales Order created!\nDeducted " + qty + " units from selected batch.");
                loadData();
                notifyDashboard();
            } catch (Exception ex) {
                try {
                    DatabaseConnection.getInstance().getConnection().rollback();
                } catch (Exception ignored) {
                }
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditPurchaseOrderDialog() {
        int row = purchaseTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a PO to edit");
            return;
        }
        row = purchaseTable.convertRowIndexToModel(row);
        int id = Integer.parseInt(((String) purchaseModel.getValueAt(row, 0)).replace("#PO-", ""));
        String dateStr = (String) purchaseModel.getValueAt(row, 3);
        int qty = (int) purchaseModel.getValueAt(row, 4);
        String costStr = ((String) purchaseModel.getValueAt(row, 5)).replace("₹", "").replace(",", "");
        String statusStr = (String) purchaseModel.getValueAt(row, 7);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField dateF = new JTextField(dateStr);
        JTextField qtyF = new JTextField(String.valueOf(qty));
        JTextField costF = new JTextField(costStr);
        JComboBox<String> statusBox = new JComboBox<>(new String[] { "Pending", "Completed", "Cancelled" });
        statusBox.setSelectedItem(statusStr);

        form.add(new JLabel("Order Date:"));
        form.add(dateF);
        form.add(new JLabel("Quantity:"));
        form.add(qtyF);
        form.add(new JLabel("Unit Cost:"));
        form.add(costF);
        form.add(new JLabel("Status:"));
        form.add(statusBox);

        if (JOptionPane.showConfirmDialog(this, form, "Edit PO #" + id,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String newDate = dateF.getText().trim();
                int newQty = Integer.parseInt(qtyF.getText().trim());
                double newCost = Double.parseDouble(costF.getText().trim());
                String newStatus = (String) statusBox.getSelectedItem();
                PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(
                        "UPDATE Purchase_Order SET order_date=?, quantity_purchased=?, unit_cost=?, total_amount=?, status=? WHERE purchase_order_id=?");
                ps.setString(1, newDate);
                ps.setInt(2, newQty);
                ps.setDouble(3, newCost);
                ps.setDouble(4, newQty * newCost);
                ps.setString(5, newStatus);
                ps.setInt(6, id);
                ps.executeUpdate();
                loadData();
                notifyDashboard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditSalesOrderDialog() {
        int row = salesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a SO to edit");
            return;
        }
        row = salesTable.convertRowIndexToModel(row);
        int id = Integer.parseInt(((String) salesModel.getValueAt(row, 0)).replace("#SO-", ""));
        String dateStr = (String) salesModel.getValueAt(row, 3);
        int qty = (int) salesModel.getValueAt(row, 4);
        String priceStr = ((String) salesModel.getValueAt(row, 5)).replace("₹", "").replace(",", "");
        String statusStr = (String) salesModel.getValueAt(row, 7);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField dateF = new JTextField(dateStr);
        JTextField qtyF = new JTextField(String.valueOf(qty));
        JTextField priceF = new JTextField(priceStr);
        JComboBox<String> statusBox = new JComboBox<>(new String[] { "Pending", "Completed", "Cancelled" });
        statusBox.setSelectedItem(statusStr);

        form.add(new JLabel("Order Date:"));
        form.add(dateF);
        form.add(new JLabel("Quantity:"));
        form.add(qtyF);
        form.add(new JLabel("Selling Price:"));
        form.add(priceF);
        form.add(new JLabel("Status:"));
        form.add(statusBox);

        if (JOptionPane.showConfirmDialog(this, form, "Edit SO #" + id,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String newDate = dateF.getText().trim();
                int newQty = Integer.parseInt(qtyF.getText().trim());
                double newPrice = Double.parseDouble(priceF.getText().trim());
                String newStatus = (String) statusBox.getSelectedItem();
                PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement(
                        "UPDATE Sales_Order SET order_date=?, quantity_sold=?, selling_price=?, total_amount=?, status=? WHERE sales_order_id=?");
                ps.setString(1, newDate);
                ps.setInt(2, newQty);
                ps.setDouble(3, newPrice);
                ps.setDouble(4, newQty * newPrice);
                ps.setString(5, newStatus);
                ps.setInt(6, id);
                ps.executeUpdate();
                loadData();
                notifyDashboard();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCSV(DefaultTableModel model, String filename) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(filename + "_export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), "UTF-8"))) {
                for (int i = 0; i < model.getColumnCount(); i++) {
                    if (i > 0)
                        pw.print(",");
                    pw.print("\"" + model.getColumnName(i) + "\"");
                }
                pw.println();
                for (int r = 0; r < model.getRowCount(); r++) {
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        if (c > 0)
                            pw.print(",");
                        Object val = model.getValueAt(r, c);
                        pw.print("\"" + (val != null ? val.toString().replace("\"", "\"\"") : "") + "\"");
                    }
                    pw.println();
                }
                JOptionPane.showMessageDialog(this, "Exported to " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createStatCard(String label, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
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
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JPanel tp = new JPanel();
        tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
        tp.setOpaque(false);
        JLabel l1 = new JLabel(label);
        l1.setFont(ThemeColors.FONT_STAT_LABEL);
        l1.setForeground(ThemeColors.TEXT_SECONDARY);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(ThemeColors.TEXT_PRIMARY);
        tp.add(l1);
        tp.add(Box.createVerticalStrut(4));
        tp.add(valueLabel);
        card.add(tp, BorderLayout.CENTER);
        return card;
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
                if (!s)
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return comp;
            }
        });
        return table;
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeColors.FONT_BUTTON);
        btn.setBackground(ThemeColors.PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(ThemeColors.FONT_BUTTON);
        btn.setBackground(Color.WHITE);
        btn.setForeground(ThemeColors.TEXT_PRIMARY);
        btn.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
