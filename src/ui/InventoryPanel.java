package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.*;


public class InventoryPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JComboBox<String> categoryFilter, stockFilter;

    public InventoryPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeColors.BG_MAIN);
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel mainContent = new JPanel(new BorderLayout(0, 16));
        mainContent.setOpaque(false);

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTools.setOpaque(false);
        JButton addBtn = createPrimaryButton("+ Add Medicine");
        addBtn.addActionListener(e -> showAddDialog());
        leftTools.add(addBtn);
        JButton batchBtn = createSecondaryButton("Manage Batches");
        batchBtn.addActionListener(e -> showManageBatchesDialog());
        leftTools.add(batchBtn);
        JButton exportBtn = createSecondaryButton("Export CSV");
        exportBtn.addActionListener(e -> exportCSV());
        leftTools.add(exportBtn);
        toolbar.add(leftTools, BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        categoryFilter = new JComboBox<>(new String[]{"All Categories", "Analgesic", "Antibiotic", "Vitamin C Supplement", "Vitamin E Supplement", "Antidepressant", "Erectile Dysfunction"});
        categoryFilter.setFont(ThemeColors.FONT_BODY);
        categoryFilter.addActionListener(e -> loadData());
        filters.add(categoryFilter);

        stockFilter = new JComboBox<>(new String[]{"All Stock", "High Stock", "Medium Stock", "Low Stock"});
        stockFilter.setFont(ThemeColors.FONT_BODY);
        stockFilter.addActionListener(e -> loadData());
        filters.add(stockFilter);

        JButton refreshBtn = createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadData());
        filters.add(refreshBtn);
        toolbar.add(filters, BorderLayout.EAST);
        mainContent.add(toolbar, BorderLayout.NORTH);

        // Table columns — includes hidden batch_count (12) and expiry_status (13)
        String[] columns = {"Med ID", "Medicine Name", "Category", "Dosage Form", "Strength",
                "Batch#", "Mfg Date", "Expiry Date", "Qty", "Location", "Price", "Actions",
                "_BatchCount", "_ExpiryStatus"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 11; }
        };
        table = createStyledTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);
        table.getColumnModel().getColumn(8).setPreferredWidth(60);
        table.getColumnModel().getColumn(11).setPreferredWidth(80);

        // Hide the batch_count and expiry_status columns (data-only)
        table.getColumnModel().getColumn(13).setMinWidth(0);
        table.getColumnModel().getColumn(13).setMaxWidth(0);
        table.getColumnModel().getColumn(13).setPreferredWidth(0);
        table.getColumnModel().getColumn(12).setMinWidth(0);
        table.getColumnModel().getColumn(12).setMaxWidth(0);
        table.getColumnModel().getColumn(12).setPreferredWidth(0);

        // Sorting — FEFO per medicine: group by medicine name, then expiry date ASC within each
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        java.util.List<RowSorter.SortKey> sortKeys = new java.util.ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));  // Medicine Name
        sortKeys.add(new RowSorter.SortKey(7, SortOrder.ASCENDING));  // Expiry Date (FEFO)
        sorter.setSortKeys(sortKeys);
        table.setRowSorter(sorter);

        // Action column
        table.getColumnModel().getColumn(11).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(11).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Context menu
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Edit Record");
        editItem.addActionListener(e -> editSelectedRow());
        JMenuItem deleteItem = new JMenuItem("Delete Record");
        deleteItem.addActionListener(e -> deleteSelectedRow());
        popup.add(editItem);
        popup.add(deleteItem);
        table.setComponentPopupMenu(popup);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedRow();
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainContent.add(scrollPane, BorderLayout.CENTER);

        // Pagination
        JPanel pagPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pagPanel.setOpaque(false);
        pagPanel.add(new JLabel("Showing all records"));
        mainContent.add(pagPanel, BorderLayout.SOUTH);

        wrapper.add(mainContent, BorderLayout.CENTER);

        // Side panel
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(200, 0));
        sidePanel.setOpaque(false);
        sidePanel.add(createValuationCard());
        wrapper.add(sidePanel, BorderLayout.EAST);

        add(wrapper, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::loadData);
    }

    public void refreshData() {
        loadData();
    }

    private void loadData() {
        new javax.swing.SwingWorker<java.util.List<Object[]>, Void>() {
            @Override protected java.util.List<Object[]> doInBackground() {
                java.util.List<Object[]> rows = new java.util.ArrayList<>();
                try {
                    DatabaseConnection db = DatabaseConnection.getInstance();
                    // Data-driven query with batch_count and expiry_status
                    String sql = "SELECT m.medicine_id, m.medicine_name, m.category, m.dosage_form, m.strength, " +
                        "COALESCE(b.batch_number, 'N/A') as batch_number, " +
                        "b.manufacture_date, b.expiry_date, " +
                        "COALESCE(i.quantity_available, 0) as quantity_available, " +
                        "COALESCE(i.storage_location, 'Unassigned') as storage_location, " +
                        "COALESCE(b.selling_price, m.standard_price, 0) as selling_price, " +
                        "(SELECT COUNT(*) FROM Batch b2 WHERE b2.medicine_id = m.medicine_id AND b2.status = 'Active') as batch_count, " +
                        "CASE " +
                        "  WHEN b.expiry_date IS NULL THEN 'Normal' " +
                        "  WHEN b.expiry_date < CURDATE() THEN 'Expired' " +
                        "  WHEN b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 'Critical' " +
                        "  WHEN b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY) THEN 'Near Expiry' " +
                        "  ELSE 'Normal' " +
                        "END as expiry_status " +
                        "FROM Medicine m " +
                        "LEFT JOIN Batch b ON m.medicine_id = b.medicine_id AND b.status = 'Active' " +
                        "LEFT JOIN Inventory i ON b.batch_id = i.batch_id";

                    String cat = (String) categoryFilter.getSelectedItem();
                    if (cat != null && !cat.equals("All Categories")) {
                        sql += " WHERE m.category = '" + cat.replace("'", "''") + "'";
                    }

                    String stock = (String) stockFilter.getSelectedItem();
                    if (stock != null && !stock.equals("All Stock")) {
                        String cond = switch (stock) {
                            case "High Stock" -> "COALESCE(i.quantity_available, 0) > 1000";
                            case "Medium Stock" -> "COALESCE(i.quantity_available, 0) BETWEEN 500 AND 1000";
                            case "Low Stock" -> "COALESCE(i.quantity_available, 0) < 500";
                            default -> "1=1";
                        };
                        sql += (sql.contains("WHERE") ? " AND " : " WHERE ") + cond;
                    }

                    // Group by medicine, then expiry date ASC (FEFO within each medicine)
                    sql += " ORDER BY m.medicine_id, b.expiry_date ASC";

                    ResultSet rs = db.executeQuery(sql);
                    while (rs.next()) {
                        rows.add(new Object[]{
                            rs.getInt("medicine_id"),
                            rs.getString("medicine_name"),
                            rs.getString("category"),
                            rs.getString("dosage_form"),
                            rs.getString("strength"),
                            rs.getString("batch_number"),
                            rs.getString("manufacture_date"),
                            rs.getString("expiry_date"),
                            rs.getInt("quantity_available"),
                            rs.getString("storage_location"),
                            String.format("₹%.2f", rs.getDouble("selling_price")),
                            "Edit",
                            rs.getInt("batch_count"),
                            rs.getString("expiry_status")
                        });
                    }
                    rs.close();
                } catch (SQLException e) {
                    javax.swing.SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(InventoryPanel.this, "Error loading data: " + e.getMessage()));
                }
                return rows;
            }

            @Override protected void done() {
                try {
                    java.util.List<Object[]> rows = get();
                    model.setRowCount(0);
                    for (Object[] row : rows) model.addRow(row);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void showAddDialog() {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField idF = new JTextField(); JTextField nameF = new JTextField();
        JTextField catF = new JTextField(); JTextField dosageF = new JTextField();
        JTextField strengthF = new JTextField(); JTextField priceF = new JTextField();
        JTextField descF = new JTextField();

        JTextField batchF = new JTextField(); JTextField expiryF = new JTextField("YYYY-MM-DD");
        JTextField qtyF = new JTextField("0"); JTextField locF = new JTextField();
        JComboBox<String> supplierBox = new JComboBox<>();
        supplierBox.addItem("-- None --");
        try {
            ResultSet rsSup = DatabaseConnection.getInstance().executeQuery("SELECT supplier_id, supplier_name FROM Supplier ORDER BY supplier_id");
            while (rsSup.next()) supplierBox.addItem(rsSup.getInt(1) + " - " + rsSup.getString(2));
            rsSup.close();
        } catch (Exception ex) {}

        form.add(new JLabel("Medicine ID (Auto if blank):")); form.add(idF);
        form.add(new JLabel("Name* :")); form.add(nameF);
        form.add(new JLabel("Category* :")); form.add(catF);
        form.add(new JLabel("Dosage Form:")); form.add(dosageF);
        form.add(new JLabel("Strength:")); form.add(strengthF);
        form.add(new JLabel("Standard Price* :")); form.add(priceF);
        form.add(new JLabel("Description:")); form.add(descF);
        form.add(new JLabel("Batch Number (Optional):")); form.add(batchF);
        form.add(new JLabel("Expiry Date (Optional):")); form.add(expiryF);
        form.add(new JLabel("Supplier (for Batch):")); form.add(supplierBox);
        form.add(new JLabel("Initial Qty:")); form.add(qtyF);
        form.add(new JLabel("Storage Loc:")); form.add(locF);

        int result = JOptionPane.showConfirmDialog(this, form, "Add New Medicine & Initial Stock", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                db.getConnection().setAutoCommit(false);

                int medId;
                if (idF.getText().trim().isEmpty()) {
                    ResultSet rs = db.executeQuery("SELECT COALESCE(MAX(medicine_id), 0) + 1 FROM Medicine");
                    rs.next(); medId = rs.getInt(1); rs.close();
                } else {
                    medId = Integer.parseInt(idF.getText().trim());
                }

                PreparedStatement psMed = db.prepareStatement(
                    "INSERT INTO Medicine (medicine_id, medicine_name, category, dosage_form, strength, standard_price, description) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)");
                psMed.setInt(1, medId);
                psMed.setString(2, nameF.getText().trim());
                psMed.setString(3, catF.getText().trim());
                psMed.setString(4, dosageF.getText().trim());
                psMed.setString(5, strengthF.getText().trim());
                psMed.setDouble(6, Double.parseDouble(priceF.getText().trim()));
                psMed.setString(7, descF.getText().trim());
                psMed.executeUpdate();

                String batchText = batchF.getText().trim();
                if (!batchText.isEmpty()) {
                    ResultSet rsBatch = db.executeQuery("SELECT COALESCE(MAX(batch_id), 0) + 1 FROM Batch");
                    rsBatch.next(); int batchId = rsBatch.getInt(1); rsBatch.close();

                    String suppSel = (String) supplierBox.getSelectedItem();
                    Integer suppId = null;
                    if (suppSel != null && !suppSel.equals("-- None --")) {
                        suppId = Integer.parseInt(suppSel.split(" - ")[0]);
                    }

                    PreparedStatement psBatch = db.prepareStatement(
                        "INSERT INTO Batch (batch_id, batch_number, manufacture_date, expiry_date, purchase_price, selling_price, medicine_id, supplier_id) " +
                        "VALUES (?, ?, CURDATE(), ?, ?, ?, ?, ?)");
                    psBatch.setInt(1, batchId);
                    psBatch.setString(2, batchText);
                    psBatch.setString(3, expiryF.getText().trim().equals("YYYY-MM-DD") ? null : expiryF.getText().trim());
                    double stdPrice = Double.parseDouble(priceF.getText().trim());
                    psBatch.setDouble(4, stdPrice * 0.7);
                    psBatch.setDouble(5, stdPrice);
                    psBatch.setInt(6, medId);
                    if (suppId != null) psBatch.setInt(7, suppId);
                    else psBatch.setNull(7, java.sql.Types.INTEGER);
                    psBatch.executeUpdate();

                    ResultSet rsInvId = db.executeQuery("SELECT COALESCE(MAX(inventory_id), 0) + 1 FROM Inventory");
                    rsInvId.next(); int invId = rsInvId.getInt(1); rsInvId.close();
                    PreparedStatement psInv = db.prepareStatement(
                        "INSERT INTO Inventory (inventory_id, quantity_available, storage_location, batch_id) VALUES (?, ?, ?, ?)");
                    psInv.setInt(1, invId);
                    psInv.setInt(2, Integer.parseInt(qtyF.getText().trim()));
                    psInv.setString(3, locF.getText().trim());
                    psInv.setInt(4, batchId);
                    psInv.executeUpdate();
                }

                db.getConnection().commit();
                db.getConnection().setAutoCommit(true);
                JOptionPane.showMessageDialog(this, "Medicine/Stock added successfully!");
                loadData();
            } catch (Exception ex) {
                try { DatabaseConnection.getInstance().getConnection().rollback(); } catch (Exception ignored) {}
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showManageBatchesDialog() {
        // Select a medicine first
        JComboBox<String> medBox = new JComboBox<>();
        try {
            ResultSet rs = DatabaseConnection.getInstance().executeQuery("SELECT medicine_id, medicine_name FROM Medicine ORDER BY medicine_id");
            while (rs.next()) medBox.addItem(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
        } catch (Exception e) {}

        JPanel selectPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        selectPanel.add(new JLabel("Select Medicine:"));
        selectPanel.add(medBox);

        if (JOptionPane.showConfirmDialog(this, selectPanel, "Manage Batches", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        if (medBox.getSelectedItem() == null) return;

        int medId = Integer.parseInt(((String) medBox.getSelectedItem()).split(" - ")[0]);
        String medName = ((String) medBox.getSelectedItem()).split(" - ", 2)[1];

        // Show existing batches and allow add/edit
        JPanel form = new JPanel(new BorderLayout(0, 12));

        // Existing batches list
        DefaultTableModel batchModel = new DefaultTableModel(new String[]{"Batch ID", "Batch#", "Supplier", "Mfg Date", "Expiry", "Purchase Price", "Selling Price", "Qty", "Location"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable batchTable = new JTable(batchModel);
        batchTable.setFont(ThemeColors.FONT_TABLE);
        batchTable.setRowHeight(32);

        try {
            ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                "SELECT b.batch_id, b.batch_number, COALESCE(s.supplier_name, 'N/A'), b.manufacture_date, b.expiry_date, " +
                "b.purchase_price, b.selling_price, COALESCE(i.quantity_available, 0), " +
                "COALESCE(i.storage_location, 'N/A') " +
                "FROM Batch b LEFT JOIN Inventory i ON b.batch_id = i.batch_id " +
                "LEFT JOIN Supplier s ON b.supplier_id = s.supplier_id " +
                "WHERE b.medicine_id = " + medId + " AND b.status = 'Active' ORDER BY b.expiry_date ASC");
            while (rs.next()) {
                batchModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getDouble(6), rs.getDouble(7), rs.getInt(8), rs.getString(9)});
            }
            rs.close();
        } catch (Exception e) {}

        JScrollPane sp = new JScrollPane(batchTable);
        sp.setPreferredSize(new Dimension(700, 200));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton addBatchBtn = new JButton("+ Add New Batch");
        addBatchBtn.setFont(ThemeColors.FONT_BUTTON);
        addBatchBtn.setBackground(ThemeColors.PRIMARY);
        addBatchBtn.setForeground(Color.WHITE);
        addBatchBtn.setFocusPainted(false); addBatchBtn.setBorderPainted(false);
        addBatchBtn.addActionListener(e -> {
            addNewBatchForMedicine(medId);
            // Refresh batch table
            batchModel.setRowCount(0);
            try {
                ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                    "SELECT b.batch_id, b.batch_number, COALESCE(s.supplier_name, 'N/A'), b.manufacture_date, b.expiry_date, " +
                    "b.purchase_price, b.selling_price, COALESCE(i.quantity_available, 0), " +
                    "COALESCE(i.storage_location, 'N/A') " +
                    "FROM Batch b LEFT JOIN Inventory i ON b.batch_id = i.batch_id " +
                    "LEFT JOIN Supplier s ON b.supplier_id = s.supplier_id " +
                    "WHERE b.medicine_id = " + medId + " AND b.status = 'Active' ORDER BY b.expiry_date ASC");
                while (rs.next()) {
                    batchModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getDouble(6), rs.getDouble(7), rs.getInt(8), rs.getString(9)});
                }
                rs.close();
            } catch (Exception ex) {}
        });
        btnPanel.add(addBatchBtn);

        JButton editBatchBtn = new JButton("Edit Selected Batch");
        editBatchBtn.setFont(ThemeColors.FONT_BUTTON);
        editBatchBtn.setBackground(Color.WHITE);
        editBatchBtn.setForeground(ThemeColors.TEXT_PRIMARY);
        editBatchBtn.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        editBatchBtn.setFocusPainted(false);
        editBatchBtn.addActionListener(e -> {
            int row = batchTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a batch to edit"); return; }
            int batchId = (int) batchModel.getValueAt(row, 0);
            editBatch(batchId);
            // Refresh
            batchModel.setRowCount(0);
            try {
                ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                    "SELECT b.batch_id, b.batch_number, COALESCE(s.supplier_name, 'N/A'), b.manufacture_date, b.expiry_date, " +
                    "b.purchase_price, b.selling_price, COALESCE(i.quantity_available, 0), " +
                    "COALESCE(i.storage_location, 'N/A') " +
                    "FROM Batch b LEFT JOIN Inventory i ON b.batch_id = i.batch_id " +
                    "LEFT JOIN Supplier s ON b.supplier_id = s.supplier_id " +
                    "WHERE b.medicine_id = " + medId + " AND b.status = 'Active' ORDER BY b.expiry_date ASC");
                while (rs.next()) {
                    batchModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getDouble(6), rs.getDouble(7), rs.getInt(8), rs.getString(9)});
                }
                rs.close();
            } catch (Exception ex) {}
        });
        btnPanel.add(editBatchBtn);

        form.add(new JLabel("Batches for: " + medName), BorderLayout.NORTH);
        form.add(sp, BorderLayout.CENTER);
        form.add(btnPanel, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(this, form, "Manage Batches - " + medName, JOptionPane.PLAIN_MESSAGE);
        loadData();
    }

    private void addNewBatchForMedicine(int medId) {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField batchNumF = new JTextField();
        JTextField expiryF = new JTextField("YYYY-MM-DD");
        JTextField purchaseF = new JTextField();
        JTextField sellingF = new JTextField();
        JTextField qtyF = new JTextField("0");
        JTextField locF = new JTextField();
        JComboBox<String> supplierBox = new JComboBox<>();
        supplierBox.addItem("-- None --");
        try {
            ResultSet rs = DatabaseConnection.getInstance().executeQuery("SELECT supplier_id, supplier_name FROM Supplier ORDER BY supplier_id");
            while (rs.next()) supplierBox.addItem(rs.getInt(1) + " - " + rs.getString(2));
            rs.close();
        } catch (Exception e) {}

        form.add(new JLabel("Batch Number*:")); form.add(batchNumF);
        form.add(new JLabel("Expiry Date*:")); form.add(expiryF);
        form.add(new JLabel("Purchase Price:")); form.add(purchaseF);
        form.add(new JLabel("Selling Price:")); form.add(sellingF);
        form.add(new JLabel("Supplier:")); form.add(supplierBox);
        form.add(new JLabel("Quantity:")); form.add(qtyF);
        form.add(new JLabel("Storage Location:")); form.add(locF);

        if (JOptionPane.showConfirmDialog(this, form, "Add New Batch", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                db.getConnection().setAutoCommit(false);

                ResultSet rsBatch = db.executeQuery("SELECT COALESCE(MAX(batch_id), 0) + 1 FROM Batch");
                rsBatch.next(); int batchId = rsBatch.getInt(1); rsBatch.close();

                String suppSel = (String) supplierBox.getSelectedItem();
                Integer suppId = null;
                if (suppSel != null && !suppSel.equals("-- None --")) suppId = Integer.parseInt(suppSel.split(" - ")[0]);

                PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO Batch (batch_id, batch_number, manufacture_date, expiry_date, purchase_price, selling_price, medicine_id, supplier_id) " +
                    "VALUES (?, ?, CURDATE(), ?, ?, ?, ?, ?)");
                ps.setInt(1, batchId);
                ps.setString(2, batchNumF.getText().trim());
                ps.setString(3, expiryF.getText().trim().equals("YYYY-MM-DD") ? null : expiryF.getText().trim());
                ps.setDouble(4, Double.parseDouble(purchaseF.getText().trim()));
                ps.setDouble(5, Double.parseDouble(sellingF.getText().trim()));
                ps.setInt(6, medId);
                if (suppId != null) ps.setInt(7, suppId);
                else ps.setNull(7, java.sql.Types.INTEGER);
                ps.executeUpdate();

                // Create inventory record
                ResultSet rsInvId = db.executeQuery("SELECT COALESCE(MAX(inventory_id), 0) + 1 FROM Inventory");
                rsInvId.next(); int invId = rsInvId.getInt(1); rsInvId.close();
                PreparedStatement psInv = db.prepareStatement(
                    "INSERT INTO Inventory (inventory_id, quantity_available, storage_location, batch_id) VALUES (?, ?, ?, ?)");
                psInv.setInt(1, invId);
                psInv.setInt(2, Integer.parseInt(qtyF.getText().trim()));
                psInv.setString(3, locF.getText().trim());
                psInv.setInt(4, batchId);
                psInv.executeUpdate();

                db.getConnection().commit();
                db.getConnection().setAutoCommit(true);
                JOptionPane.showMessageDialog(this, "Batch added!");
            } catch (Exception ex) {
                try { DatabaseConnection.getInstance().getConnection().rollback(); } catch (Exception ignored) {}
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editBatch(int batchId) {
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery(
                "SELECT b.batch_number, b.expiry_date, b.purchase_price, b.selling_price, " +
                "COALESCE(i.quantity_available, 0), COALESCE(i.storage_location, ''), b.supplier_id " +
                "FROM Batch b LEFT JOIN Inventory i ON b.batch_id = i.batch_id WHERE b.batch_id = " + batchId);
            if (!rs.next()) { rs.close(); return; }

            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            JTextField batchNumF = new JTextField(rs.getString(1));
            JTextField expiryF = new JTextField(rs.getString(2) != null ? rs.getString(2) : "");
            JTextField purchaseF = new JTextField(String.valueOf(rs.getDouble(3)));
            JTextField sellingF = new JTextField(String.valueOf(rs.getDouble(4)));
            JTextField qtyF = new JTextField(String.valueOf(rs.getInt(5)));
            JTextField locF = new JTextField(rs.getString(6));
            int currentSuppId = rs.getInt(7);
            boolean hasSupplierId = !rs.wasNull();
            rs.close();

            JComboBox<String> supplierBox = new JComboBox<>();
            supplierBox.addItem("-- None --");
            try {
                ResultSet rsSupp = db.executeQuery("SELECT supplier_id, supplier_name FROM Supplier ORDER BY supplier_id");
                while (rsSupp.next()) {
                    int sid = rsSupp.getInt(1);
                    String item = sid + " - " + rsSupp.getString(2);
                    supplierBox.addItem(item);
                    if (hasSupplierId && sid == currentSuppId) supplierBox.setSelectedItem(item);
                }
                rsSupp.close();
            } catch (Exception e) {}

            form.add(new JLabel("Batch Number:")); form.add(batchNumF);
            form.add(new JLabel("Supplier:")); form.add(supplierBox);
            form.add(new JLabel("Expiry Date:")); form.add(expiryF);
            form.add(new JLabel("Purchase Price:")); form.add(purchaseF);
            form.add(new JLabel("Selling Price:")); form.add(sellingF);
            form.add(new JLabel("Quantity:")); form.add(qtyF);
            form.add(new JLabel("Storage Location:")); form.add(locF);

            if (JOptionPane.showConfirmDialog(this, form, "Edit Batch #" + batchId, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                db.getConnection().setAutoCommit(false);
                
                String suppSel = (String) supplierBox.getSelectedItem();
                Integer newSuppId = null;
                if (suppSel != null && !suppSel.equals("-- None --")) newSuppId = Integer.parseInt(suppSel.split(" - ")[0]);

                PreparedStatement psB = db.prepareStatement(
                    "UPDATE Batch SET batch_number=?, expiry_date=?, purchase_price=?, selling_price=?, supplier_id=? WHERE batch_id=?");
                psB.setString(1, batchNumF.getText().trim());
                psB.setString(2, expiryF.getText().trim());
                psB.setDouble(3, Double.parseDouble(purchaseF.getText().trim()));
                psB.setDouble(4, Double.parseDouble(sellingF.getText().trim()));
                if (newSuppId != null) psB.setInt(5, newSuppId);
                else psB.setNull(5, java.sql.Types.INTEGER);
                psB.setInt(6, batchId);
                psB.executeUpdate();

                // Update or insert inventory
                ResultSet rsChk = db.executeQuery("SELECT inventory_id FROM Inventory WHERE batch_id = " + batchId);
                if (rsChk.next()) {
                    PreparedStatement psI = db.prepareStatement("UPDATE Inventory SET quantity_available=?, storage_location=? WHERE batch_id=?");
                    psI.setInt(1, Integer.parseInt(qtyF.getText().trim()));
                    psI.setString(2, locF.getText().trim());
                    psI.setInt(3, batchId);
                    psI.executeUpdate();
                } else {
                    ResultSet rsInvId = db.executeQuery("SELECT COALESCE(MAX(inventory_id), 0) + 1 FROM Inventory");
                    rsInvId.next(); int invId = rsInvId.getInt(1); rsInvId.close();
                    PreparedStatement psI = db.prepareStatement("INSERT INTO Inventory (inventory_id, quantity_available, storage_location, batch_id) VALUES (?, ?, ?, ?)");
                    psI.setInt(1, invId);
                    psI.setInt(2, Integer.parseInt(qtyF.getText().trim()));
                    psI.setString(3, locF.getText().trim());
                    psI.setInt(4, batchId);
                    psI.executeUpdate();
                }
                rsChk.close();
                db.getConnection().commit();
                db.getConnection().setAutoCommit(true);
                JOptionPane.showMessageDialog(this, "Batch updated!");
            }
        } catch (Exception ex) {
            try { DatabaseConnection.getInstance().getConnection().rollback(); } catch (Exception ignored) {}
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        row = table.convertRowIndexToModel(row);
        int medId = (int) model.getValueAt(row, 0);
        String name = (String) model.getValueAt(row, 1);
        String cat = (String) model.getValueAt(row, 2);
        String dosageStr = (String) model.getValueAt(row, 3);
        String strStr = (String) model.getValueAt(row, 4);
        String batchStr = (String) model.getValueAt(row, 5);
        String expiryStr = (String) model.getValueAt(row, 7);
        int qty = (int) model.getValueAt(row, 8);
        String locStr = (String) model.getValueAt(row, 9);
        String priceStr = ((String) model.getValueAt(row, 10)).replace("₹", "").replace(",", "");

        String descStr = "";
        try {
            ResultSet rs = DatabaseConnection.getInstance().executeQuery("SELECT description FROM Medicine WHERE medicine_id=" + medId);
            if (rs.next()) descStr = rs.getString(1);
            rs.close();
        } catch (Exception e) {}

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        JTextField nameF = new JTextField(name);
        JTextField catF = new JTextField(cat);
        JTextField dosageF = new JTextField(dosageStr);
        JTextField strengthF = new JTextField(strStr);
        JTextField descF = new JTextField(descStr == null ? "" : descStr);
        JTextField batchF = new JTextField(batchStr.equals("N/A") ? "" : batchStr);
        JTextField expiryF = new JTextField(expiryStr == null ? "" : expiryStr);
        JTextField qtyF = new JTextField(String.valueOf(qty));
        JTextField locF = new JTextField(locStr.equals("Unassigned") ? "" : locStr);
        JTextField priceF = new JTextField(priceStr);

        form.add(new JLabel("Medicine ID M-" + medId)); form.add(new JLabel(""));
        form.add(new JLabel("Name:")); form.add(nameF);
        form.add(new JLabel("Category:")); form.add(catF);
        form.add(new JLabel("Dosage Form:")); form.add(dosageF);
        form.add(new JLabel("Strength:")); form.add(strengthF);
        form.add(new JLabel("Description:")); form.add(descF);
        form.add(new JLabel("Batch Number:")); form.add(batchF);
        form.add(new JLabel("Expiry Date:")); form.add(expiryF);
        form.add(new JLabel("Stock Quantity:")); form.add(qtyF);
        form.add(new JLabel("Storage Location:")); form.add(locF);
        form.add(new JLabel("Selling Price:")); form.add(priceF);

        int result = JOptionPane.showConfirmDialog(this, form, "Edit Record", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                db.getConnection().setAutoCommit(false);

                PreparedStatement psMed = db.prepareStatement("UPDATE Medicine SET medicine_name=?, category=?, dosage_form=?, strength=?, description=? WHERE medicine_id=?");
                psMed.setString(1, nameF.getText().trim());
                psMed.setString(2, catF.getText().trim());
                psMed.setString(3, dosageF.getText().trim());
                psMed.setString(4, strengthF.getText().trim());
                psMed.setString(5, descF.getText().trim());
                psMed.setInt(6, medId);
                psMed.executeUpdate();

                String newBatch = batchF.getText().trim();
                if (!newBatch.isEmpty() && !newBatch.equals("N/A")) {
                    ResultSet rsB = db.executeQuery("SELECT batch_id FROM Batch WHERE medicine_id=" + medId + " LIMIT 1");
                    if (rsB.next()) {
                        int bId = rsB.getInt(1);
                        PreparedStatement psB = db.prepareStatement("UPDATE Batch SET batch_number=?, expiry_date=?, selling_price=? WHERE batch_id=?");
                        psB.setString(1, newBatch);
                        psB.setString(2, expiryF.getText().trim());
                        psB.setDouble(3, Double.parseDouble(priceF.getText().trim()));
                        psB.setInt(4, bId);
                        psB.executeUpdate();

                        PreparedStatement psI = db.prepareStatement("UPDATE Inventory SET quantity_available=?, storage_location=? WHERE batch_id=?");
                        psI.setInt(1, Integer.parseInt(qtyF.getText().trim()));
                        psI.setString(2, locF.getText().trim());
                        psI.setInt(3, bId);
                        psI.executeUpdate();
                    } else {
                        ResultSet rsNewB = db.executeQuery("SELECT COALESCE(MAX(batch_id), 0) + 1 FROM Batch");
                        rsNewB.next(); int newBId = rsNewB.getInt(1); rsNewB.close();
                        PreparedStatement psB = db.prepareStatement(
                            "INSERT INTO Batch (batch_id, batch_number, manufacture_date, expiry_date, purchase_price, selling_price, medicine_id) " +
                            "VALUES (?, ?, CURDATE(), ?, ?, ?, ?)");
                        psB.setInt(1, newBId);
                        psB.setString(2, newBatch);
                        psB.setString(3, expiryF.getText().trim());
                        double sp = Double.parseDouble(priceF.getText().trim());
                        psB.setDouble(4, sp * 0.7);
                        psB.setDouble(5, sp);
                        psB.setInt(6, medId);
                        psB.executeUpdate();

                        ResultSet rsInvId = db.executeQuery("SELECT COALESCE(MAX(inventory_id), 0) + 1 FROM Inventory");
                        rsInvId.next(); int invId = rsInvId.getInt(1); rsInvId.close();
                        PreparedStatement psI = db.prepareStatement("INSERT INTO Inventory (inventory_id, quantity_available, storage_location, batch_id) VALUES (?, ?, ?, ?)");
                        psI.setInt(1, invId);
                        psI.setInt(2, Integer.parseInt(qtyF.getText().trim()));
                        psI.setString(3, locF.getText().trim());
                        psI.setInt(4, newBId);
                        psI.executeUpdate();
                    }
                    rsB.close();
                }

                db.getConnection().commit();
                db.getConnection().setAutoCommit(true);
                loadData();
            } catch (Exception ex) {
                try { DatabaseConnection.getInstance().getConnection().rollback(); } catch (Exception e) {}
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        row = table.convertRowIndexToModel(row);
        int medId = (int) model.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete medicine ID " + medId + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseConnection.getInstance().executeUpdate("DELETE FROM Medicine WHERE medicine_id=" + medId);
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage() +
                    "\n(Cannot delete if referenced by Batch records)", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("inventory_export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), "UTF-8"))) {
                for (int i = 0; i < model.getColumnCount() - 1; i++) { // skip Actions column
                    if (i > 0) pw.print(",");
                    pw.print("\"" + model.getColumnName(i) + "\"");
                }
                pw.println();
                for (int r = 0; r < model.getRowCount(); r++) {
                    for (int c = 0; c < model.getColumnCount() - 1; c++) {
                        if (c > 0) pw.print(",");
                        Object val = model.getValueAt(r, c);
                        pw.print("\"" + (val != null ? val.toString().replace("\"", "\"\"") : "") + "\"");
                    }
                    pw.println();
                }
                JOptionPane.showMessageDialog(this, "Exported to " + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createValuationCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(239, 246, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setMaximumSize(new Dimension(280, 120));
        JLabel l1 = new JLabel("Total Valuation");
        l1.setFont(ThemeColors.FONT_SMALL);
        l1.setForeground(ThemeColors.PRIMARY);
        JLabel l2 = new JLabel("Loading...");
        l2.setFont(ThemeColors.FONT_STAT_VALUE);
        l2.setForeground(ThemeColors.TEXT_PRIMARY);
        JPanel tp = new JPanel();
        tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
        tp.setOpaque(false);
        tp.add(l1); tp.add(l2);
        card.add(tp, BorderLayout.CENTER);

        SwingUtilities.invokeLater(() -> {
            try {
                ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                    "SELECT COALESCE(SUM(i.quantity_available * b.selling_price), 0) FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id WHERE b.status = 'Active'");
                if (rs.next()) l2.setText(String.format("₹%,.0f", rs.getDouble(1)));
                rs.close();
            } catch (Exception ex) { l2.setText("₹0"); }
        });
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
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER));
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));

        // Multi-batch-aware expiry renderer
        // Col 12 = batch_count (hidden), Col 13 = expiry_status (hidden)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                if (!sel) {
                    int modelRow = t.convertRowIndexToModel(row);
                    Color bg = row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252);

                    // Read data-driven expiry status and batch count
                    Object statusObj = t.getModel().getValueAt(modelRow, 13);
                    Object countObj = t.getModel().getValueAt(modelRow, 12);
                    String status = statusObj != null ? statusObj.toString() : "Normal";
                    int batchCount = 1;
                    try { batchCount = Integer.parseInt(countObj.toString()); } catch (Exception ignored) {}

                    boolean isMultiBatch = batchCount > 1;

                    if (isMultiBatch) {
                        // Multi-batch medicines: STRONG highlighting for near-expiry batches
                        switch (status) {
                            case "Expired", "Critical" -> bg = ThemeColors.EXPIRY_CRITICAL_BG;
                            case "Near Expiry" -> bg = ThemeColors.EXPIRY_WARNING_BG;
                            // "Normal" batches of multi-batch medicines: keep default (visible but less prominent)
                        }
                    } else {
                        // Single-batch medicines: subtle highlighting (still visible)
                        switch (status) {
                            case "Expired", "Critical" -> bg = new Color(255, 235, 235); // subtle red
                            case "Near Expiry" -> bg = new Color(255, 248, 230); // subtle orange
                        }
                    }
                    c.setBackground(bg);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return c;
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
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 20, ThemeColors.BUTTON_HEIGHT));
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
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 20, ThemeColors.BUTTON_HEIGHT));
        return btn;
    }

    // Button renderer/editor for action column
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setFont(ThemeColors.FONT_BUTTON);
            setForeground(ThemeColors.PRIMARY);
            setBackground(Color.WHITE);
            setBorderPainted(false); setFocusPainted(false);
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int row, int col) {
            setText(val != null ? val.toString() : "Edit");
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int currentRow;
        private boolean isPushed;
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("Edit");
            button.setFont(ThemeColors.FONT_BUTTON);
            button.setForeground(ThemeColors.PRIMARY);
            button.setBackground(Color.WHITE);
            button.setBorderPainted(false);
            button.addActionListener(e -> {
                isPushed = true;
                fireEditingStopped();
            });
        }
        @Override
        public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
            currentRow = row;
            isPushed = false;
            button.setText("Edit");
            return button;
        }
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                SwingUtilities.invokeLater(() -> {
                    table.setRowSelectionInterval(currentRow, currentRow);
                    editSelectedRow();
                });
            }
            isPushed = false;
            return "Edit";
        }
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}
