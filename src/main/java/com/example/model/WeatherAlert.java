package com.example.model;

import java.time.LocalDateTime;

public class WeatherAlert {
    private Long id;
    private String alertType;
    private String description;
    private int severity; // 1-5 scale
    private LocalDateTime timestamp;
    private boolean active;

    // Constructor for new alerts
    public WeatherAlert(String alertType, String description, int severity) {
        this.alertType = alertType;
        this.description = description;
        this.severity = severity;
        this.timestamp = LocalDateTime.now();
        this.active = true;
    }

    // Constructor for database retrieval
    public WeatherAlert(Long id, String alertType, String description, 
                       int severity, LocalDateTime timestamp, boolean active) {
        this.id = id;
        this.alertType = alertType;
        this.description = description;
        this.severity = severity;
        this.timestamp = timestamp;
        this.active = active;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "WeatherAlert{" +
                "id=" + id +
                ", alertType='" + alertType + '\'' +
                ", description='" + description + '\'' +
                ", severity=" + severity +
                ", timestamp=" + timestamp +
                ", active=" + active +
                '}';
    }
}