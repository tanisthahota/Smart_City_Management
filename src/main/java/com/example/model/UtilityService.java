package com.example.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.cdimascio.dotenv.Dotenv;


public class UtilityService {

    private static final Dotenv dotenv;
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;
    private static final String TABLE_NAME;

    static {
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();

            DB_URL = dotenv.get("DB_URL");
            DB_USER = dotenv.get("DB_USER");
            DB_PASSWORD = dotenv.get("DB_PASSWORD");
            TABLE_NAME = dotenv.get("DB_TABLE");

            if (DB_URL == null || DB_USER == null || DB_PASSWORD == null || TABLE_NAME == null) {
                   throw new RuntimeException("Error: One or more required environment variables (DB_URL, DB_USER, DB_PASSWORD, DB_TABLE) are missing. Check .env file or system environment.");
            }

            Class.forName("com.mysql.cj.jdbc.Driver");

        } catch (RuntimeException e) {
             System.err.println("Error during static initialization (potentially .env loading or validation): " + e.getMessage());
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

    private static UtilityService instance;

    private UtilityService() {
    }

    public static synchronized UtilityService getInstance() {
        if (instance == null) {
            instance = new UtilityService();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public Optional<PowerReading> getLatestReading() {
        String sql = "SELECT id, reading_date, power_consumed, fault_detected FROM " + TABLE_NAME +
                     " ORDER BY reading_date DESC, id DESC LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                int id = rs.getInt("id");
                LocalDate date = rs.getDate("reading_date").toLocalDate();
                double powerConsumed = rs.getDouble("power_consumed");
                boolean faultDetected = rs.getBoolean("fault_detected");

                PowerReading latestReading = new PowerReading(id, date, powerConsumed, faultDetected);
                return Optional.of(latestReading);
            } else {
                return Optional.empty();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public String generateMonthlyReport(YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        String sql = "SELECT reading_date, power_consumed, fault_detected FROM " + TABLE_NAME +
                     " WHERE reading_date BETWEEN ? AND ?";

        double totalConsumption = 0;
        long faultCount = 0;
        int daysRecorded = 0;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(startDate));
            pstmt.setDate(2, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("reading_date").toLocalDate();
                    double powerConsumed = rs.getDouble("power_consumed");
                    boolean faultDetected = rs.getBoolean("fault_detected");

                    totalConsumption += powerConsumed;
                    if (faultDetected) {
                        faultCount++;
                    }
                    daysRecorded++;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error generating report: Database query failed.";
        }

        if (daysRecorded == 0) {
            return "No data available for " + month.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + ".";
        }

        double averageConsumption = totalConsumption / daysRecorded;

        upsertMonthlyStats(month, totalConsumption, faultCount, daysRecorded, averageConsumption);


        return String.format("Power Consumption Report for %s:\n" +
                             "--------------------------------------------------\n" +
                             "Total Days Recorded: %d\n" +
                             "Total Consumption: %.2f kWh\n" +
                             "Average Daily Consumption: %.2f kWh\n" +
                             "Total Fault Days: %d\n" +
                             "--------------------------------------------------",
                             month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                             daysRecorded,
                             totalConsumption,
                             averageConsumption,
                             faultCount);
    }

    private void upsertMonthlyStats(YearMonth month, double totalConsumption, long faultCount, int daysRecorded, double averageConsumption) {
        String yearMonthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String upsertSql = "INSERT INTO power_stats (`year_month`, `total_consumption`, `fault_count`, `days_recorded`, `average_consumption`, `last_updated`) "
                     + "VALUES (?, ?, ?, ?, ?, NOW()) "
                     + "ON DUPLICATE KEY UPDATE "
                     + "`total_consumption` = VALUES(`total_consumption`), "
                     + "`fault_count` = VALUES(`fault_count`), "
                     + "`days_recorded` = VALUES(`days_recorded`), "
                     + "`average_consumption` = VALUES(`average_consumption`), "
                     + "`last_updated` = NOW();";


        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {

            pstmt.setString(1, yearMonthStr);
            pstmt.setDouble(2, totalConsumption);
            pstmt.setLong(3, faultCount);
            pstmt.setInt(4, daysRecorded);
            pstmt.setDouble(5, averageConsumption);

            int rowsAffected = pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating power_stats table for month " + yearMonthStr + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String generateLatestMonthlyReport() {
        String findLatestDateSql = "SELECT MAX(reading_date) FROM " + TABLE_NAME;
        LocalDate latestDate = null;

        try (Connection conn = getConnection();
             PreparedStatement pstmtLatest = conn.prepareStatement(findLatestDateSql);
             ResultSet rs = pstmtLatest.executeQuery()) {

            if (rs.next()) {
                java.sql.Date sqlDate = rs.getDate(1);
                if (sqlDate != null) {
                    latestDate = sqlDate.toLocalDate();
                } else {
                    return "No data found in the table. Cannot generate report.";
                }
            } else {
                   return "No data found in the table. Cannot generate report.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error finding the latest date for report generation: " + e.getMessage();
        }

        YearMonth latestMonth = YearMonth.from(latestDate);

        return generateMonthlyReport(latestMonth);
    }

    public String deleteReadingsBeforeLatestMonth() {
        String findLatestDateSql = "SELECT MAX(reading_date) FROM " + TABLE_NAME;
        LocalDate latestDate = null;

        try (Connection conn = getConnection();
             PreparedStatement pstmtLatest = conn.prepareStatement(findLatestDateSql);
             ResultSet rs = pstmtLatest.executeQuery()) {

            if (rs.next()) {
                java.sql.Date sqlDate = rs.getDate(1);
                if (sqlDate != null) {
                    latestDate = sqlDate.toLocalDate();
                } else {
                    return "No data found in the table. Nothing to delete.";
                }
            } else {
                return "No data found in the table. Nothing to delete.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error finding the latest date for deletion: " + e.getMessage();
        }

        LocalDate deleteBeforeDate = YearMonth.from(latestDate).atDay(1);

        if (latestDate.equals(deleteBeforeDate)) {
             return "Latest data is from the first day of the month. No older data to delete before this month.";
        }

        String deleteSql = "DELETE FROM " + TABLE_NAME + " WHERE reading_date < ?";
        int rowsDeleted = 0;

        try (Connection conn = getConnection();
             PreparedStatement pstmtDelete = conn.prepareStatement(deleteSql)) {

            pstmtDelete.setDate(1, java.sql.Date.valueOf(deleteBeforeDate));

            rowsDeleted = pstmtDelete.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error deleting old data: " + e.getMessage();
        }

        return String.format("Successfully deleted %d reading(s) before %s.",
                             rowsDeleted,
                             deleteBeforeDate.format(DateTimeFormatter.ISO_DATE));
    }

    public List<PowerReading> findRecentFaults(int days) {
        List<PowerReading> faultReadings = new ArrayList<>();
        LocalDate sinceDate = LocalDate.now().minusDays(days);
        String sql = "SELECT id, reading_date, power_consumed, fault_detected FROM " + TABLE_NAME +
                     " WHERE fault_detected = true AND reading_date >= ? ORDER BY reading_date DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDate(1, java.sql.Date.valueOf(sinceDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    faultReadings.add(new PowerReading(
                           rs.getInt("id"),
                           rs.getDate("reading_date").toLocalDate(),
                           rs.getDouble("power_consumed"),
                           rs.getBoolean("fault_detected")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return faultReadings;
    }

}