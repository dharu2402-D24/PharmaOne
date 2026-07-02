import com.formdev.flatlaf.FlatLightLaf;
import db.DatabaseConnection;
import ui.MainFrame;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Set FlatLaf look and feel
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("TabbedPane.selectedBackground", new java.awt.Color(219, 234, 254));
            UIManager.put("TabbedPane.selectedForeground", new java.awt.Color(59, 130, 246));
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            // Show connection dialog
            DatabaseConnection db = DatabaseConnection.getInstance();
            boolean connected = db.showConnectionDialog(null);

            if (!connected) {
                int choice = JOptionPane.showConfirmDialog(null,
                    "Failed to connect to MySQL.\nContinue without database connection?",
                    "Connection Failed", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
            if (connected) {
                // Run expired batch cleanup on startup
                db.runExpiredBatchCleanup();
            }

            // Launch main frame
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
