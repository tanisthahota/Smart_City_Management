import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;

public class EnvironmentalDataGenerator {
    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3306/smart_city_db";
    private static String DB_USER = "root";
    private static String DB_PASSWORD = "";

    // Air quality locations
    private static final String[] AIR_QUALITY_LOCATIONS = {
        "Downtown", "Industrial Zone", "Residential Area", "City Park", 
        "Shopping District", "University Campus", "Suburban Area", "Highway Junction"
    };
    
    // Noise monitoring locations
    private static final String[] NOISE_LOCATIONS = {
        "Main Street", "Hospital Zone", "School Zone", "Entertainment District", 
        "Residential Complex", "Industrial Park", "Airport Vicinity", "Railway Station"
    };
    
    // Zone types for noise monitoring
    private static final String[] ZONE_TYPES = {
        "Residential", "Commercial", "Industrial", "Silence Zone"
    };
    
    // Noise limits by zone type (in decibels)
    private static final double[] ZONE_LIMITS = {
        55.0,  // Residential
        65.0,  // Commercial
        75.0,  // Industrial
        50.0   // Silence Zone
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Get database credentials
        System.out.print("Enter database username (default: root): ");
        String inputUser = scanner.nextLine().trim();
        if (!inputUser.isEmpty()) {
            DB_USER = inputUser;
        }
        
        System.out.print("Enter database password: ");
        DB_PASSWORD = scanner.nextLine();
        
        // Get number of days to generate data for
        System.out.print("Enter number of days to generate data for (default: 30): ");
        String daysInput = scanner.nextLine().trim();
        int days = daysInput.isEmpty() ? 30 : Integer.parseInt(daysInput);
        
        // Get readings per day
        System.out.print("Enter readings per day for each location (default: 4): ");
        String readingsInput = scanner.nextLine().trim();
        int readingsPerDay = readingsInput.isEmpty() ? 4 : Integer.parseInt(readingsInput);
        
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Generate and insert data
            generateAirQualityData(days, readingsPerDay);
            generateNoiseLevelData(days, readingsPerDay);
            
            System.out.println("Data generation completed successfully!");
            
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    private static void generateAirQualityData(int days, int readingsPerDay) throws SQLException {
        System.out.println("Generating air quality data...");
        Random random = new Random();
        
        // Calculate total number of readings
        int totalReadings = AIR_QUALITY_LOCATIONS.length * days * readingsPerDay;
        int completedReadings = 0;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO air_quality_readings (timestamp, location, pm25_level, pm10_level, ozone_level, quality_index) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // Generate data for each day
                for (int day = 0; day < days; day++) {
                    LocalDateTime date = LocalDateTime.now().minusDays(days - day);
                    
                    // Generate readings for each location
                    for (String location : AIR_QUALITY_LOCATIONS) {
                        // Base values for this location (some locations are cleaner than others)
                        double basePm25 = getBaseValue(location, 10, 30);
                        double basePm10 = getBaseValue(location, 20, 50);
                        double baseOzone = getBaseValue(location, 30, 70);
                        
                        // Generate readings throughout the day
                        for (int reading = 0; reading < readingsPerDay; reading++) {
                            // Add some hours to spread readings throughout the day
                            LocalDateTime timestamp = date.plusHours(reading * (24 / readingsPerDay));
                            
                            // Generate values with some randomness
                            double pm25 = basePm25 + (random.nextDouble() * 15) - 7.5;
                            double pm10 = basePm10 + (random.nextDouble() * 25) - 12.5;
                            double ozone = baseOzone + (random.nextDouble() * 20) - 10;
                            
                            // Ensure values are positive
                            pm25 = Math.max(1.0, pm25);
                            pm10 = Math.max(2.0, pm10);
                            ozone = Math.max(5.0, ozone);
                            
                            // Determine quality index based on values
                            String qualityIndex = determineAirQualityIndex(pm25, pm10, ozone);
                            
                            // Set parameters and execute
                            pstmt.setObject(1, timestamp);
                            pstmt.setString(2, location);
                            pstmt.setDouble(3, pm25);
                            pstmt.setDouble(4, pm10);
                            pstmt.setDouble(5, ozone);
                            pstmt.setString(6, qualityIndex);
                            pstmt.executeUpdate();
                            
                            completedReadings++;
                            if (completedReadings % 100 == 0 || completedReadings == totalReadings) {
                                System.out.printf("Air quality progress: %d/%d (%.1f%%)\n", 
                                    completedReadings, totalReadings, 
                                    (completedReadings * 100.0 / totalReadings));
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("Air quality data generation completed!");
    }
    
    private static void generateNoiseLevelData(int days, int readingsPerDay) throws SQLException {
        System.out.println("Generating noise level data...");
        Random random = new Random();
        
        // Calculate total number of readings
        int totalReadings = NOISE_LOCATIONS.length * days * readingsPerDay;
        int completedReadings = 0;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "INSERT INTO noise_level_readings (timestamp, location, decibel_level, zone_type, exceeds_limit) " +
                         "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // Generate data for each day
                for (int day = 0; day < days; day++) {
                    LocalDateTime date = LocalDateTime.now().minusDays(days - day);
                    
                    // Generate readings for each location
                    for (int locIndex = 0; locIndex < NOISE_LOCATIONS.length; locIndex++) {
                        String location = NOISE_LOCATIONS[locIndex];
                        
                        // Assign a zone type to this location
                        String zoneType = ZONE_TYPES[locIndex % ZONE_TYPES.length];
                        double zoneLimit = ZONE_LIMITS[locIndex % ZONE_TYPES.length];
                        
                        // Base noise level for this location
                        double baseNoiseLevel = getBaseNoiseLevel(location, zoneType);
                        
                        // Generate readings throughout the day
                        for (int reading = 0; reading < readingsPerDay; reading++) {
                            // Add some hours to spread readings throughout the day
                            LocalDateTime timestamp = date.plusHours(reading * (24 / readingsPerDay));
                            
                            // Generate noise level with some randomness
                            // More variation during day, less at night
                            double hourFactor = timestamp.getHour() >= 8 && timestamp.getHour() <= 20 ? 1.2 : 0.7;
                            double noiseLevel = baseNoiseLevel * hourFactor + (random.nextDouble() * 15) - 7.5;
                            
                            // Ensure value is positive
                            noiseLevel = Math.max(30.0, noiseLevel);
                            
                            // Determine if it exceeds the limit
                            boolean exceedsLimit = noiseLevel > zoneLimit;
                            
                            // Set parameters and execute
                            pstmt.setObject(1, timestamp);
                            pstmt.setString(2, location);
                            pstmt.setDouble(3, noiseLevel);
                            pstmt.setString(4, zoneType);
                            pstmt.setBoolean(5, exceedsLimit);
                            pstmt.executeUpdate();
                            
                            completedReadings++;
                            if (completedReadings % 100 == 0 || completedReadings == totalReadings) {
                                System.out.printf("Noise level progress: %d/%d (%.1f%%)\n", 
                                    completedReadings, totalReadings, 
                                    (completedReadings * 100.0 / totalReadings));
                            }
                        }
                    }
                }
            }
        }
        
        System.out.println("Noise level data generation completed!");
    }
    
    // Helper method to determine air quality index based on pollutant levels
    private static String determineAirQualityIndex(double pm25, double pm10, double ozone) {
        // Simple algorithm to determine air quality
        // In a real system, this would follow established AQI calculation methods
        
        if (pm25 > 35 || pm10 > 150 || ozone > 70) {
            return "Hazardous";
        } else if (pm25 > 25 || pm10 > 100 || ozone > 55) {
            return "Poor";
        } else if (pm25 > 15 || pm10 > 50 || ozone > 40) {
            return "Moderate";
        } else {
            return "Good";
        }
    }
    
    // Helper method to get base value for a location
    private static double getBaseValue(String location, double min, double max) {
        // Different locations have different baseline pollution levels
        switch (location) {
            case "Industrial Zone":
                return max * 0.9; // Higher pollution
            case "Highway Junction":
                return max * 0.8;
            case "Downtown":
                return (min + max) / 2;
            case "Shopping District":
                return (min + max) / 2;
            case "Residential Area":
                return min * 1.5;
            case "Suburban Area":
                return min * 1.3;
            case "University Campus":
                return min * 1.2;
            case "City Park":
                return min; // Lowest pollution
            default:
                return (min + max) / 2;
        }
    }
    
    // Helper method to get base noise level for a location and zone type
    private static double getBaseNoiseLevel(String location, String zoneType) {
        // Base level depends on location
        double baseLevel;
        
        switch (location) {
            case "Entertainment District":
                baseLevel = 70.0;
                break;
            case "Main Street":
            case "Industrial Park":
                baseLevel = 65.0;
                break;
            case "Railway Station":
            case "Airport Vicinity":
                baseLevel = 75.0;
                break;
            case "Residential Complex":
                baseLevel = 50.0;
                break;
            case "Hospital Zone":
            case "School Zone":
                baseLevel = 45.0;
                break;
            default:
                baseLevel = 55.0;
        }
        
        // Adjust based on zone type
        switch (zoneType) {
            case "Industrial":
                return baseLevel * 1.1;
            case "Commercial":
                return baseLevel * 1.0;
            case "Residential":
                return baseLevel * 0.9;
            case "Silence Zone":
                return baseLevel * 0.8;
            default:
                return baseLevel;
        }
    }
}