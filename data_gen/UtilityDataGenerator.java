import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet; // <-- Add import for ResultSet
import java.sql.SQLException;
import java.sql.Statement; // <-- Add import for Statement
import java.time.LocalDate;
import java.util.Random;
import java.util.concurrent.TimeUnit;

// Import Dotenv
import io.github.cdimascio.dotenv.Dotenv;
// import io.github.cdimascio.dotenv.DotenvException; // Commented out as requested


// Renamed class
public class UtilityDataGenerator {

    // Remove hardcoded DB constants
    // private static final String DB_URL = ...
    // private static final String DB_USER = ...
    // private static final String DB_PASSWORD = ...
    // private static final String TABLE_NAME = ...


    // --- Data Generation Parameters ---
    private static final double MIN_POWER = 10.0;
    private static final double MAX_POWER_RANGE = 5.0; // Max power = MIN_POWER + MAX_POWER_RANGE
    private static final int FAULT_CHANCE_PERCENT = 5; // 5% chance of fault

    public static void main(String[] args) {
        // Load environment variables from .env file
        Dotenv dotenv = null;
        try {
            // Load .env file from the current directory (project root)
            dotenv = Dotenv.configure().load();
        // } catch (DotenvException e) { // Original catch block
        } catch (Exception e) { // Catch general Exception instead
            System.err.println("Error loading .env file. Make sure it exists in the project root.");
            // Consider logging the specific exception type if needed for debugging
            // System.err.println("Exception type: " + e.getClass().getName());
            System.err.println("Details: " + e.getMessage());
            System.exit(1); // Exit if config is missing
        }

        // Get DB config from Dotenv
        final String dbUrl = dotenv.get("DB_URL");
        final String dbUser = dotenv.get("DB_USER");
        final String dbPassword = dotenv.get("DB_PASSWORD");
        final String tableName = dotenv.get("DB_TABLE");

        // Basic validation
        if (dbUrl == null || dbUser == null || dbPassword == null || tableName == null) {
            System.err.println("Error: One or more required environment variables (DB_URL, DB_USER, DB_PASSWORD, DB_TABLE) are missing in the .env file.");
            System.exit(1);
        }


        Random random = new Random();
        // Remove the initial assignment here, we'll set it after checking the DB
        // LocalDate currentDate = LocalDate.now().minusMonths(3);
        LocalDate currentDate; // Declare currentDate

        System.out.println("Starting utility data generator...");
        System.out.println("Connecting to database: " + dbUrl);

        // Load the MySQL JDBC driver
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: MySQL JDBC Driver not found. Make sure it's in the classpath.");
            e.printStackTrace();
            return;
        }

        // SQL Statements remain the same, using loaded tableName
        String insertSql = "INSERT INTO " + tableName + " (reading_date, power_consumed, fault_detected) VALUES (?, ?, ?)";
        String deleteSql = "DELETE FROM " + tableName + " WHERE reading_date < DATE_SUB(CURDATE(), INTERVAL 1 YEAR)";
        String latestDateSql = "SELECT MAX(reading_date) AS latest_date FROM " + tableName; // <-- Query to get the latest date

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            System.out.println("Database connection successful.");

            // --- Determine the starting date ---
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(latestDateSql)) {
                if (rs.next() && rs.getDate("latest_date") != null) {
                    LocalDate latestDateFromDb = rs.getDate("latest_date").toLocalDate();
                    currentDate = latestDateFromDb.plusDays(1); // Start from the day after the latest entry
                    System.out.println("Found latest date in DB: " + latestDateFromDb + ". Starting generation from: " + currentDate);
                } else {
                    // Table is empty or latest date is null, use default start date
                    currentDate = LocalDate.now().minusMonths(3);
                    System.out.println("No existing data found or table empty. Starting generation from default date: " + currentDate);
                }
            } catch (SQLException e) {
                System.err.println("Error querying for latest date. Using default start date.");
                e.printStackTrace();
                currentDate = LocalDate.now().minusMonths(3); // Fallback to default on error
            }
            // --- End of starting date determination ---


            conn.setAutoCommit(false); // Use transactions

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

                int deleteCounter = 0;

                // The loop now starts with the correctly determined currentDate
                while (true) {
                    // ... (data generation logic remains the same) ...
                    double dailyConsumption = MIN_POWER + random.nextDouble() * MAX_POWER_RANGE;
                    boolean fault = random.nextInt(100) < FAULT_CHANCE_PERCENT;

                    // ... (prepare insert statement remains the same) ...
                    insertStmt.setDate(1, java.sql.Date.valueOf(currentDate));
                    insertStmt.setDouble(2, dailyConsumption);
                    insertStmt.setBoolean(3, fault);

                    insertStmt.executeUpdate();
                    System.out.printf("Inserted: Date: %s, Power: %.2f kWh, Fault: %s%n",
                            currentDate, dailyConsumption, fault);

                    // ... (periodic delete logic remains the same) ...
                    deleteCounter++;
                    if (deleteCounter >= 100) {
                        int deletedRows = deleteStmt.executeUpdate();
                        if (deletedRows > 0) {
                            System.out.println("Performed cleanup: Deleted " + deletedRows + " records older than 1 year.");
                        }
                        deleteCounter = 0;
                    }

                    conn.commit(); // Commit transaction

                    // ... (move to next day and sleep remains the same) ...
                    currentDate = currentDate.plusDays(1);
                    TimeUnit.SECONDS.sleep(1); // Keep the sleep interval

                }
            } catch (SQLException e) {
                System.err.println("Error during statement execution. Rolling back transaction.");
                if (conn != null) { // Check if conn is null before rollback
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        System.err.println("Error during rollback: " + ex.getMessage());
                    }
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