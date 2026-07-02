package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.*;

public class SuppliersPanel extends JPanel {
    private JTable supplierTable;
    private DefaultTableModel supplierModel;

    public SuppliersPanel() {
        setLayout(new BorderLayout());
        setBackground(ThemeColors.BG_MAIN);
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 16));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setOpaque(false);
        JButton addSupp = createPrimaryButton("+ Add Supplier");
        addSupp.addActionListener(e -> showAddSupplierDialog());
        toolbar.add(addSupp);
        JButton exportBtn = createSecondaryButton("Export CSV");
        exportBtn.addActionListener(e -> exportCSV());
        toolbar.add(exportBtn);
        wrapper.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] suppCols = {"ID", "Supplier Name", "License Number", "Contact", "Address"};
        supplierModel = new DefaultTableModel(suppCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        supplierTable = createStyledTable(supplierModel);
        supplierTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        supplierTable.getColumnModel().getColumn(1).setPreferredWidth(250);

        TableRowSorter<DefaultTableModel> sSorter = new TableRowSorter<>(supplierModel);
        supplierTable.setRowSorter(sSorter);

        // Context menu — edit only, no delete
        JPopupMenu suppPopup = new JPopupMenu();
        JMenuItem sEditItem = new JMenuItem("Edit Supplier");
        sEditItem.addActionListener(e -> showEditSupplierDialog());
        suppPopup.add(sEditItem);
        supplierTable.setComponentPopupMenu(suppPopup);
        supplierTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showEditSupplierDialog();
            }
        });

        JScrollPane scroll = new JScrollPane(supplierTable);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        wrapper.add(scroll, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
        SwingUtilities.invokeLater(this::loadData);
    }

    public void refreshData() {
        loadData();
    }

    private void loadData() {
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT * FROM Supplier ORDER BY supplier_id");
            supplierModel.setRowCount(0);
            while (rs.next()) {
                supplierModel.addRow(new Object[]{
                    rs.getInt("supplier_id"), rs.getString("supplier_name"),
                    rs.getString("license_number"), rs.getString("contact_number"), rs.getString("address")
                });
            }
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void showAddSupplierDialog() {
        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        JTextField idF = new JTextField(), nameF = new JTextField(), licF = new JTextField(), contF = new JTextField(), addrF = new JTextField();
        form.add(new JLabel("ID (Auto if blank):")); form.add(idF);
        form.add(new JLabel("Name:")); form.add(nameF);
        form.add(new JLabel("License#:")); form.add(licF);
        form.add(new JLabel("Contact:")); form.add(contF);
        form.add(new JLabel("Address:")); form.add(addrF);
        if (JOptionPane.showConfirmDialog(this, form, "Add Supplier", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                int id;
                if (idF.getText().trim().isEmpty()) {
                    ResultSet rs = db.executeQuery("SELECT COALESCE(MAX(supplier_id), 0) + 1 FROM Supplier");
                    rs.next(); id = rs.getInt(1); rs.close();
                } else {
                    id = Integer.parseInt(idF.getText().trim());
                }
                PreparedStatement ps = db.prepareStatement("INSERT INTO Supplier VALUES(?,?,?,?,?)");
                ps.setInt(1, id);
                ps.setString(2, nameF.getText().trim()); ps.setString(3, licF.getText().trim());
                ps.setString(4, contF.getText().trim()); ps.setString(5, addrF.getText().trim());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Supplier added!"); loadData();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void showEditSupplierDialog() {
        int row = supplierTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a supplier to edit"); return; }
        row = supplierTable.convertRowIndexToModel(row);
        int id = (int) supplierModel.getValueAt(row, 0);
        String name = (String) supplierModel.getValueAt(row, 1);
        String lic = (String) supplierModel.getValueAt(row, 2);
        String cont = (String) supplierModel.getValueAt(row, 3);
        String addr = (String) supplierModel.getValueAt(row, 4);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        JTextField nameF = new JTextField(name), licF = new JTextField(lic), contF = new JTextField(cont), addrF = new JTextField(addr);
        form.add(new JLabel("Name:")); form.add(nameF);
        form.add(new JLabel("License#:")); form.add(licF);
        form.add(new JLabel("Contact:")); form.add(contF);
        form.add(new JLabel("Address:")); form.add(addrF);
        if (JOptionPane.showConfirmDialog(this, form, "Edit Supplier (ID: " + id + ")", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement("UPDATE Supplier SET supplier_name=?, license_number=?, contact_number=?, address=? WHERE supplier_id=?");
                ps.setString(1, nameF.getText().trim()); ps.setString(2, licF.getText().trim());
                ps.setString(3, contF.getText().trim()); ps.setString(4, addrF.getText().trim());
                ps.setInt(5, id);
                ps.executeUpdate();
                loadData();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void exportCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("suppliers_export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), "UTF-8"))) {
                for (int i = 0; i < supplierModel.getColumnCount(); i++) {
                    if (i > 0) pw.print(",");
                    pw.print("\"" + supplierModel.getColumnName(i) + "\"");
                }
                pw.println();
                for (int r = 0; r < supplierModel.getRowCount(); r++) {
                    for (int c = 0; c < supplierModel.getColumnCount(); c++) {
                        if (c > 0) pw.print(",");
                        Object val = supplierModel.getValueAt(r, c);
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

    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(ThemeColors.FONT_TABLE);
        table.setRowHeight(ThemeColors.TABLE_ROW_HEIGHT);
        table.setShowGrid(false);
        table.setSelectionBackground(ThemeColors.PRIMARY_LIGHT);
        table.setSelectionForeground(ThemeColors.TEXT_PRIMARY);
        table.getTableHeader().setFont(ThemeColors.FONT_TABLE_HEADER);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(ThemeColors.TEXT_SECONDARY);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return comp;
            }
        });
        return table;
    }

    private JButton createPrimaryButton(String t) {
        JButton b = new JButton(t); b.setFont(ThemeColors.FONT_BUTTON); b.setBackground(ThemeColors.PRIMARY);
        b.setForeground(Color.WHITE); b.setFocusPainted(false); b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private JButton createSecondaryButton(String t) {
        JButton b = new JButton(t); b.setFont(ThemeColors.FONT_BUTTON); b.setBackground(Color.WHITE);
        b.setForeground(ThemeColors.TEXT_PRIMARY); b.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
}
