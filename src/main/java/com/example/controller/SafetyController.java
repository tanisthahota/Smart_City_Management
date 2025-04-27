package com.example.controller;

import com.example.PublicSafetyManagementView;
import com.example.model.Emergency;
import com.example.model.SafetyService;
import com.example.model.WeatherAlert;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class SafetyController {

    private final SafetyService service;
    private final PublicSafetyManagementView view;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Add this method to your SafetyController class
    private void verifyDatabaseConnection() {
        try {
            List<Emergency> emergencies = service.getActiveEmergencies();
            List<WeatherAlert> alerts = service.getActiveWeatherAlerts();
            
            view.showNotification(
                "Database connection successful. Found " + 
                emergencies.size() + " emergencies and " + 
                alerts.size() + " weather alerts.", 
                false
            );
        } catch (Exception e) {
            view.showNotification("Database connection error: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    // Then call this method from your constructor
    public SafetyController(SafetyService service, PublicSafetyManagementView view) {
        this.service = service;
        this.view = view;
    
        // Attach listeners
        this.view.addCreateEmergencyListener(e -> handleCreateEmergency());
        this.view.addUpdateEmergencyListener(e -> handleUpdateEmergency());
        this.view.addCreateWeatherAlertListener(e -> handleCreateWeatherAlert());
        this.view.addRefreshListener(e -> handleRefresh());
        this.view.addDeleteOldDataListener(e -> handleDeleteOldData());
        
        // Add a verification button listener
        this.view.addVerifyDatabaseListener(e -> verifyDatabaseConnection());
    
        // Initial data load
        refreshEmergencyList();
        refreshWeatherAlerts();
    }

    private void handleCreateEmergency() {
        // Get form values from view
        String type = view.getEmergencyType();
        String location = view.getEmergencyLocation();
        String description = view.getEmergencyDescription();
        int severity = view.getEmergencySeverity();

        // Validate input
        if (type.isEmpty() || location.isEmpty() || description.isEmpty()) {
            view.showNotification("Please fill all required fields", true);
            return;
        }

        // Create emergency
        Emergency emergency = new Emergency(type, location, description, severity);
        Emergency created = service.createEmergency(emergency);

        if (created != null) {
            view.showNotification("Emergency created successfully", false);
            view.clearEmergencyForm();
            refreshEmergencyList();
            view.playEmergencyAlert(); // Play alert sound for emergencies
        } else {
            view.showNotification("Failed to create emergency", true);
        }
    }

    private void handleUpdateEmergency() {
        // Get selected emergency and new status
        Long selectedId = view.getSelectedEmergencyId();
        String newStatus = view.getSelectedStatus();

        if (selectedId == null) {
            view.showNotification("Please select an emergency to update", true);
            return;
        }

        // Update emergency status
        boolean updated = service.updateEmergencyStatus(selectedId, newStatus);

        if (updated) {
            view.showNotification("Emergency status updated to: " + newStatus, false);
            refreshEmergencyList();
        } else {
            view.showNotification("Failed to update emergency status", true);
        }
    }

    private void handleCreateWeatherAlert() {
        // Get form values from view
        String alertType = view.getWeatherAlertType();
        String description = view.getWeatherAlertDescription();
        int severity = view.getWeatherAlertSeverity();

        // Validate input
        if (alertType.isEmpty() || description.isEmpty()) {
            view.showNotification("Please fill all required fields", true);
            return;
        }

        // Create weather alert
        WeatherAlert alert = new WeatherAlert(alertType, description, severity);
        WeatherAlert created = service.createWeatherAlert(alert);

        if (created != null) {
            view.showNotification("Weather alert created successfully", false);
            view.clearWeatherAlertForm();
            refreshWeatherAlerts();
        } else {
            view.showNotification("Failed to create weather alert", true);
        }
    }

    private void handleRefresh() {
        try {
            refreshEmergencyList();
            refreshWeatherAlerts();
            view.showNotification("Data refreshed successfully", false);
        } catch (Exception e) {
            view.showNotification("Error refreshing data: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void handleDeleteOldData() {
        int daysToKeep = view.getDaysToKeep();
        int deleted = service.deleteOldEmergencies(daysToKeep);
        view.showNotification("Deleted " + deleted + " old emergency records", false);
        refreshEmergencyList();
    }

    private void refreshEmergencyList() {
        List<Emergency> emergencies = service.getActiveEmergencies();
        view.updateEmergencyList(emergencies);
    }

    private void refreshWeatherAlerts() {
        List<WeatherAlert> alerts = service.getActiveWeatherAlerts();
        view.updateWeatherAlertList(alerts);
        
        // Update latest alert display
        Optional<WeatherAlert> latestAlert = service.getLatestWeatherAlert();
        if (latestAlert.isPresent()) {
            WeatherAlert alert = latestAlert.get();
            String alertInfo = String.format(
                "Latest Weather Alert (%s):\n - Type: %s\n - Severity: %d/5\n - Time: %s\n - %s",
                alert.isActive() ? "ACTIVE" : "INACTIVE",
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getTimestamp().format(DATE_TIME_FORMATTER),
                alert.getDescription()
            );
            view.setLatestAlertInfo(alertInfo, alert.getAlertType()); // Pass the alert type for styling
        } else {
            view.setLatestAlertInfo("No weather alerts available.", null);
        }
    }
}