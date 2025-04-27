import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
// Removed Timestamp and Instant imports as they are no longer needed for traffic data
// import java.sql.Timestamp;
// import java.time.Instant;
// Removed Arrays and List imports as SIGNAL_LOCATIONS is removed
// import java.util.Arrays;
// import java.util.List;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// Import Dotenv
import io.github.cdimascio.dotenv.Dotenv;

public class TrafficDataGenerator {

    // --- Data Generation Parameters ---
    // Removed SIGNAL_LOCATIONS list
    private static final int JUNCTION_LANES = 4; // Number of approaches/lanes at the junction
    private static final int MAX_VEHICLES_PER_LANE = 30; // Max vehicles detected in a lane at once
    private static final int PARKING_SPOT_UPDATE_CHANCE_PERCENT = 100; // 100% chance each cycle to update a parking spot
    private static final String JUNCTION_ID = "MainJunction"; // Identifier for our single junction

    // --- Define Initial Parking Spots ---
    // Updated to 5 simple spots
    private static final Map<String, String> INITIAL_PARKING_SPOTS = Map.of(
            "Spot-1", "Parking Spot 1",
            "Spot-2", "Parking Spot 2",
            "Spot-3", "Parking Spot 3",
            "Spot-4", "Parking Spot 4",
            "Spot-5", "Parking Spot 5"
    );


    // --- Database Table Names (read from .env) ---
    // Removed TRAFFIC_TABLE_NAME as it's no longer used by this generator
    // private static String TRAFFIC_TABLE_NAME = "traffic_readings";
    private static String PARKING_TABLE_NAME = "parking_spots";   // Default, will be overridden by .env
    private static String JUNCTION_TABLE_NAME = "junction_state"; // Default, will be overridden by .env

    public static void main(String[] args) {
        // Load environment variables from .env file
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure().load();
        } catch (Exception e) {
            System.err.println("Error loading .env file. Make sure it exists in the project root.");
            System.err.println("Details: " + e.getMessage());
            System.exit(1);
        }

        // Get DB config from Dotenv
        final String dbUrl = dotenv.get("DB_URL");
        final String dbUser = dotenv.get("DB_USER");
        final String dbPassword = dotenv.get("DB_PASSWORD");
        // Get table names from .env, use defaults if not found
        PARKING_TABLE_NAME = dotenv.get("DB_PARKING_TABLE", PARKING_TABLE_NAME);
        JUNCTION_TABLE_NAME = dotenv.get("DB_JUNCTION_TABLE", JUNCTION_TABLE_NAME); // Read junction table name


