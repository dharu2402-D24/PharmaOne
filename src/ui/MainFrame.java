package ui;

import util.ThemeColors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private HeaderPanel headerPanel;
    private Sidebar sidebar;
    private StatusBar statusBar;

    // Panel references
    private DashboardPanel dashboardPanel;
    private InventoryPanel inventoryPanel;
    private OrdersPanel ordersPanel;
    private SuppliersPanel suppliersPanel;
    private CustomersPanel customersPanel;
    private ReportsPanel reportsPanel;
    private SettingsPanel settingsPanel;
    private SQLConsolePanel sqlConsolePanel;
    private TriggerDemoPanel triggerDemoPanel;
    private DiscardedBatchesPanel discardedBatchesPanel;
    private TransactionDemoPanel transactionDemoPanel;

    public MainFrame() {
        setTitle("PharmaOne - Inventory Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        setIconImage(util.CustomIcons.getPharmaLogoImage(64));

        // Main layout
        JPanel root = new JPanel(new BorderLayout());

        // Sidebar
        sidebar = new Sidebar(this);
        root.add(sidebar, BorderLayout.WEST);

        // Center: header + content
        JPanel centerPanel = new JPanel(new BorderLayout());

        headerPanel = new HeaderPanel(this);
        centerPanel.add(headerPanel, BorderLayout.NORTH);

        // Content area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(ThemeColors.BG_MAIN);

        dashboardPanel = new DashboardPanel();
        inventoryPanel = new InventoryPanel();
        ordersPanel = new OrdersPanel();
        suppliersPanel = new SuppliersPanel();
        customersPanel = new CustomersPanel();
        reportsPanel = new ReportsPanel();
        settingsPanel = new SettingsPanel();
        sqlConsolePanel = new SQLConsolePanel();
        triggerDemoPanel = new TriggerDemoPanel();
        discardedBatchesPanel = new DiscardedBatchesPanel();
        transactionDemoPanel = new TransactionDemoPanel();

        // Wire up dashboard refresh callback
        ordersPanel.setDashboardRefreshCallback(() -> dashboardPanel.refreshData());

        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(inventoryPanel, "Inventory");
        contentPanel.add(ordersPanel, "Orders");
        contentPanel.add(suppliersPanel, "Suppliers");
        contentPanel.add(customersPanel, "Customers");
        contentPanel.add(reportsPanel, "Reports");
        contentPanel.add(settingsPanel, "Settings");
        contentPanel.add(sqlConsolePanel, "SQL Console");
        contentPanel.add(triggerDemoPanel, "Trigger Demo");
        contentPanel.add(discardedBatchesPanel, "Discarded");
        contentPanel.add(transactionDemoPanel, "Transactions");

        centerPanel.add(contentPanel, BorderLayout.CENTER);
        root.add(centerPanel, BorderLayout.CENTER);

        // Status bar
        statusBar = new StatusBar();
        root.add(statusBar, BorderLayout.SOUTH);

        setContentPane(root);

        // Keyboard shortcuts
        setupKeyboardShortcuts();
    }

    public void showPanel(String name) {
        cardLayout.show(contentPanel, name);
        switch (name) {
            case "Dashboard" -> headerPanel.setTitle("Dashboard", "Modern pharma inventory management dashboard");
            case "Inventory" ->
                headerPanel.setTitle("Inventory Catalog", "Manage pharmaceutical stock and batch details");
            case "Orders" -> headerPanel.setTitle("Orders & Shipments", "Manage inbound and outbound logistics");
            case "Suppliers" -> headerPanel.setTitle("Supplier Directory", "Manage pharmaceutical suppliers");
            case "Customers" -> headerPanel.setTitle("Customer Directory", "Manage pharmaceutical customers");
            case "Reports" ->
                headerPanel.setTitle("Analytics & Reports", "Track performance, compliance, and forecasting");
            case "Settings" -> headerPanel.setTitle("System Configuration", "Manage platform settings and connections");
            case "SQL Console" -> headerPanel.setTitle("SQL Console", "Execute queries against pharma_db");
            case "Trigger Demo" -> headerPanel.setTitle("Trigger Demonstrations", "Demonstrate database triggers");
            case "Discarded" ->
                headerPanel.setTitle("Discarded Batches", "Archived expired batches — lifecycle management");
            case "Transactions" ->
                headerPanel.setTitle("Transaction Demos", "Database transaction & concurrency demonstrations");
        }
        statusBar.updateStatus();
    }

    public void refreshAllPanels() {
        dashboardPanel.refreshData();
        inventoryPanel.refreshData();
        ordersPanel.refreshData();
        suppliersPanel.refreshData();
        customersPanel.refreshData();
        reportsPanel.refreshData();
        statusBar.updateStatus();
        JOptionPane.showMessageDialog(this, "All data refreshed from database!", "Refresh Complete",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void setupKeyboardShortcuts() {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        getRootPane().getActionMap().put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshAllPanels();
            }
        });
    }

}
