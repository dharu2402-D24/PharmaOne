package db;

import java.sql.*;
import javax.swing.*;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private String host = "localhost";
    private int port = 3306;
    private String database = "pharma_db";
    private String username = "root";
    private String password = "";

    private DatabaseConnection() {
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public boolean connect() {
        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            connection = DriverManager.getConnection(url, username, password);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean showConnectionDialog(java.awt.Component parent) {
        JPanel panel = new JPanel(new java.awt.GridLayout(5, 2, 8, 8));
        JTextField hostField = new JTextField(host);
        JTextField portField = new JTextField(String.valueOf(port));
        JTextField dbField = new JTextField(database);
        JTextField userField = new JTextField(username);
        JPasswordField passField = new JPasswordField(password);

        panel.add(new JLabel("Host:"));
        panel.add(hostField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);
        panel.add(new JLabel("Database:"));
        panel.add(dbField);
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);

        int result = JOptionPane.showConfirmDialog(parent, panel,
                "MySQL Database Connection", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            host = hostField.getText().trim();
            port = Integer.parseInt(portField.getText().trim());
            database = dbField.getText().trim();
            username = userField.getText().trim();
            password = new String(passField.getPassword());
            return connect();
        }
        return false;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        return stmt.executeQuery(sql);
    }

    public int executeUpdate(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        return stmt.executeUpdate(sql);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return getConnection().prepareStatement(sql);
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a NEW independent database connection (separate MySQL session).
     * Used for transaction/concurrency demos where multiple sessions are needed.
     * The caller is responsible for closing this connection.
     */
    public Connection createNewConnection() throws SQLException {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, username, password);
    }

    // Getters for settings display
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    // Automated expired batch lifecycle cleanup — runs on startup.

    public void runExpiredBatchCleanup() {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);

            // 1. Insert newly expired batches into Discarded_Batch (skip those already
            // transferred)
            String insertSql = "INSERT INTO Discarded_Batch (batch_id, medicine_id, quantity_discarded) " +
                    "SELECT b.batch_id, b.medicine_id, " +
                    "COALESCE(SUM(i.quantity_available), 0) AS quantity_discarded " +
                    "FROM Batch b LEFT JOIN Inventory i ON b.batch_id = i.batch_id " +
                    "WHERE b.expiry_date < CURDATE() AND b.status = 'Active' " +
                    "AND b.batch_id NOT IN (SELECT d.batch_id FROM Discarded_Batch d WHERE d.batch_id IS NOT NULL) " +
                    "GROUP BY b.batch_id, b.medicine_id";
            Statement stmt1 = conn.createStatement();
            stmt1.executeUpdate(insertSql);

            // 2. Mark expired batches as Discarded
            String updateSql = "UPDATE Batch SET status = 'Discarded' WHERE expiry_date < CURDATE() AND status = 'Active'";
            Statement stmt2 = conn.createStatement();
            stmt2.executeUpdate(updateSql);

            // 3. Remove inventory for discarded batches
            String deleteSql = "DELETE FROM Inventory WHERE batch_id IN (SELECT batch_id FROM Batch WHERE status = 'Discarded')";
            Statement stmt3 = conn.createStatement();
            stmt3.executeUpdate(deleteSql);

            conn.commit();
            conn.setAutoCommit(true);
            System.out.println("[PharmaOne] Expired batch cleanup completed successfully.");
        } catch (SQLException e) {
            System.err.println("[PharmaOne] Expired batch cleanup error: " + e.getMessage());
            try {
                getConnection().rollback();
                getConnection().setAutoCommit(true);
            } catch (Exception ignored) {
            }
        }
    }
}