        // Basic validation
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("Error: One or more required environment variables (DB_URL, DB_USER, DB_PASSWORD) are missing in the .env file.");
            System.exit(1);
        }

        Random random = new Random();

        System.out.println("Starting traffic data generator...");
        System.out.println("Target Junction State Table: " + JUNCTION_TABLE_NAME);
        System.out.println("Target Parking Table: " + PARKING_TABLE_NAME);
        System.out.println("Connecting to database: " + dbUrl);

        // Load the MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: MySQL JDBC Driver not found. Make sure it's in the classpath.");
            e.printStackTrace();
            return;
        }

        // SQL Statements
        // Removed insertTrafficSql and deleteOldTrafficSql

        // Use INSERT IGNORE to avoid errors if spots already exist
        String insertParkingSql = "INSERT IGNORE INTO " + PARKING_TABLE_NAME +
                                  " (spot_id, location_description, is_occupied) VALUES (?, ?, ?)";
        String updateParkingSql = "UPDATE " + PARKING_TABLE_NAME +
                                  " SET is_occupied = ?, last_updated = CURRENT_TIMESTAMP" +
                                  " WHERE spot_id = (SELECT spot_id FROM (SELECT spot_id FROM " + PARKING_TABLE_NAME + " ORDER BY RAND() LIMIT 1) AS temp)";

        // SQL to update the junction state (or insert if it doesn't exist)
        // Uses INSERT ... ON DUPLICATE KEY UPDATE to handle both initial creation and subsequent updates
        String updateJunctionSql = "INSERT INTO " + JUNCTION_TABLE_NAME +
                                   " (junction_id, lane_1_vehicles, lane_2_vehicles, lane_3_vehicles, lane_4_vehicles, green_lane_id) " +
                                   " VALUES (?, ?, ?, ?, ?, ?) " +
                                   " ON DUPLICATE KEY UPDATE " +
                                   " lane_1_vehicles = VALUES(lane_1_vehicles), " +
                                   " lane_2_vehicles = VALUES(lane_2_vehicles), " +
                                   " lane_3_vehicles = VALUES(lane_3_vehicles), " +
                                   " lane_4_vehicles = VALUES(lane_4_vehicles), " +
                                   " green_lane_id = VALUES(green_lane_id)";


        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            System.out.println("Database connection successful.");
            conn.setAutoCommit(false); // Use transactions

            // --- Initial Parking Spot Population ---
            System.out.println("Attempting to populate initial parking spots (if they don't exist)...");
            try (PreparedStatement insertParkingStmt = conn.prepareStatement(insertParkingSql)) {
                int spotsAdded = 0;
                for (Map.Entry<String, String> entry : INITIAL_PARKING_SPOTS.entrySet()) {
                    insertParkingStmt.setString(1, entry.getKey());    // spot_id
                    insertParkingStmt.setString(2, entry.getValue());  // location_description
                    insertParkingStmt.setBoolean(3, random.nextBoolean()); // Set initial random occupancy
                    insertParkingStmt.addBatch();
                    spotsAdded++;
                }
                int[] initialParkingResults = insertParkingStmt.executeBatch();
                // Note: executeBatch with INSERT IGNORE returns 1 for successful insert, 0 for ignored duplicate.
                // We can't easily tell how many were *newly* added vs ignored without extra queries.
                System.out.println("Initial parking spot population attempt complete for " + spotsAdded + " defined spots.");
                conn.commit(); // Commit initial parking spots
            } catch (SQLException e) {
                System.err.println("Error during initial parking spot population. Rolling back.");
                try { conn.rollback(); } catch (SQLException ex) { /* ignore rollback error */ }
                e.printStackTrace();
                // Decide if you want to exit or continue without initial spots
                // System.exit(1);
            }
            // --- End Initial Parking Spot Population ---


            // Updated try-with-resources to include updateJunctionStmt
            try (PreparedStatement updateParkingStmt = conn.prepareStatement(updateParkingSql);
                 PreparedStatement updateJunctionStmt = conn.prepareStatement(updateJunctionSql)) {

                // Removed deleteCounter

                while (true) {
                    // Removed timestamp generation as it's handled by DB now

                    // --- Generate Junction State Data ---
                    int[] vehicleCounts = new int[JUNCTION_LANES];
                    int maxVehicles = -1;
                    int greenLane = 1; // Default to lane 1

                    System.out.print("Generated Vehicle Counts: ");
                    for (int i = 0; i < JUNCTION_LANES; i++) {
                        vehicleCounts[i] = random.nextInt(MAX_VEHICLES_PER_LANE + 1);
                        System.out.printf("Lane %d: %d | ", i + 1, vehicleCounts[i]);
                        if (vehicleCounts[i] > maxVehicles) {
                            maxVehicles = vehicleCounts[i];
                            greenLane = i + 1; // Lane IDs are 1-based
                        }
                    }
                    System.out.printf("=> Green Light for Lane: %d%n", greenLane);

                    // Prepare the junction update statement
                    updateJunctionStmt.setString(1, JUNCTION_ID); // The fixed ID of our junction
                    for (int i = 0; i < JUNCTION_LANES; i++) {
                        updateJunctionStmt.setInt(i + 2, vehicleCounts[i]); // Parameters 2, 3, 4, 5 for lanes 1-4
                    }
                    updateJunctionStmt.setInt(JUNCTION_LANES + 2, greenLane); // Last parameter for green_lane_id

                    updateJunctionStmt.executeUpdate(); // Execute the insert/update
                    // --- End Generate Junction State Data ---


                    // --- Simulate Parking Spot Update ---
                    if (random.nextInt(100) < PARKING_SPOT_UPDATE_CHANCE_PERCENT) {
                        boolean newOccupiedStatus = random.nextBoolean();
                        updateParkingStmt.setBoolean(1, newOccupiedStatus);
                        int updatedSpots = updateParkingStmt.executeUpdate();
                        if (updatedSpots > 0) {
                            System.out.println("Updated a random parking spot status to: " + newOccupiedStatus);
                        } else {
                             // This message might appear if the table is empty initially, which shouldn't happen after the population step
                             System.out.println("Attempted parking spot update, but no spots found or error occurred.");
                        }
                    }

                    // --- Periodic Cleanup ---
                    // Removed the old traffic_readings cleanup logic

                    conn.commit(); // Commit transaction

                    // Wait before generating next batch of data
                    TimeUnit.SECONDS.sleep(5); // Generate data every 5 seconds (adjust as needed)

                }
            } catch (SQLException e) {
                System.err.println("Error during statement execution. Rolling back transaction.");
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error during rollback: " + ex.getMessage());
                }
                e.printStackTrace();
            } catch (InterruptedException e) {
                System.err.println("Data generator interrupted.");
                Thread.currentThread().interrupt();
            }

        } catch (SQLException e) {
            System.err.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}