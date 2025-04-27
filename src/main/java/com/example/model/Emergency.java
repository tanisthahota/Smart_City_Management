package com.example.model;

import java.time.LocalDateTime;

public class Emergency {
    private Long id;
    private String type;
    private String location;
    private String description;
    private int severity; // 1-5 scale
    private LocalDateTime timestamp;
    private String status; // PENDING, DISPATCHED, RESOLVED

    // Constructor for new emergencies
    public Emergency(String type, String location, String description, int severity) {
        this.type = type;
        this.location = location;
        this.description = description;
        this.severity = severity;
        this.timestamp = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Constructor for database retrieval
    public Emergency(Long id, String type, String location, String description, 
                    int severity, LocalDateTime timestamp, String status) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.description = description;
        this.severity = severity;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Emergency{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", location='" + location + '\'' +
                ", severity=" + severity +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
}