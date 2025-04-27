package com.example.model;

import io.github.cdimascio.dotenv.Dotenv;

// Add this import at the top of the file with other imports
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SafetyService {

    // --- Load Environment Variables ---
    private static final Dotenv dotenv;
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;
    private static final String EMERGENCY_TABLE;
    private static final String WEATHER_TABLE;

    static {
        try {
            // Load .env file
            dotenv = Dotenv.configure().ignoreIfMissing().load();

            DB_URL = dotenv.get("DB_URL");
            DB_USER = dotenv.get("DB_USER");
            DB_PASSWORD = dotenv.get("DB_PASSWORD");
            EMERGENCY_TABLE = "emergencies"; // Table name for emergencies
            WEATHER_TABLE = "weather_alerts"; // Table name for weather alerts

            // Basic validation
            if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
                throw new RuntimeException("Error: One or more required environment variables (DB_URL, DB_USER, DB_PASSWORD) are missing. Check .env file or system environment.");
            }

            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (RuntimeException e) {
            System.err.println("Error during static initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error during static initialization.", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error: MySQL JDBC Driver not found.", e);
        } catch (Exception e) {
            System.err.println("Unexpected error during static initialization: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Unexpected error during static initialization.", e);
        }
    }

    // Singleton pattern
    private static SafetyService instance;

    private SafetyService() {
        // Constructor is empty, initialization happens in static block
    }

    public static synchronized SafetyService getInstance() {
        if (instance == null) {
            instance = new SafetyService();
        }
        return instance;
    }

    // Helper method to get a database connection
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    /**
     * Verifies that the required database tables exist and creates them if they don't
     * @return true if verification/creation was successful
     */
    public boolean verifyDatabaseTables() {
        try (Connection conn = getConnection()) {
            // Check if tables exist
            DatabaseMetaData meta = conn.getMetaData();
            boolean emergencyTableExists = false;
            boolean weatherTableExists = false;
            
            try (ResultSet tables = meta.getTables(null, null, EMERGENCY_TABLE, null)) {
                emergencyTableExists = tables.next();
            }
            
            try (ResultSet tables = meta.getTables(null, null, WEATHER_TABLE, null)) {
                weatherTableExists = tables.next();
            }
            
            // Create tables if they don't exist
            if (!emergencyTableExists) {
                try (Statement stmt = conn.createStatement()) {
                    String sql = "CREATE TABLE IF NOT EXISTS " + EMERGENCY_TABLE + " (" +
                        "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "`type` VARCHAR(100) NOT NULL, " +
                        "`location` VARCHAR(255) NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`severity` INT NOT NULL, " +
                        "`timestamp` DATETIME NOT NULL, " +
                        "`status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                        "INDEX idx_status (status), " +
                        "INDEX idx_severity (severity)" +
                        ")";
                    stmt.executeUpdate(sql);
                    System.out.println("Created emergency table: " + EMERGENCY_TABLE);
                }
            }
            
            if (!weatherTableExists) {
                try (Statement stmt = conn.createStatement()) {
                    String sql = "CREATE TABLE IF NOT EXISTS " + WEATHER_TABLE + " (" +
                        "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "`alert_type` VARCHAR(100) NOT NULL, " +
                        "`description` TEXT NOT NULL, " +
                        "`severity` INT NOT NULL, " +
                        "`timestamp` DATETIME NOT NULL, " +
                        "`active` BOOLEAN NOT NULL DEFAULT TRUE, " +
                        "INDEX idx_active (active), " +
                        "INDEX idx_severity (severity)" +
                        ")";
                    stmt.executeUpdate(sql);
                    System.out.println("Created weather alert table: " + WEATHER_TABLE);
                }
            }
            
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- Emergency Management Methods ---

    /**
     * Creates a new emergency in the database
     * @param emergency The emergency to create
     * @return The created emergency with ID
     */
    public Emergency createEmergency(Emergency emergency) {
        String sql = "INSERT INTO " + EMERGENCY_TABLE + 
                    " (type, location, description, severity, timestamp, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, emergency.getType());
            pstmt.setString(2, emergency.getLocation());
            pstmt.setString(3, emergency.getDescription());
            pstmt.setInt(4, emergency.getSeverity());
            pstmt.setTimestamp(5, Timestamp.valueOf(emergency.getTimestamp()));
            pstmt.setString(6, emergency.getStatus());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Successfully added emergency to database: " + emergency.getType());
            }
            if (affectedRows == 0) {
                throw new SQLException("Creating emergency failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    emergency.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating emergency failed, no ID obtained.");
                }
            }

            return emergency;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets all active emergencies
     * @return List of active emergencies
     */
    public List<Emergency> getActiveEmergencies() {
        List<Emergency> emergencies = new ArrayList<>();
        String sql = "SELECT * FROM " + EMERGENCY_TABLE + 
                    " WHERE status != 'RESOLVED' ORDER BY severity DESC, timestamp DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                emergencies.add(new Emergency(
                    rs.getLong("id"),
                    rs.getString("type"),
                    rs.getString("location"),
                    rs.getString("description"),
                    rs.getInt("severity"),
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return emergencies;
    }

    /**
     * Updates the status of an emergency
     * @param id The emergency ID
     * @param newStatus The new status
     * @return true if successful, false otherwise
     */
    public boolean updateEmergencyStatus(Long id, String newStatus) {
        String sql = "UPDATE " + EMERGENCY_TABLE + " SET status = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setLong(2, id);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes old resolved emergencies
     * @param daysToKeep Number of days of data to keep
     * @return Number of records deleted
     */
    public int deleteOldEmergencies(int daysToKeep) {
        LocalDate cutoffDate = LocalDate.now().minusDays(daysToKeep);
        String sql = "DELETE FROM " + EMERGENCY_TABLE + 
                    " WHERE status = 'RESOLVED' AND DATE(timestamp) < ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(cutoffDate));
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // --- Weather Alert Methods ---

    /**
     * Creates a new weather alert
     * @param alert The weather alert to create
     * @return The created alert with ID
     */
    public WeatherAlert createWeatherAlert(WeatherAlert alert) {
        String sql = "INSERT INTO " + WEATHER_TABLE + 
                    " (alert_type, description, severity, timestamp, active) " +
                    "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, alert.getAlertType());
            pstmt.setString(2, alert.getDescription());
            pstmt.setInt(3, alert.getSeverity());
            pstmt.setTimestamp(4, Timestamp.valueOf(alert.getTimestamp()));
            pstmt.setBoolean(5, alert.isActive());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Successfully added weather alert to database: " + alert.getAlertType());
            }
            if (affectedRows == 0) {
                throw new SQLException("Creating weather alert failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    alert.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating weather alert failed, no ID obtained.");
                }
            }

            return alert;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets all active weather alerts
     * @return List of active weather alerts
     */
    public List<WeatherAlert> getActiveWeatherAlerts() {
        List<WeatherAlert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM " + WEATHER_TABLE + 
                    " WHERE active = true ORDER BY severity DESC, timestamp DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                alerts.add(new WeatherAlert(
                    rs.getLong("id"),
                    rs.getString("alert_type"),
                    rs.getString("description"),
                    rs.getInt("severity"),
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    rs.getBoolean("active")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    /**
     * Deactivates a weather alert
     * @param id The alert ID
     * @return true if successful, false otherwise
     */
    public boolean deactivateWeatherAlert(Long id) {
        String sql = "UPDATE " + WEATHER_TABLE + " SET active = false WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the latest weather alert
     * @return Optional containing the latest alert, or empty if none exists
     */
    public Optional<WeatherAlert> getLatestWeatherAlert() {
        String sql = "SELECT * FROM " + WEATHER_TABLE + 
                    " ORDER BY timestamp DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                WeatherAlert alert = new WeatherAlert(
                    rs.getLong("id"),
                    rs.getString("alert_type"),
                    rs.getString("description"),
                    rs.getInt("severity"),
                    rs.getTimestamp("timestamp").toLocalDateTime(),
                    rs.getBoolean("active")
                );
                return Optional.of(alert);
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}