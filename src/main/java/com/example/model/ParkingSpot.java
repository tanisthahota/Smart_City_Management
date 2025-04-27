package com.example.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class ParkingSpot {
    private String spotId;
    private String locationDescription;
    private boolean occupied;
    private LocalDateTime lastUpdated;

    public ParkingSpot(String spotId, String locationDescription, boolean occupied, Timestamp lastUpdated) {
        this.spotId = spotId;
        this.locationDescription = locationDescription;
        this.occupied = occupied;
        this.lastUpdated = (lastUpdated != null) ? lastUpdated.toLocalDateTime() : null;
    }

    public String getSpotId() {
        return spotId;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return "ParkingSpot{" +
                "spotId='" + spotId + '\'' +
                ", locationDescription='" + locationDescription + '\'' +
                ", occupied=" + occupied +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}