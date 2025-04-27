
-- Create the database (if it doesn't exist)
CREATE DATABASE IF NOT EXISTS smart_city_db;

-- Use the database
USE smart_city_db;

-- Create the table for power readings
CREATE TABLE IF NOT EXISTS power_readings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reading_date DATE NOT NULL,
    power_consumed DOUBLE NOT NULL,
    fault_detected BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Optional: track when record was inserted
    INDEX idx_reading_date (reading_date) -- Add index for faster date filtering/deletion
);

-- --- New Table for Monthly Statistics ---
CREATE TABLE IF NOT EXISTS `power_stats` (
    `year_month` CHAR(7) NOT NULL PRIMARY KEY,
    `total_consumption` DOUBLE NOT NULL,
    `fault_count` INT NOT NULL,
    `days_recorded` INT NOT NULL,
    `average_consumption` DOUBLE NOT NULL,
    `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- --- New Table for Single Junction State ---
CREATE TABLE IF NOT EXISTS junction_state (
    junction_id VARCHAR(50) PRIMARY KEY,    -- Identifier for the junction (e.g., "MainJunction")
    lane_1_vehicles INT NOT NULL DEFAULT 0, -- Vehicles in approach/lane 1
    lane_2_vehicles INT NOT NULL DEFAULT 0, -- Vehicles in approach/lane 2
    lane_3_vehicles INT NOT NULL DEFAULT 0, -- Vehicles in approach/lane 3
    lane_4_vehicles INT NOT NULL DEFAULT 0, -- Vehicles in approach/lane 4
    green_lane_id INT NOT NULL DEFAULT 1,   -- Which lane (1-4) currently has green
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Initialize the state for the main junction (optional, the generator will do this too)
-- INSERT INTO junction_state (junction_id) VALUES ('MainJunction') ON DUPLICATE KEY UPDATE junction_id=junction_id;

-- --- New Table for Traffic Signal Readings ---  <- REMOVE THIS BLOCK
-- CREATE TABLE IF NOT EXISTS traffic_readings (
--     id INT AUTO_INCREMENT PRIMARY KEY,
--     signal_location VARCHAR(255) NOT NULL, -- e.g., "Main St & 1st Ave"
--     lane_id INT NOT NULL,                  -- e.g., 1, 2, 3, 4
--     vehicle_count INT NOT NULL,
--     reading_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     INDEX idx_signal_time (signal_location, reading_timestamp), -- Index for faster lookups
--     INDEX idx_timestamp (reading_timestamp) -- Index for faster time-based filtering/deletion
-- );

-- --- New Table for Parking Spots ---
CREATE TABLE IF NOT EXISTS parking_spots (
    spot_id VARCHAR(50) PRIMARY KEY,       -- e.g., "P1-A01", "DowntownGarage-3B-12"
    location_description VARCHAR(255),     -- e.g., "Parking Lot 1, Row A, Spot 01"
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- --- Optional: Initial Parking Spot Data ---
-- You might want to pre-populate some parking spots
-- INSERT INTO parking_spots (spot_id, location_description, is_occupied) VALUES
-- ('P1-A01', 'Main St Lot, Row A, Spot 01', FALSE),
-- ('P1-A02', 'Main St Lot, Row A, Spot 02', TRUE),
-- ('P1-A03', 'Main St Lot, Row A, Spot 03', FALSE),
-- ('DG-1A-01', 'Downtown Garage, Level 1A, Spot 01', FALSE)
-- ON DUPLICATE KEY UPDATE spot_id=spot_id; -- Avoid errors if run multiple times
-- Air Quality Readings Table
CREATE TABLE air_quality_readings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    location VARCHAR(100) NOT NULL,
    pm25_level DOUBLE NOT NULL,
    pm10_level DOUBLE NOT NULL,
    ozone_level DOUBLE NOT NULL,
    quality_index VARCHAR(20) NOT NULL,
    INDEX idx_location (location),
    INDEX idx_timestamp (timestamp)
);

-- Noise Level Readings Table
CREATE TABLE noise_level_readings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    location VARCHAR(100) NOT NULL,
    decibel_level DOUBLE NOT NULL,
    zone_type VARCHAR(50) NOT NULL,
    exceeds_limit BOOLEAN NOT NULL,
    INDEX idx_location (location),
    INDEX idx_timestamp (timestamp)
);
-- ... existing code ...

-- Create Emergency Management Table
CREATE TABLE IF NOT EXISTS emergencies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    location VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    severity INT NOT NULL,
    timestamp DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    INDEX idx_status (status),
    INDEX idx_severity (severity)
);

-- Create Weather Alert Table
CREATE TABLE IF NOT EXISTS weather_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    severity INT NOT NULL,
    timestamp DATETIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_active (active),
    INDEX idx_severity (severity)
);
