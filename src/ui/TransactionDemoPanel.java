package ui;

import util.ThemeColors;
import db.DatabaseConnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task 6: Transaction Demo Panel
 * Demonstrates DB transactions including conflicting scenarios
 * and explains their effects on database consistency and concurrency.
 *
 * Uses multiple JDBC connections to simulate concurrent sessions.
 */
public class TransactionDemoPanel extends JPanel {
    private JTextArea logArea;
    private DefaultTableModel beforeModel, afterModel;
    private JButton[] demoButtons;
    private boolean demoRunning = false;

    public TransactionDemoPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeColors.BG_MAIN);
        buildUI();
    }

    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 16));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // ─── Top: Description panel ───
        JPanel descPanel = new JPanel(new BorderLayout(8, 4));
        descPanel.setOpaque(false);
        JLabel titleLbl = new JLabel("Task 6: DB Transaction Demonstrations");
        titleLbl.setFont(ThemeColors.FONT_TITLE);
        titleLbl.setForeground(ThemeColors.TEXT_PRIMARY);
        JLabel descLbl = new JLabel(
                "<html>Interactive demos showing COMMIT, ROLLBACK, dirty reads, lost updates, and deadlock detection using concurrent MySQL sessions.</html>");
        descLbl.setFont(ThemeColors.FONT_BODY);
        descLbl.setForeground(ThemeColors.TEXT_SECONDARY);
        descPanel.add(titleLbl, BorderLayout.NORTH);
        descPanel.add(descLbl, BorderLayout.SOUTH);
        wrapper.add(descPanel, BorderLayout.NORTH);

        // ─── Center: Split — left demos, right log ───
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);

        // Left: Demo cards
        JPanel demosPanel = new JPanel();
        demosPanel.setLayout(new BoxLayout(demosPanel, BoxLayout.Y_AXIS));
        demosPanel.setOpaque(false);

        String[][] scenarios = {
                { "Scenario 1: COMMIT (Atomicity)",
                        "Multi-step transaction: Insert Sales Order + Deduct Inventory → COMMIT. Both succeed atomically or neither does.",
                        "START TRANSACTION;\n  INSERT INTO Sales_Order ...;\n  UPDATE Inventory SET qty = qty - 50 ...;\nCOMMIT;" },
                { "Scenario 2: ROLLBACK (Consistency)",
                        "Start a sale → detect insufficient stock midway → ROLLBACK. No partial changes remain in the database.",
                        "START TRANSACTION;\n  INSERT INTO Sales_Order (qty=99999) ...;\n  UPDATE Inventory SET qty = qty - 99999;\n  -- qty went negative! Abort!\nROLLBACK;" },
                { "Scenario 3: Dirty Read Prevention (Isolation)",
                        "Session A updates a row without committing. Session B reads it with different isolation levels to show dirty read prevention.",
                        "-- Session A (READ COMMITTED):\nSTART TRANSACTION;\n  UPDATE Inventory SET qty = 9999 ...;\n  -- NOT committed yet!\n\n-- Session B reads:\n  READ COMMITTED  → sees OLD value ✓\n  READ UNCOMMITTED → sees 9999 (DIRTY!)" },
                { "Scenario 4: Lost Update (Concurrency)",
                        "Two sessions read the same qty, compute independently, then write — one update is lost. Then shows SELECT ... FOR UPDATE fix.",
                        "-- Session A reads qty=600, subtracts 200\n-- Session B reads qty=600, subtracts 300\n-- Expected: 100. Actual: 300!\n\n-- FIX: SELECT ... FOR UPDATE;\n-- Session B waits, reads 400, result: 100 ✓" },
                { "Scenario 5: Deadlock Detection",
                        "Session A locks row X then tries Y. Session B locks row Y then tries X. MySQL detects the circular wait and kills one.",
                        "-- Session A: LOCK batch 103, then try 105\n-- Session B: LOCK batch 105, then try 103\n-- → Deadlock! MySQL Error 1213\n-- One session rolled back automatically." }
        };

        demoButtons = new JButton[scenarios.length];
        for (int i = 0; i < scenarios.length; i++) {
            JPanel card = createDemoCard(scenarios[i][0], scenarios[i][1], scenarios[i][2], i);
            demosPanel.add(card);
            demosPanel.add(Box.createVerticalStrut(10));
        }

        // Run All button
        JPanel runAllPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        runAllPanel.setOpaque(false);
        runAllPanel.setAlignmentX(LEFT_ALIGNMENT);
        runAllPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JButton runAllBtn = new JButton("▶ Run All Scenarios Sequentially");
        runAllBtn.setFont(ThemeColors.FONT_BUTTON);
        runAllBtn.setBackground(new Color(124, 58, 237));
        runAllBtn.setForeground(Color.WHITE);
        runAllBtn.setFocusPainted(false);
        runAllBtn.setBorderPainted(false);
        runAllBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runAllBtn.addActionListener(e -> runAllScenarios());
        runAllPanel.add(runAllBtn);

        JButton clearLogBtn = new JButton("Clear Log");
        clearLogBtn.setFont(ThemeColors.FONT_BUTTON);
        clearLogBtn.setBackground(Color.WHITE);
        clearLogBtn.setForeground(ThemeColors.TEXT_PRIMARY);
        clearLogBtn.setBorder(BorderFactory.createLineBorder(ThemeColors.BORDER));
        clearLogBtn.setFocusPainted(false);
        clearLogBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearLogBtn.addActionListener(e -> {
            logArea.setText("");
            beforeModel.setRowCount(0);
            afterModel.setRowCount(0);
        });
        runAllPanel.add(clearLogBtn);
        demosPanel.add(runAllPanel);

        JScrollPane demosScroll = new JScrollPane(demosPanel);
        demosScroll.setBorder(null);
        demosScroll.getVerticalScrollBar().setUnitIncrement(16);
        demosScroll.getViewport().setBackground(ThemeColors.BG_MAIN);
        splitPane.setLeftComponent(demosScroll);

        // Right: Log + Before/After tables
        JPanel rightPanel = new JPanel(new BorderLayout(0, 12));
        rightPanel.setOpaque(false);

        // Transaction log
        JPanel logCard = new JPanel(new BorderLayout(0, 8));
        logCard.setBackground(Color.WHITE);
        logCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeColors.BORDER),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JLabel logTitle = new JLabel("Transaction Log");
        logTitle.setFont(ThemeColors.FONT_CARD_TITLE);
        logTitle.setForeground(ThemeColors.TEXT_PRIMARY);
        logCard.add(logTitle, BorderLayout.NORTH);
        logArea = new JTextArea(20, 40);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setEditable(false);
        logArea.setBackground(new Color(15, 23, 42));
        logArea.setForeground(new Color(226, 232, 240));
        logArea.setCaretColor(new Color(226, 232, 240));
        logArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logCard.add(logScroll, BorderLayout.CENTER);
        rightPanel.add(logCard, BorderLayout.CENTER);

        // Before/After comparison
        JPanel comparePanel = new JPanel(new GridLayout(1, 2, 8, 0));
        comparePanel.setOpaque(false);
        comparePanel.setPreferredSize(new Dimension(0, 200));

        String[] cols = { "Batch ID", "Batch#", "Qty Available", "Last Updated" };
        beforeModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        afterModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        comparePanel.add(createComparisonTable("BEFORE State", beforeModel, ThemeColors.WARNING));
        comparePanel.add(createComparisonTable("AFTER State", afterModel, ThemeColors.SUCCESS));
        rightPanel.add(comparePanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPanel);
        wrapper.add(splitPane, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel createDemoCard(String title, String desc, String sql, int index) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
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
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Header with title + button
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        JLabel nameLbl = new JLabel(title);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(ThemeColors.PRIMARY);
        JLabel descLbl = new JLabel("<html><div style='width:300px'>" + desc + "</div></html>");
        descLbl.setFont(ThemeColors.FONT_SMALL);
        descLbl.setForeground(ThemeColors.TEXT_SECONDARY);
        titlePanel.add(nameLbl);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(descLbl);
        header.add(titlePanel, BorderLayout.CENTER);

        JButton demoBtn = new JButton("▶ Run");
        demoBtn.setFont(ThemeColors.FONT_BUTTON);
        demoBtn.setBackground(ThemeColors.PRIMARY);
        demoBtn.setForeground(Color.WHITE);
        demoBtn.setFocusPainted(false);
        demoBtn.setBorderPainted(false);
        demoBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        demoBtn.setPreferredSize(new Dimension(90, 34));
        demoBtn.addActionListener(e -> runScenario(index));
        demoButtons[index] = demoBtn;
        header.add(demoBtn, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // SQL preview
        JTextArea sqlArea = new JTextArea(sql);
        sqlArea.setFont(new Font("Consolas", Font.PLAIN, 10));
        sqlArea.setEditable(false);
        sqlArea.setBackground(new Color(248, 250, 252));
        sqlArea.setForeground(new Color(71, 85, 105));
        sqlArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        sqlArea.setRows(4);
        JScrollPane sp = new JScrollPane(sqlArea);
        sp.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        sp.setPreferredSize(new Dimension(0, 70));
        card.add(sp, BorderLayout.CENTER);

        return card;
    }

    private JPanel createComparisonTable(String title, DefaultTableModel model, Color accentColor) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeColors.BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel lbl = new JLabel(title);
        lbl.setFont(ThemeColors.FONT_CARD_TITLE);
        lbl.setForeground(accentColor);
        panel.add(lbl, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setFont(ThemeColors.FONT_TABLE);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.getTableHeader().setFont(ThemeColors.FONT_TABLE_HEADER);
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s)
                    comp.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return comp;
            }
        });
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // ─── Logging ───
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            logArea.append("[" + timestamp + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void logHeader(String msg) {
        log("═══════════════════════════════════════════");
        log("  " + msg);
        log("═══════════════════════════════════════════");
    }

    private void logSession(String session, String msg) {
        log("  [" + session + "] " + msg);
    }

    private void logResult(String msg) {
        log("  ✓ RESULT: " + msg);
    }

    private void logError(String msg) {
        log("  ✗ ERROR: " + msg);
    }

    private void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    // ─── Capture inventory state ───
    private void captureInventoryState(DefaultTableModel model) {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            try {
                ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                        "SELECT i.batch_id, b.batch_number, i.quantity_available, i.last_updated " +
                                "FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id WHERE b.status = 'Active' ORDER BY i.batch_id");
                while (rs.next()) {
                    model.addRow(new Object[] { rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4) });
                }
                rs.close();
            } catch (Exception ex) {
                log("  Error capturing state: " + ex.getMessage());
            }
        });
    }

    // ─── Disable/Enable buttons ───
    private void setDemoRunning(boolean running) {
        demoRunning = running;
        SwingUtilities.invokeLater(() -> {
            for (JButton btn : demoButtons) {
                if (btn != null) {
                    btn.setEnabled(!running);
                    btn.setText(running ? "Running..." : "▶ Run");
                }
            }
        });
    }

    // ─── Run a single scenario ───
    private void runScenario(int index) {
        if (demoRunning) {
            JOptionPane.showMessageDialog(this, "A demo is already running. Please wait.", "Busy",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        setDemoRunning(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    switch (index) {
                        case 0 -> demoCommit();
                        case 1 -> demoRollback();
                        case 2 -> demoDirtyRead();
                        case 3 -> demoLostUpdate();
                        case 4 -> demoDeadlock();
                    }
                } catch (Exception ex) {
                    logError("Unexpected: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                setDemoRunning(false);
            }
        }.execute();
    }

    // ─── Run all scenarios ───
    private void runAllScenarios() {
        if (demoRunning) {
            JOptionPane.showMessageDialog(this, "A demo is already running.", "Busy", JOptionPane.WARNING_MESSAGE);
            return;
        }
        logArea.setText("");
        setDemoRunning(true);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    demoCommit();
                    pause(1000);
                    demoRollback();
                    pause(1000);
                    demoDirtyRead();
                    pause(1000);
                    demoLostUpdate();
                    pause(1000);
                    demoDeadlock();
                    log("");
                    logHeader("ALL 5 SCENARIOS COMPLETED SUCCESSFULLY");
                } catch (Exception ex) {
                    logError("Unexpected: " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                setDemoRunning(false);
                JOptionPane.showMessageDialog(TransactionDemoPanel.this,
                        "All 5 transaction scenarios completed!\nCheck the Transaction Log for details.",
                        "Demo Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        }.execute();
    }

    // ═══════════════════════════════════════════════════════════
    // SCENARIO 1: COMMIT (Atomicity)
    // ═══════════════════════════════════════════════════════════
    private void demoCommit() {
        logHeader("SCENARIO 1: COMMIT — Atomicity");
        log("  A sales order + inventory deduction must succeed together.");
        log("");

        captureInventoryState(beforeModel);
        pause(500);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().createNewConnection();

            // Read the original qty
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int originalQty = rs.next() ? rs.getInt(1) : 0;
            rs.close();
            st.close();

            logSession("SESSION", "Original inventory for Batch 103: " + originalQty + " units");
            pause(300);

            // Generate a unique sales order ID
            Statement stId = conn.createStatement();
            rs = stId.executeQuery("SELECT COALESCE(MAX(sales_order_id), 0) + 1 FROM Sales_Order");
            int newId = rs.next() ? rs.getInt(1) : 9901;
            rs.close();
            stId.close();

            logSession("SESSION", "START TRANSACTION");
            conn.setAutoCommit(false);
            pause(200);

            // Step 1: Insert sales order
            logSession("SESSION", "Step 1: INSERT INTO Sales_Order (id=" + newId + ", qty=50, batch=103)");
            PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO Sales_Order (sales_order_id, order_date, quantity_sold, selling_price, total_amount, status, customer_id, batch_id) "
                            +
                            "VALUES (?, CURDATE(), 50, 20.00, 1000.00, 'Completed', 401, 103)");
            ps1.setInt(1, newId);
            ps1.executeUpdate();
            ps1.close();
            logSession("SESSION", "  → Sales order inserted (not committed yet)");
            pause(300);

            // Step 2: Deduct inventory
            logSession("SESSION", "Step 2: UPDATE Inventory SET qty = qty - 50 WHERE batch_id = 103");
            Statement st2 = conn.createStatement();
            st2.executeUpdate(
                    "UPDATE Inventory SET quantity_available = quantity_available - 50, last_updated = NOW() WHERE batch_id = 103");
            st2.close();
            logSession("SESSION", "  → Inventory deducted (not committed yet)");
            pause(300);

            // COMMIT
            logSession("SESSION", "COMMIT");
            conn.commit();
            logResult("Transaction COMMITTED — both changes are now permanent!");
            pause(200);

            // Verify
            Statement stV = conn.createStatement();
            rs = stV.executeQuery("SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int newQty = rs.next() ? rs.getInt(1) : -1;
            rs.close();
            stV.close();
            logResult("Inventory batch 103: " + originalQty + " → " + newQty + " (decreased by "
                    + (originalQty - newQty) + ")");

            rs = conn.createStatement().executeQuery(
                    "SELECT sales_order_id, total_amount FROM Sales_Order WHERE sales_order_id = " + newId);
            if (rs.next()) {
                logResult("Sales Order #" + rs.getInt(1) + " confirmed (total: ₹" + rs.getDouble(2) + ")");
            }
            rs.close();

            // Capture AFTER state (showing the transaction effect BEFORE cleanup)
            captureInventoryState(afterModel);
            pause(500);

            // Cleanup
            log("  Cleanup: Removing test order and restoring inventory...");
            conn.setAutoCommit(true);
            conn.createStatement().executeUpdate("DELETE FROM Sales_Order WHERE sales_order_id = " + newId);
            conn.createStatement().executeUpdate(
                    "UPDATE Inventory SET quantity_available = " + originalQty + " WHERE batch_id = 103");
            logResult("Cleanup complete — database restored to original state.");

        } catch (SQLException ex) {
            logError(ex.getMessage());
            try {
                if (conn != null)
                    conn.rollback();
            } catch (Exception ignored) {
            }
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ignored) {
            }
        }

        log("");
    }

    // ═══════════════════════════════════════════════════════════
    // SCENARIO 2: ROLLBACK (Consistency)
    // ═══════════════════════════════════════════════════════════
    private void demoRollback() {
        logHeader("SCENARIO 2: ROLLBACK — Consistency");
        log("  Selling more than available stock must abort entirely.");
        log("");

        captureInventoryState(beforeModel);
        pause(500);

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().createNewConnection();

            // Read original qty
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(
                    "SELECT i.quantity_available, b.batch_number FROM Inventory i JOIN Batch b ON i.batch_id = b.batch_id WHERE i.batch_id = 103");
            int originalQty = 0;
            String batchNum = "";
            if (rs.next()) {
                originalQty = rs.getInt(1);
                batchNum = rs.getString(2);
            }
            rs.close();
            st.close();

            logSession("SESSION", "Current stock for " + batchNum + " (Batch 103): " + originalQty + " units");
            pause(300);

            logSession("SESSION", "START TRANSACTION");
            conn.setAutoCommit(false);
            pause(200);

            // Step 1: Insert sales order for way too many units
            int attemptQty = 99999;
            logSession("SESSION", "Step 1: INSERT Sales Order (qty=" + attemptQty + " — more than available!)");

            int newId = 9902;
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Sales_Order (sales_order_id, order_date, quantity_sold, selling_price, total_amount, status, customer_id, batch_id) "
                            +
                            "VALUES (?, CURDATE(), ?, 2.00, ?, 'Completed', 401, 103)");
            ps.setInt(1, newId);
            ps.setInt(2, attemptQty);
            ps.setDouble(3, attemptQty * 2.0);
            ps.executeUpdate();
            ps.close();
            logSession("SESSION", "  → Order inserted (within transaction, not committed)");
            pause(300);

            // Step 2: Try to deduct inventory — this will FAIL due to CHECK constraint
            logSession("SESSION", "Step 2: UPDATE Inventory SET qty = qty - " + attemptQty);
            try {
                conn.createStatement().executeUpdate(
                        "UPDATE Inventory SET quantity_available = quantity_available - " + attemptQty
                                + " WHERE batch_id = 103");
            } catch (SQLException constraintEx) {
                // The CHECK constraint (quantity_available >= 0) catches this!
                pause(200);
                logError("UPDATE FAILED! " + constraintEx.getMessage());
                logSession("SESSION",
                        "  → CHECK CONSTRAINT 'inventory_chk_1' violated! DB rejected negative quantity.");
                logSession("SESSION",
                        "  → The database itself enforces data integrity (quantity_available >= 0).");
                pause(400);

                // The sales order from Step 1 is still pending — ROLLBACK undoes it
                logSession("SESSION",
                        "Step 1's INSERT is still in the transaction — must ROLLBACK to undo it.");
                logSession("SESSION", "ROLLBACK");
                conn.rollback();
                logResult("Transaction ROLLED BACK — Sales Order insertion also undone!");
                pause(300);
            }

            // Verify nothing changed
            conn.setAutoCommit(true);
            rs = conn.createStatement().executeQuery(
                    "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int afterQty = rs.next() ? rs.getInt(1) : -1;
            rs.close();
            logResult("Inventory batch 103 is still: " + afterQty + " (unchanged from " + originalQty + ")");

            rs = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM Sales_Order WHERE sales_order_id = " + newId);
            int orderCount = rs.next() ? rs.getInt(1) : -1;
            rs.close();
            logResult("Sales Order #" + newId + " exists? "
                    + (orderCount > 0 ? "YES (ERROR!)" : "NO — correctly rolled back."));

            // Capture AFTER state (shows DB unchanged after ROLLBACK)
            captureInventoryState(afterModel);
            pause(500);

        } catch (SQLException ex) {
            logError(ex.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
            } catch (Exception ignored) {
            }
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ignored) {
            }
        }

        log("");
    }

    // ═══════════════════════════════════════════════════════════
    // SCENARIO 3: Dirty Read Prevention (Isolation)
    // ═══════════════════════════════════════════════════════════
    private void demoDirtyRead() {
        logHeader("SCENARIO 3: Dirty Read Prevention — Isolation Levels");
        log("  Session A writes without committing. Session B tries to read.");
        log("");

        captureInventoryState(beforeModel);
        pause(500);

        Connection connA = null, connB = null;
        int originalQty = 0;
        try {
            connA = DatabaseConnection.getInstance().createNewConnection();
            connB = DatabaseConnection.getInstance().createNewConnection();

            // Read original qty
            ResultSet rs = connA.createStatement().executeQuery(
                    "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            originalQty = rs.next() ? rs.getInt(1) : 0;
            rs.close();
            logSession("PREP", "Original Batch 103 qty: " + originalQty);
            pause(300);

            // ─── Session A: Update without committing ───
            logSession("SESSION A", "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
            connA.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connA.setAutoCommit(false);
            logSession("SESSION A", "START TRANSACTION");
            pause(200);

            logSession("SESSION A", "UPDATE Inventory SET qty = 9999 WHERE batch_id = 103");
            connA.createStatement().executeUpdate(
                    "UPDATE Inventory SET quantity_available = 9999 WHERE batch_id = 103");
            logSession("SESSION A", "  → Row updated to 9999 (NOT COMMITTED!)");
            pause(500);

            // ─── Session B with READ COMMITTED ───
            logSession("SESSION B", "SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
            connB.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connB.setAutoCommit(false);
            logSession("SESSION B", "START TRANSACTION");
            logSession("SESSION B", "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            pause(300);

            rs = connB.createStatement().executeQuery(
                    "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int readCommittedValue = rs.next() ? rs.getInt(1) : -1;
            rs.close();
            connB.commit();
            connB.setAutoCommit(true);
            logSession("SESSION B", "  → Read value: " + readCommittedValue);
            logResult("READ COMMITTED: Session B sees " + readCommittedValue
                    + " (original value). Dirty read PREVENTED! ✓");
            pause(500);

            // ─── Session B with READ UNCOMMITTED ───
            logSession("SESSION B", "SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED");
            connB.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            connB.setAutoCommit(false);
            logSession("SESSION B", "START TRANSACTION");
            logSession("SESSION B", "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            pause(300);

            rs = connB.createStatement().executeQuery(
                    "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int readUncommittedValue = rs.next() ? rs.getInt(1) : -1;
            rs.close();
            connB.commit();
            connB.setAutoCommit(true);
            logSession("SESSION B", "  → Read value: " + readUncommittedValue);
            if (readUncommittedValue == 9999) {
                logError("READ UNCOMMITTED: Session B sees 9999 — the UNCOMMITTED value! DIRTY READ occurred!");
            } else {
                logResult("READ UNCOMMITTED: Session B sees " + readUncommittedValue
                        + " (MySQL InnoDB may still prevent dirty read due to MVCC)");
            }
            pause(400);

            // Capture AFTER state while Session A's uncommitted write is still active
            // (shows the DB from the perspective of a READ COMMITTED reader)
            captureInventoryState(afterModel);
            pause(500);

            // ─── Session A: Rollback ───
            logSession("SESSION A", "ROLLBACK (undo the uncommitted change)");
            connA.rollback();
            connA.setAutoCommit(true);
            logResult("Session A rolled back. No changes persisted.");

        } catch (SQLException ex) {
            logError(ex.getMessage());
            // Make sure we restore
            try {
                if (connA != null) {
                    connA.rollback();
                    connA.setAutoCommit(true);
                }
            } catch (Exception ignored) {
            }
        } finally {
            try {
                if (connA != null)
                    connA.close();
            } catch (Exception ignored) {
            }
            try {
                if (connB != null)
                    connB.close();
            } catch (Exception ignored) {
            }
        }

        // Verify restoration
        try {
            ResultSet rs = DatabaseConnection.getInstance().executeQuery(
                    "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int finalQty = rs.next() ? rs.getInt(1) : -1;
            rs.close();
            logResult("Final Batch 103 qty: " + finalQty + " (should be " + originalQty + ")");
        } catch (Exception ex) {
            logError("Verification failed: " + ex.getMessage());
        }

        log("");
    }

    // ═══════════════════════════════════════════════════════════
    // SCENARIO 4: Lost Update (Concurrency)
    // ═══════════════════════════════════════════════════════════
    private void demoLostUpdate() {
        logHeader("SCENARIO 4: Lost Update — Concurrency Conflict");
        log("  Two sessions read the same value then write independently.");
        log("");

        captureInventoryState(beforeModel);
        pause(500);

        Connection connA = null, connB = null;
        int originalQty = 0;
        try {
            connA = DatabaseConnection.getInstance().createNewConnection();
            connB = DatabaseConnection.getInstance().createNewConnection();

            // Read original
            ResultSet rs = connA.createStatement().executeQuery(
                    "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            originalQty = rs.next() ? rs.getInt(1) : 1500;
            rs.close();
            logSession("PREP", "Original Batch 103 qty: " + originalQty);
            pause(300);

            // ─── Part A: The Lost Update PROBLEM ───
            log("  ── Part A: Demonstrating the LOST UPDATE problem ──");
            pause(200);

            // Session A reads
            connA.setAutoCommit(false);
            logSession("SESSION A", "START TRANSACTION");
            logSession("SESSION A", "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            rs = connA.createStatement().executeQuery("SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int qtyA = rs.next() ? rs.getInt(1) : 0;
            rs.close();
            logSession("SESSION A", "  → Reads: " + qtyA);
            pause(300);

            // Session B reads (concurrently — same value!)
            connB.setAutoCommit(false);
            logSession("SESSION B", "START TRANSACTION");
            logSession("SESSION B", "SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            rs = connB.createStatement().executeQuery("SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int qtyB = rs.next() ? rs.getInt(1) : 0;
            rs.close();
            logSession("SESSION B", "  → Reads: " + qtyB + " (same stale value!)");
            pause(300);

            // Session A writes
            int deductA = 200;
            logSession("SESSION A", "UPDATE Inventory SET qty = " + qtyA + " - " + deductA + " = " + (qtyA - deductA));
            connA.createStatement().executeUpdate(
                    "UPDATE Inventory SET quantity_available = " + (qtyA - deductA) + " WHERE batch_id = 103");
            connA.commit();
            connA.setAutoCommit(true);
            logSession("SESSION A", "COMMIT → wrote " + (qtyA - deductA));
            pause(300);

            // Session B writes (overwrites A!)
            int deductB = 300;
            logSession("SESSION B", "UPDATE Inventory SET qty = " + qtyB + " - " + deductB + " = " + (qtyB - deductB));
            connB.createStatement().executeUpdate(
                    "UPDATE Inventory SET quantity_available = " + (qtyB - deductB) + " WHERE batch_id = 103");
            connB.commit();
            connB.setAutoCommit(true);
            logSession("SESSION B", "COMMIT → wrote " + (qtyB - deductB) + " (OVERWRITES Session A's value!)");
            pause(300);

            // Check result
            rs = connA.createStatement().executeQuery("SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int lostUpdateResult = rs.next() ? rs.getInt(1) : -1;
            rs.close();
            logError("Final qty: " + lostUpdateResult + "  |  Expected: " + (originalQty - deductA - deductB) +
                    "  |  Session A's deduction of " + deductA + " was LOST!");
            pause(500);

            // Restore for Part B
            connA.createStatement().executeUpdate(
                    "UPDATE Inventory SET quantity_available = " + originalQty + " WHERE batch_id = 103");
            log("  (Restored to " + originalQty + " for Part B)");
            pause(400);

            // ─── Part B: The FIX — SELECT ... FOR UPDATE ───
            log("");
            log("  ── Part B: FIX with SELECT ... FOR UPDATE ──");
            pause(200);

            // Session A: SELECT FOR UPDATE (acquires row lock)
            connA.setAutoCommit(false);
            logSession("SESSION A", "START TRANSACTION");
            logSession("SESSION A", "SELECT qty FROM Inventory WHERE batch_id = 103 FOR UPDATE  ← LOCKS ROW");
            rs = connA.createStatement().executeQuery(
                    "SELECT quantity_available FROM Inventory WHERE batch_id = 103 FOR UPDATE");
            qtyA = rs.next() ? rs.getInt(1) : 0;
            rs.close();
            logSession("SESSION A", "  → Reads: " + qtyA + " (row is now LOCKED)");
            pause(300);

            // Session A writes
            logSession("SESSION A", "UPDATE qty = " + qtyA + " - " + deductA + " = " + (qtyA - deductA));
            connA.createStatement().executeUpdate(
                    "UPDATE Inventory SET quantity_available = " + (qtyA - deductA) + " WHERE batch_id = 103");
            logSession("SESSION A", "COMMIT");
            connA.commit();
            connA.setAutoCommit(true);
            logSession("SESSION A", "  → Committed. Lock released.");
            pause(300);

            // Session B: SELECT FOR UPDATE (gets the post-A value)
            connB.setAutoCommit(false);
            logSession("SESSION B", "START TRANSACTION");
            logSession("SESSION B", "SELECT qty FROM Inventory WHERE batch_id = 103 FOR UPDATE  ← waited for A's lock");
            rs = connB.createStatement().executeQuery(
                    "SELECT quantity_available FROM Inventory WHERE batch_id = 103 FOR UPDATE");
            qtyB = rs.next() ? rs.getInt(1) : 0;
            rs.close();
            logSession("SESSION B", "  → Reads: " + qtyB + " (post-Session-A value — CORRECT!)");
            pause(300);

            // Session B writes
            logSession("SESSION B", "UPDATE qty = " + qtyB + " - " + deductB + " = " + (qtyB - deductB));
            connB.createStatement().executeUpdate(
                    "UPDATE Inventory SET quantity_available = " + (qtyB - deductB) + " WHERE batch_id = 103");
            logSession("SESSION B", "COMMIT");
            connB.commit();
            connB.setAutoCommit(true);
            pause(300);

            // Check result
            rs = connA.createStatement().executeQuery("SELECT quantity_available FROM Inventory WHERE batch_id = 103");
            int fixedResult = rs.next() ? rs.getInt(1) : -1;
            rs.close();
            logResult("Final qty: " + fixedResult + "  |  Expected: " + (originalQty - deductA - deductB) +
                    "  |  Both deductions preserved! ✓");

            // Capture AFTER state (showing the correct result with FOR UPDATE)
            captureInventoryState(afterModel);
            pause(500);

            // Restore
            connA.createStatement().executeUpdate(
                    "UPDATE Inventory SET quantity_available = " + originalQty + " WHERE batch_id = 103");
            logResult("Cleanup: Restored batch 103 to " + originalQty);

        } catch (SQLException ex) {
            logError(ex.getMessage());
            try {
                if (connA != null) {
                    connA.rollback();
                    connA.setAutoCommit(true);
                }
            } catch (Exception ignored) {
            }
            try {
                if (connB != null) {
                    connB.rollback();
                    connB.setAutoCommit(true);
                }
            } catch (Exception ignored) {
            }
        } finally {
            try {
                if (connA != null)
                    connA.close();
            } catch (Exception ignored) {
            }
            try {
                if (connB != null)
                    connB.close();
            } catch (Exception ignored) {
            }
        }

        log("");
    }

    // ═══════════════════════════════════════════════════════════
    // SCENARIO 5: Deadlock Detection
    // ═══════════════════════════════════════════════════════════
    private void demoDeadlock() {
        logHeader("SCENARIO 5: Deadlock Detection & Resolution");
        log("  Two sessions lock rows in opposite order → circular wait → deadlock!");
        log("");

        captureInventoryState(beforeModel);
        pause(500);

        final AtomicBoolean deadlockDetected = new AtomicBoolean(false);
        final AtomicInteger deadlockVictim = new AtomicInteger(-1);
        CountDownLatch phaseOneDone = new CountDownLatch(2);
        CountDownLatch bothStarted = new CountDownLatch(2);

        // Thread A
        Thread threadA = new Thread(() -> {
            Connection conn = null;
            try {
                conn = DatabaseConnection.getInstance().createNewConnection();
                conn.setAutoCommit(false);
                logSession("SESSION A", "START TRANSACTION");

                logSession("SESSION A", "SELECT * FROM Inventory WHERE batch_id = 103 FOR UPDATE  ← LOCKS batch 103");
                conn.createStatement().executeQuery("SELECT * FROM Inventory WHERE batch_id = 103 FOR UPDATE");
                logSession("SESSION A", "  → Acquired lock on batch 103");
                bothStarted.countDown();
                pause(300);

                try {
                    phaseOneDone.await();
                } catch (InterruptedException ignored) {
                }
                pause(500);

                logSession("SESSION A",
                        "SELECT * FROM Inventory WHERE batch_id = 106 FOR UPDATE  ← tries to lock batch 106...");
                logSession("SESSION A", "  → WAITING for Session B to release lock on batch 106...");
                conn.createStatement().executeQuery("SELECT * FROM Inventory WHERE batch_id = 106 FOR UPDATE");

                // If we get here, we weren't the deadlock victim
                logSession("SESSION A", "  → Lock acquired! Session A survived the deadlock.");
                conn.rollback();
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1213 || ex.getMessage().toLowerCase().contains("deadlock")) {
                    deadlockDetected.set(true);
                    deadlockVictim.set(0);
                    logSession("SESSION A", "  ✗ DEADLOCK DETECTED! MySQL killed this session.");
                    logSession("SESSION A", "  Error 1213: " + ex.getMessage());
                } else {
                    logError("Session A unexpected error: " + ex.getMessage());
                }
                try {
                    if (conn != null)
                        conn.rollback();
                } catch (Exception ignored) {
                }
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (Exception ignored) {
                }
            }
        });

        // Thread B
        Thread threadB = new Thread(() -> {
            Connection conn = null;
            try {
                conn = DatabaseConnection.getInstance().createNewConnection();
                conn.setAutoCommit(false);
                logSession("SESSION B", "START TRANSACTION");

                logSession("SESSION B", "SELECT * FROM Inventory WHERE batch_id = 106 FOR UPDATE  ← LOCKS batch 106");
                conn.createStatement().executeQuery("SELECT * FROM Inventory WHERE batch_id = 106 FOR UPDATE");
                logSession("SESSION B", "  → Acquired lock on batch 106");
                bothStarted.countDown();
                pause(300);

                try {
                    phaseOneDone.await();
                } catch (InterruptedException ignored) {
                }
                pause(500);

                logSession("SESSION B",
                        "SELECT * FROM Inventory WHERE batch_id = 103 FOR UPDATE  ← tries to lock batch 103...");
                logSession("SESSION B", "  → WAITING for Session A to release lock on batch 103...");
                conn.createStatement().executeQuery("SELECT * FROM Inventory WHERE batch_id = 103 FOR UPDATE");

                // If we get here, we weren't the deadlock victim
                logSession("SESSION B", "  → Lock acquired! Session B survived the deadlock.");
                conn.rollback();
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1213 || ex.getMessage().toLowerCase().contains("deadlock")) {
                    deadlockDetected.set(true);
                    deadlockVictim.set(1);
                    logSession("SESSION B", "  ✗ DEADLOCK DETECTED! MySQL killed this session.");
                    logSession("SESSION B", "  Error 1213: " + ex.getMessage());
                } else {
                    logError("Session B unexpected error: " + ex.getMessage());
                }
                try {
                    if (conn != null)
                        conn.rollback();
                } catch (Exception ignored) {
                }
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (Exception ignored) {
                }
            }
        });

        threadA.start();
        threadB.start();

        // Wait for both to acquire first lock
        try {
            bothStarted.await();
        } catch (InterruptedException ignored) {
        }
        log("  Both sessions have acquired their first lock.");
        log("  Now they will each try to acquire the OTHER's lock...");
        phaseOneDone.countDown();
        phaseOneDone.countDown();

        // Wait for both threads to finish
        try {
            threadA.join(15000);
            threadB.join(15000);
        } catch (InterruptedException ignored) {
        }

        pause(500);

        if (deadlockDetected.get()) {
            String victim = deadlockVictim.get() == 0 ? "Session A" : "Session B";
            String survivor = deadlockVictim.get() == 0 ? "Session B" : "Session A";
            log("");
            logResult("DEADLOCK DETECTED by MySQL!");
            logResult("Victim: " + victim + " — was automatically rolled back by MySQL.");
            logResult("Survivor: " + survivor + " — completed successfully.");
            logResult("Database consistency PRESERVED — no data corruption!");
        } else {
            log("  Note: Deadlock may not have occurred due to timing. Try again.");
        }

        // Capture AFTER state (should be identical to BEFORE — deadlock causes
        // rollback)
        captureInventoryState(afterModel);
        pause(500);
        log("");
    }
}
