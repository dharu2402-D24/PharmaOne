package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TriggerDemoPanel extends JPanel {
    private JTextArea logArea;
    private DefaultTableModel beforeModel, afterModel;

    public TriggerDemoPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(ThemeColors.BG_MAIN);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        buildUI();
    }

    private void buildUI() {
        // Title
        JLabel title = new JLabel("Trigger Demonstrations");
        title.setFont(ThemeColors.FONT_TITLE);
        title.setForeground(ThemeColors.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Trigger 1: Prevent expired batch sale
        content.add(createTriggerCard(
            "Trigger 1: prevent_expired_batch_sale",
            "BEFORE INSERT on Sales_Order — blocks sale if batch is expired",
            "DELIMITER //\n" +
            "CREATE TRIGGER prevent_expired_batch_sale\n" +
            "BEFORE INSERT ON Sales_Order\n" +
            "FOR EACH ROW\n" +
            "BEGIN\n" +
            "    DECLARE exp_date DATE;\n" +
            "    SELECT expiry_date INTO exp_date FROM Batch WHERE batch_id = NEW.batch_id;\n" +
            "    IF exp_date < CURDATE() THEN\n" +
            "        SIGNAL SQLSTATE '45000'\n" +
            "        SET MESSAGE_TEXT = 'Cannot sell expired batch!';\n" +
            "    END IF;\n" +
            "END //\n" +
            "DELIMITER ;",
            "Demo: Attempt Expired Batch Sale",
            e -> demoTrigger1()
        ));

        content.add(Box.createVerticalStrut(16));

        // Trigger 2: Auto-update inventory
        content.add(createTriggerCard(
            "Trigger 2: auto_update_inventory",
            "AFTER INSERT on Sales_Order — automatically decrements inventory quantity",
            "DELIMITER //\n" +
            "CREATE TRIGGER auto_update_inventory\n" +
            "AFTER INSERT ON Sales_Order\n" +
            "FOR EACH ROW\n" +
            "BEGIN\n" +
            "    UPDATE Inventory\n" +
            "    SET quantity_available = quantity_available - NEW.quantity_sold,\n" +
            "        last_updated = NOW()\n" +
            "    WHERE batch_id = NEW.batch_id;\n" +
            "END //\n" +
            "DELIMITER ;",
            "Demo: Auto Inventory Update",
            e -> demoTrigger2()
        ));

        content.add(Box.createVerticalStrut(16));

        // Before/After comparison tables
        JPanel comparePanel = new JPanel(new GridLayout(1, 2, 16, 0));
        comparePanel.setOpaque(false);
        comparePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        comparePanel.setAlignmentX(LEFT_ALIGNMENT);

        String[] cols = {"Batch ID", "Batch#", "Qty Available", "Last Updated"};
        beforeModel = new DefaultTableModel(cols, 0);
        afterModel = new DefaultTableModel(cols, 0);

        JPanel beforePanel = createTableCard("BEFORE State", beforeModel);
        JPanel afterPanel = createTableCard("AFTER State", afterModel);
        comparePanel.add(beforePanel);
        comparePanel.add(afterPanel);
        content.add(comparePanel);

        content.add(Box.createVerticalStrut(16));

        // Transaction log
        JPanel logCard = new JPanel(new BorderLayout(0, 8));
        logCard.setBackground(Color.WHITE);
        logCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        logCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        logCard.setAlignmentX(LEFT_ALIGNMENT);
        JLabel logTitle = new JLabel("Transaction Log");
        logTitle.setFont(ThemeColors.FONT_CARD_TITLE);
        logTitle.setForeground(ThemeColors.TEXT_PRIMARY);
        logCard.add(logTitle, BorderLayout.NORTH);
        logArea = new JTextArea(6, 40);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setBackground(new Color(248, 250, 252));
        logCard.add(new JScrollPane(logArea), BorderLayout.CENTER);
        content.add(logCard);

        // Create triggers button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(LEFT_ALIGNMENT);
        JButton createBtn = new JButton("Create Both Triggers in Database");
        createBtn.setFont(ThemeColors.FONT_BUTTON);
        createBtn.setBackground(ThemeColors.WARNING);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);
        createBtn.setBorderPainted(false);
        createBtn.addActionListener(e -> createTriggers());
        btnPanel.add(createBtn);
        content.add(Box.createVerticalStrut(8));
        content.add(btnPanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(ThemeColors.BG_MAIN);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createTriggerCard(String name, String desc, String sql, String btnText, java.awt.event.ActionListener action) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(ThemeColors.FONT_CARD_TITLE);
        nameLbl.setForeground(ThemeColors.PRIMARY);
        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(ThemeColors.FONT_SMALL);
        descLbl.setForeground(ThemeColors.TEXT_SECONDARY);
        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(nameLbl); headerText.add(descLbl);
        header.add(headerText, BorderLayout.CENTER);

        JButton demoBtn = new JButton(btnText);
        demoBtn.setFont(ThemeColors.FONT_BUTTON);
        demoBtn.setBackground(ThemeColors.PRIMARY);
        demoBtn.setForeground(Color.WHITE);
        demoBtn.setFocusPainted(false);
        demoBtn.setBorderPainted(false);
        demoBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        demoBtn.addActionListener(action);
        header.add(demoBtn, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JTextArea sqlArea = new JTextArea(sql);
        sqlArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        sqlArea.setEditable(false);
        sqlArea.setBackground(new Color(248, 250, 252));
        sqlArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane sp = new JScrollPane(sqlArea);
        sp.setPreferredSize(new Dimension(0, 150));
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private JPanel createTableCard(String title, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeColors.BORDER),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(ThemeColors.FONT_CARD_TITLE);
        lbl.setForeground(title.contains("BEFORE") ? ThemeColors.WARNING : ThemeColors.SUCCESS);
        panel.add(lbl, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setFont(ThemeColors.FONT_TABLE);
        table.setRowHeight(36);
        table.getTableHeader().setFont(ThemeColors.FONT_TABLE_HEADER);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void log(String msg) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        logArea.append("[" + timestamp + "] " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void captureInventoryState(DefaultTableModel model) {
        model.setRowCount(0);
        try {
            ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                "SELECT i.batch_id, b.batch_number, i.quantity_available, i.last_updated " +
                "FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id ORDER BY i.batch_id");
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4)});
            }
            rs.close();
        } catch (Exception ex) { log("Error: " + ex.getMessage()); }
    }

    private void createTriggers() {
        log("Attempting to create triggers...");
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            // Drop if exists
            try { db.executeUpdate("DROP TRIGGER IF EXISTS prevent_expired_batch_sale"); } catch (Exception e) {}
            try { db.executeUpdate("DROP TRIGGER IF EXISTS auto_update_inventory"); } catch (Exception e) {}

            // Create trigger 1
            db.executeUpdate(
                "CREATE TRIGGER prevent_expired_batch_sale BEFORE INSERT ON Sales_Order " +
                "FOR EACH ROW BEGIN " +
                "DECLARE exp_date DATE; " +
                "SELECT expiry_date INTO exp_date FROM Batch WHERE batch_id = NEW.batch_id; " +
                "IF exp_date < CURDATE() THEN " +
                "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cannot sell expired batch!'; " +
                "END IF; END");
            log("\u2713 Trigger 'prevent_expired_batch_sale' created successfully");

            // Create trigger 2
            db.executeUpdate(
                "CREATE TRIGGER auto_update_inventory AFTER INSERT ON Sales_Order " +
                "FOR EACH ROW BEGIN " +
                "UPDATE Inventory SET quantity_available = quantity_available - NEW.quantity_sold, " +
                "last_updated = NOW() WHERE batch_id = NEW.batch_id; END");
            log("\u2713 Trigger 'auto_update_inventory' created successfully");

            JOptionPane.showMessageDialog(this, "Both triggers created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            log("\u2717 Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void demoTrigger1() {
        log("=== DEMO: prevent_expired_batch_sale ===");
        log("Finding an expired batch...");
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            ResultSet rs = db.executeQuery("SELECT batch_id, batch_number, expiry_date FROM Batch WHERE expiry_date < CURDATE() LIMIT 1");
            if (rs.next()) {
                int batchId = rs.getInt(1);
                String batchNum = rs.getString(2);
                String expDate = rs.getString(3);
                log("Found expired batch: " + batchNum + " (expired: " + expDate + ")");
                log("Attempting: INSERT INTO Sales_Order VALUES (999, CURDATE(), 10, 20.00, 200.00, 'Completed', 401, " + batchId + ")");
                rs.close();

                captureInventoryState(beforeModel);

                try {
                    db.executeUpdate("INSERT INTO Sales_Order VALUES (999, CURDATE(), 10, 20.00, 200.00, 'Completed', 401, " + batchId + ")");
                    log("Unexpected: Insert succeeded (trigger may not be created yet)");
                } catch (SQLException ex) {
                    log("\u2713 TRIGGER FIRED! Sale blocked: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this,
                        "Trigger fired successfully!\n\nBatch: " + batchNum + "\nExpired: " + expDate +
                        "\n\nError: " + ex.getMessage(),
                        "Trigger 1 Demo - Sale Blocked", JOptionPane.WARNING_MESSAGE);
                }
                captureInventoryState(afterModel);
            } else {
                rs.close();
                log("No expired batches found. Insert one with past expiry date to demo.");
                JOptionPane.showMessageDialog(this, "No expired batches in database.\nAll batch expiry dates are in the future.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            log("Error: " + ex.getMessage());
        }
    }

    private void demoTrigger2() {
        log("=== DEMO: auto_update_inventory ===");
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();

            // Find a valid batch with inventory
            ResultSet rs = db.executeQuery(
                "SELECT b.batch_id, b.batch_number, i.quantity_available FROM Batch b " +
                "JOIN Inventory i ON b.batch_id = i.batch_id " +
                "WHERE b.expiry_date >= CURDATE() AND i.quantity_available > 50 LIMIT 1");

            if (rs.next()) {
                int batchId = rs.getInt(1);
                String batchNum = rs.getString(2);
                int qtyBefore = rs.getInt(3);
                rs.close();

                log("Selected batch: " + batchNum + " (current qty: " + qtyBefore + ")");
                captureInventoryState(beforeModel);

                // Generate a unique sales order ID
                rs = db.executeQuery("SELECT COALESCE(MAX(sales_order_id), 0) + 1 FROM Sales_Order");
                int newId = rs.next() ? rs.getInt(1) : 9999;
                rs.close();

                int saleQty = 5;
                log("Inserting: Sales Order #" + newId + " for " + saleQty + " units of batch " + batchNum);
                db.executeUpdate("INSERT INTO Sales_Order VALUES (" + newId + ", CURDATE(), " +
                    saleQty + ", 20.00, " + (saleQty * 20.0) + ", 'Completed', 401, " + batchId + ")");

                log("\u2713 Sales order inserted");

                // Check inventory after
                rs = db.executeQuery("SELECT quantity_available FROM Inventory WHERE batch_id = " + batchId);
                int qtyAfter = rs.next() ? rs.getInt(1) : -1;
                rs.close();

                captureInventoryState(afterModel);

                log("\u2713 Inventory auto-updated: " + qtyBefore + " -> " + qtyAfter + " (decreased by " + (qtyBefore - qtyAfter) + ")");

                JOptionPane.showMessageDialog(this,
                    "Trigger fired successfully!\n\nBatch: " + batchNum +
                    "\nBefore: " + qtyBefore + " units\nAfter: " + qtyAfter + " units" +
                    "\nAuto-decreased by: " + (qtyBefore - qtyAfter),
                    "Trigger 2 Demo - Inventory Updated", JOptionPane.INFORMATION_MESSAGE);

                // Clean up: remove the test order
                db.executeUpdate("DELETE FROM Sales_Order WHERE sales_order_id = " + newId);
                db.executeUpdate("UPDATE Inventory SET quantity_available = " + qtyBefore + " WHERE batch_id = " + batchId);
                log("Cleanup: Test order removed, inventory restored");
            } else {
                rs.close();
                log("No valid batches with inventory found");
            }
        } catch (Exception ex) {
            log("Error: " + ex.getMessage());
        }
    }
}
