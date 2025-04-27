package com.example.model;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnvironmentalService {

    private static final Dotenv dotenv;
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;
    private static final String AIR_QUALITY_TABLE;
    private static final String NOISE_LEVEL_TABLE;

    static {
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();

            DB_URL = dotenv.get("DB_URL");
            DB_USER = dotenv.get("DB_USER");
            DB_PASSWORD = dotenv.get("DB_PASSWORD");
            AIR_QUALITY_TABLE = "air_quality_readings";
            NOISE_LEVEL_TABLE = "noise_level_readings";

            if (DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
                throw new RuntimeException("Error: One or more required environment variables (DB_URL, DB_USER, DB_PASSWORD) are missing. Check .env file or system environment.");
            }

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

    private static EnvironmentalService instance;

    private EnvironmentalService() {
    }

    public static synchronized EnvironmentalService getInstance() {
        if (instance == null) {
            instance = new EnvironmentalService();
        }
        return instance;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    public static class AirQualityReading {
        private int id;
        private LocalDateTime timestamp;
        private String location;
        private double pm25Level;
        private double pm10Level;
        private double ozoneLevel;
        private String qualityIndex;

        public AirQualityReading(int id, LocalDateTime timestamp, String location,
                                 double pm25Level, double pm10Level, double ozoneLevel,
                                 String qualityIndex) {
            this.id = id;
            this.timestamp = timestamp;
            this.location = location;
            this.pm25Level = pm25Level;
            this.pm10Level = pm10Level;
            this.ozoneLevel = ozoneLevel;
            this.qualityIndex = qualityIndex;
        }

        public int getId() { return id; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getLocation() { return location; }
        public double getPm25Level() { return pm25Level; }
        public double getPm10Level() { return pm10Level; }
        public double getOzoneLevel() { return ozoneLevel; }
        public String getQualityIndex() { return qualityIndex; }
    }

    public static class NoiseLevelReading {
        private int id;
        private LocalDateTime timestamp;
        private String location;
        private double decibelLevel;
        private String zoneType;
        private boolean exceedsLimit;

        public NoiseLevelReading(int id, LocalDateTime timestamp, String location,
                                 double decibelLevel, String zoneType, boolean exceedsLimit) {
            this.id = id;
            this.timestamp = timestamp;
            this.location = location;
            this.decibelLevel = decibelLevel;
            this.zoneType = zoneType;
            this.exceedsLimit = exceedsLimit;
        }

        public int getId() { return id; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getLocation() { return location; }
        public double getDecibelLevel() { return decibelLevel; }
        public String getZoneType() { return zoneType; }
        public boolean isExceedsLimit() { return exceedsLimit; }
    }

    public Map<String, AirQualityReading> getLatestAirQualityReadings() {
        Map<String, AirQualityReading> latestReadings = new HashMap<>();

        String sql = "SELECT * FROM " + AIR_QUALITY_TABLE +
                     " WHERE (location, timestamp) IN " +
                     "(SELECT location, MAX(timestamp) FROM " + AIR_QUALITY_TABLE +
                     " GROUP BY location)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                String location = rs.getString("location");
                double pm25Level = rs.getDouble("pm25_level");
                double pm10Level = rs.getDouble("pm10_level");
                double ozoneLevel = rs.getDouble("ozone_level");
                String qualityIndex = rs.getString("quality_index");

                AirQualityReading reading = new AirQualityReading(
                    id, timestamp, location, pm25Level, pm10Level, ozoneLevel, qualityIndex
                );
                latestReadings.put(location, reading);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return latestReadings;
    }

    public List<NoiseLevelReading> getLatestNoiseLevelReadings() {
        List<NoiseLevelReading> readings = new ArrayList<>();

        String sql = "SELECT * FROM " + NOISE_LEVEL_TABLE +
                     " WHERE (location, timestamp) IN " +
                     "(SELECT location, MAX(timestamp) FROM " + NOISE_LEVEL_TABLE +
                     " GROUP BY location)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                String location = rs.getString("location");
                double decibelLevel = rs.getDouble("decibel_level");
                String zoneType = rs.getString("zone_type");
                boolean exceedsLimit = rs.getBoolean("exceeds_limit");

                NoiseLevelReading reading = new NoiseLevelReading(
                    id, timestamp, location, decibelLevel, zoneType, exceedsLimit
                );
                readings.add(reading);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return readings;
    }

    public List<AirQualityReading> getAirQualityAlerts(int daysBack) {
        List<AirQualityReading> alerts = new ArrayList<>();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);

        String sql = "SELECT * FROM " + AIR_QUALITY_TABLE +
                     " WHERE timestamp >= ? AND (quality_index = 'Poor' OR quality_index = 'Hazardous')" +
                     " ORDER BY timestamp DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(cutoffDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                    String location = rs.getString("location");
                    double pm25Level = rs.getDouble("pm25_level");
                    double pm10Level = rs.getDouble("pm10_level");
                    double ozoneLevel = rs.getDouble("ozone_level");
                    String qualityIndex = rs.getString("quality_index");

                    AirQualityReading reading = new AirQualityReading(
                        id, timestamp, location, pm25Level, pm10Level, ozoneLevel, qualityIndex
                    );
                    alerts.add(reading);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return alerts;
    }

    public List<NoiseLevelReading> getNoiseViolations(int daysBack) {
        List<NoiseLevelReading> violations = new ArrayList<>();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);

        String sql = "SELECT * FROM " + NOISE_LEVEL_TABLE +
                     " WHERE timestamp >= ? AND exceeds_limit = true" +
                     " ORDER BY timestamp DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(cutoffDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                    String location = rs.getString("location");
                    double decibelLevel = rs.getDouble("decibel_level");
                    String zoneType = rs.getString("zone_type");
                    boolean exceedsLimit = rs.getBoolean("exceeds_limit");

                    NoiseLevelReading reading = new NoiseLevelReading(
                        id, timestamp, location, decibelLevel, zoneType, exceedsLimit
                    );
                    violations.add(reading);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return violations;
    }

    public String deleteOldEnvironmentalData(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        int airQualityRowsDeleted = 0;
        int noiseLevelRowsDeleted = 0;

        try (Connection conn = getConnection()) {
            String airQualitySql = "DELETE FROM " + AIR_QUALITY_TABLE + " WHERE timestamp < ?";
            try (PreparedStatement pstmt = conn.prepareStatement(airQualitySql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(cutoffDate));
                airQualityRowsDeleted = pstmt.executeUpdate();
            }

            String noiseLevelSql = "DELETE FROM " + NOISE_LEVEL_TABLE + " WHERE timestamp < ?";
            try (PreparedStatement pstmt = conn.prepareStatement(noiseLevelSql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(cutoffDate));
                noiseLevelRowsDeleted = pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error deleting old environmental data: " + e.getMessage();
        }

        return String.format("Successfully deleted %d air quality readings and %d noise level readings older than %s.",
                airQualityRowsDeleted, noiseLevelRowsDeleted,
                cutoffDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    public String generateAirQualityReport(String location, int daysBack) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysBack);

        String sql = "SELECT AVG(pm25_level) as avg_pm25, AVG(pm10_level) as avg_pm10, " +
                     "AVG(ozone_level) as avg_ozone, COUNT(*) as reading_count, " +
                     "SUM(CASE WHEN quality_index = 'Good' THEN 1 ELSE 0 END) as good_count, " +
                     "SUM(CASE WHEN quality_index = 'Moderate' THEN 1 ELSE 0 END) as moderate_count, " +
                     "SUM(CASE WHEN quality_index = 'Poor' THEN 1 ELSE 0 END) as poor_count, " +
                     "SUM(CASE WHEN quality_index = 'Hazardous' THEN 1 ELSE 0 END) as hazardous_count " +
                     "FROM " + AIR_QUALITY_TABLE + " WHERE location = ? AND timestamp >= ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, location);
            pstmt.setTimestamp(2, Timestamp.valueOf(cutoffDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double avgPm25 = rs.getDouble("avg_pm25");
                    double avgPm10 = rs.getDouble("avg_pm10");
                    double avgOzone = rs.getDouble("avg_ozone");
                    int readingCount = rs.getInt("reading_count");
                    int goodCount = rs.getInt("good_count");
                    int moderateCount = rs.getInt("moderate_count");
                    int poorCount = rs.getInt("poor_count");
                    int hazardousCount = rs.getInt("hazardous_count");

                    if (readingCount == 0) {
                        return "No air quality data available for " + location + " in the last " + daysBack + " days.";
                    }

                    return String.format(
                        "Air Quality Report for %s (Last %d Days):\n" +
                        "--------------------------------------------------\n" +
                        "Total Readings: %d\n" +
                        "Average PM2.5 Level: %.2f μg/m³\n" +
                        "Average PM10 Level: %.2f μg/m³\n" +
                        "Average Ozone Level: %.2f ppb\n\n" +
                        "Quality Index Distribution:\n" +
                        "  Good: %d (%.1f%%)\n" +
                        "  Moderate: %d (%.1f%%)\n" +
                        "  Poor: %d (%.1f%%)\n" +
                        "  Hazardous: %d (%.1f%%)\n" +
                        "--------------------------------------------------",
                        location, daysBack, readingCount, avgPm25, avgPm10, avgOzone,
                        goodCount, (goodCount * 100.0 / readingCount),
                        moderateCount, (moderateCount * 100.0 / readingCount),
                        poorCount, (poorCount * 100.0 / readingCount),
                        hazardousCount, (hazardousCount * 100.0 / readingCount)
                    );
                } else {
                    return "No air quality data available for " + location + " in the last " + daysBack + " days.";
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Error generating air quality report: " + e.getMessage();
        }
    }
}