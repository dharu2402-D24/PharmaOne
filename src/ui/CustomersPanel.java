package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.*;

public class CustomersPanel extends JPanel {
    private JTable customerTable;
    private DefaultTableModel customerModel;

    public CustomersPanel() {
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
        JButton addCust = createPrimaryButton("+ Add Customer");
        addCust.addActionListener(e -> showAddCustomerDialog());
        toolbar.add(addCust);
        JButton exportBtn = createSecondaryButton("Export CSV");
        exportBtn.addActionListener(e -> exportCSV());
        toolbar.add(exportBtn);
        wrapper.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] custCols = {"ID", "Customer Name", "Type", "Address", "License Number", "Contact"};
        customerModel = new DefaultTableModel(custCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        customerTable = createStyledTable(customerModel);

        TableRowSorter<DefaultTableModel> cSorter = new TableRowSorter<>(customerModel);
        customerTable.setRowSorter(cSorter);

        // Context menu — edit only, no delete
        JPopupMenu custPopup = new JPopupMenu();
        JMenuItem cEditItem = new JMenuItem("Edit Customer");
        cEditItem.addActionListener(e -> showEditCustomerDialog());
        custPopup.add(cEditItem);
        customerTable.setComponentPopupMenu(custPopup);
        customerTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showEditCustomerDialog();
            }
        });

        JScrollPane scroll = new JScrollPane(customerTable);
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
            ResultSet rs = db.executeQuery("SELECT * FROM Customer ORDER BY customer_id");
            customerModel.setRowCount(0);
            while (rs.next()) {
                customerModel.addRow(new Object[]{
                    rs.getInt("customer_id"), rs.getString("customer_name"),
                    rs.getString("customer_type"), rs.getString("address"),
                    rs.getString("license_number"), rs.getString("contact_number")
                });
            }
            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void showAddCustomerDialog() {
        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        JTextField idF = new JTextField(), nameF = new JTextField(), addrF = new JTextField(), licF = new JTextField(), contF = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Pharmacy", "Hospital"});
        form.add(new JLabel("ID (Auto if blank):")); form.add(idF);
        form.add(new JLabel("Name:")); form.add(nameF);
        form.add(new JLabel("Type:")); form.add(typeBox);
        form.add(new JLabel("Address:")); form.add(addrF);
        form.add(new JLabel("License#:")); form.add(licF);
        form.add(new JLabel("Contact:")); form.add(contF);
        if (JOptionPane.showConfirmDialog(this, form, "Add Customer", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                DatabaseConnection db = DatabaseConnection.getInstance();
                int id;
                if (idF.getText().trim().isEmpty()) {
                    ResultSet rs = db.executeQuery("SELECT COALESCE(MAX(customer_id), 0) + 1 FROM Customer");
                    rs.next(); id = rs.getInt(1); rs.close();
                } else {
                    id = Integer.parseInt(idF.getText().trim());
                }
                PreparedStatement ps = db.prepareStatement("INSERT INTO Customer VALUES(?,?,?,?,?,?)");
                ps.setInt(1, id);
                ps.setString(2, nameF.getText().trim()); ps.setString(3, (String) typeBox.getSelectedItem());
                ps.setString(4, addrF.getText().trim()); ps.setString(5, licF.getText().trim()); ps.setString(6, contF.getText().trim());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Customer added!"); loadData();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void showEditCustomerDialog() {
        int row = customerTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a customer to edit"); return; }
        row = customerTable.convertRowIndexToModel(row);
        int id = (int) customerModel.getValueAt(row, 0);
        String name = (String) customerModel.getValueAt(row, 1);
        String type = (String) customerModel.getValueAt(row, 2);
        String addr = (String) customerModel.getValueAt(row, 3);
        String lic = (String) customerModel.getValueAt(row, 4);
        String cont = (String) customerModel.getValueAt(row, 5);

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        JTextField nameF = new JTextField(name), addrF = new JTextField(addr), licF = new JTextField(lic), contF = new JTextField(cont);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Pharmacy", "Hospital"});
        typeBox.setSelectedItem(type);
        form.add(new JLabel("Name:")); form.add(nameF);
        form.add(new JLabel("Type:")); form.add(typeBox);
        form.add(new JLabel("Address:")); form.add(addrF);
        form.add(new JLabel("License#:")); form.add(licF);
        form.add(new JLabel("Contact:")); form.add(contF);
        if (JOptionPane.showConfirmDialog(this, form, "Edit Customer (ID: " + id + ")", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                PreparedStatement ps = DatabaseConnection.getInstance().prepareStatement("UPDATE Customer SET customer_name=?, customer_type=?, address=?, license_number=?, contact_number=? WHERE customer_id=?");
                ps.setString(1, nameF.getText().trim()); ps.setString(2, (String) typeBox.getSelectedItem());
                ps.setString(3, addrF.getText().trim()); ps.setString(4, licF.getText().trim()); ps.setString(5, contF.getText().trim());
                ps.setInt(6, id);
                ps.executeUpdate();
                loadData();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void exportCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("customers_export.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), "UTF-8"))) {
                // Header
                for (int i = 0; i < customerModel.getColumnCount(); i++) {
                    if (i > 0) pw.print(",");
                    pw.print("\"" + customerModel.getColumnName(i) + "\"");
                }
                pw.println();
                // Data
                for (int r = 0; r < customerModel.getRowCount(); r++) {
                    for (int c = 0; c < customerModel.getColumnCount(); c++) {
                        if (c > 0) pw.print(",");
                        Object val = customerModel.getValueAt(r, c);
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
