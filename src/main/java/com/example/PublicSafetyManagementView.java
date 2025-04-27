
package com.example;

import com.example.model.Emergency;
import com.example.model.WeatherAlert;
import com.example.model.SafetyService;
import com.example.controller.SafetyController;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("safety") // Route for this view
public class PublicSafetyManagementView extends VerticalLayout {

    // UI Components for Emergency Management
    private final TextField emergencyTypeField;
    private final TextField emergencyLocationField;
    private final TextArea emergencyDescriptionField;
    private final ComboBox<Integer> emergencySeverityField;
    private final Button createEmergencyButton;
    private final Grid<Emergency> emergencyGrid;
    private final ComboBox<String> statusUpdateField;
    private final Button updateStatusButton;
    
    // UI Components for Weather Alerts
    private final TextField weatherAlertTypeField;
    private final TextArea weatherAlertDescriptionField;
    private final ComboBox<Integer> weatherAlertSeverityField;
    private final Button createWeatherAlertButton;
    private final Grid<WeatherAlert> weatherAlertGrid;
    private final TextArea latestAlertInfo;
    
    // Common UI Components
    private final Button refreshButton;
    private final IntegerField daysToKeepField;
    private final Button deleteOldDataButton;
    private final Button verifyDatabaseButton; // Moved this declaration here
    
    // Tabs for organization
    private final VerticalLayout emergencyTab;
    private final VerticalLayout weatherTab;

    public PublicSafetyManagementView() {
        // Set up the layout
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        
        H2 title = new H2("Public Safety Management");
        
        // Create tabs
        emergencyTab = new VerticalLayout();
        emergencyTab.setAlignItems(Alignment.CENTER);
        emergencyTab.setWidth("90%");
        
        weatherTab = new VerticalLayout();
        weatherTab.setAlignItems(Alignment.CENTER);
        weatherTab.setWidth("90%");
        
        Tab tab1 = new Tab("Emergency Response");
        Tab tab2 = new Tab("Weather Alerts");
        Tabs tabs = new Tabs(tab1, tab2);
        tabs.setWidth("100%");
        
        // --- Emergency Management Components ---
        H3 emergencyTitle = new H3("Emergency Response System");
        
        // Emergency creation form
        emergencyTypeField = new TextField("Emergency Type");
        emergencyTypeField.setPlaceholder("Fire, Medical, Police, etc.");
        
        emergencyLocationField = new TextField("Location");
        emergencyLocationField.setPlaceholder("Address or coordinates");
        
        emergencyDescriptionField = new TextArea("Description");
        emergencyDescriptionField.setPlaceholder("Details about the emergency");
        
        emergencySeverityField = new ComboBox<>("Severity (1-5)");
        emergencySeverityField.setItems(1, 2, 3, 4, 5);
        emergencySeverityField.setValue(3);
        
        createEmergencyButton = new Button("Create Emergency Alert");
        createEmergencyButton.getStyle().set("margin-top", "20px");
        
        // Form layout
        HorizontalLayout emergencyFormRow1 = new HorizontalLayout(
            emergencyTypeField, emergencyLocationField, emergencySeverityField
        );
        emergencyFormRow1.setWidthFull();
        
        // Emergency grid
        emergencyGrid = new Grid<>(Emergency.class);
        emergencyGrid.setColumns("id", "type", "location", "severity", "status", "timestamp");
        emergencyGrid.getColumnByKey("timestamp").setHeader("Time");
        emergencyGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        emergencyGrid.setWidthFull();
        
        // Status update components
        statusUpdateField = new ComboBox<>("Update Status");
        statusUpdateField.setItems("PENDING", "DISPATCHED", "IN_PROGRESS", "RESOLVED");
        statusUpdateField.setValue("DISPATCHED");
        
        updateStatusButton = new Button("Update Selected Emergency");
        
        HorizontalLayout statusUpdateLayout = new HorizontalLayout(
            statusUpdateField, updateStatusButton
        );
        
        // Add components to emergency tab
        emergencyTab.add(
            emergencyTitle,
            emergencyFormRow1,
            emergencyDescriptionField,
            createEmergencyButton,
            new Hr(),
            emergencyGrid,
            statusUpdateLayout
        );
        
        // --- Weather Alert Components ---
        H3 weatherTitle = new H3("Weather Alert System");
        
        // Weather alert form
        weatherAlertTypeField = new TextField("Alert Type");
        weatherAlertTypeField.setPlaceholder("Storm, Flood, Extreme Heat, etc.");
        
        weatherAlertDescriptionField = new TextArea("Description");
        weatherAlertDescriptionField.setPlaceholder("Details about the weather alert");
        
        weatherAlertSeverityField = new ComboBox<>("Severity (1-5)");
        weatherAlertSeverityField.setItems(1, 2, 3, 4, 5);
        weatherAlertSeverityField.setValue(3);
        
        createWeatherAlertButton = new Button("Create Weather Alert");
        createWeatherAlertButton.getStyle().set("margin-top", "20px");
        
        // Weather alert grid
        weatherAlertGrid = new Grid<>(WeatherAlert.class);
        weatherAlertGrid.setColumns("id", "alertType", "severity", "timestamp", "active");
        weatherAlertGrid.getColumnByKey("alertType").setHeader("Type");
        weatherAlertGrid.getColumnByKey("timestamp").setHeader("Time");
        weatherAlertGrid.setWidthFull();
        
        // Latest alert info
        latestAlertInfo = new TextArea("Latest Weather Alert");
        latestAlertInfo.setReadOnly(true);
        latestAlertInfo.setWidthFull();
        latestAlertInfo.setValue("No weather alerts available.");
        
        // Add components to weather tab
        weatherTab.add(
            weatherTitle,
            weatherAlertTypeField,
            weatherAlertDescriptionField,
            weatherAlertSeverityField,
            createWeatherAlertButton,
            new Hr(),
            latestAlertInfo,
            weatherAlertGrid
        );
        
        // --- Common Components ---
        refreshButton = new Button("Refresh Data");
        
        daysToKeepField = new IntegerField("Days to Keep Data");
        daysToKeepField.setValue(30);
        daysToKeepField.setMin(1);
        
        deleteOldDataButton = new Button("Delete Old Data");
        
        // Initialize the verify database button
        verifyDatabaseButton = new Button("Verify Database Connection");
        verifyDatabaseButton.getStyle().set("background-color", "#4CAF50");
        verifyDatabaseButton.getStyle().set("color", "white");
        
        HorizontalLayout dataManagementLayout = new HorizontalLayout(
            refreshButton, daysToKeepField, deleteOldDataButton, verifyDatabaseButton
        );
        dataManagementLayout.setAlignItems(Alignment.BASELINE);
        
        // Tab change listener
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(tab1)) {
                add(emergencyTab);
                remove(weatherTab);
            } else {
                add(weatherTab);
                remove(emergencyTab);
            }
        });
        
        // Initial tab
        add(emergencyTab);
        
        // Add components to main layout
        add(title, tabs, new Hr(), dataManagementLayout);
        
        // Initialize service and verify database tables
        SafetyService safetyService = SafetyService.getInstance();
        boolean tablesVerified = safetyService.verifyDatabaseTables();
        if (tablesVerified) {
            Notification.show("Database tables verified successfully", 3000, Notification.Position.BOTTOM_START);
        } else {
            Notification.show("Failed to verify database tables. Check console for details.", 
                             5000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        
        // Initialize controller
        new SafetyController(safetyService, this);
        
        // Style the emergency grid
        styleEmergencyGrid();
    }
    
    // --- Methods for Controller Interaction ---
    
    public void addCreateEmergencyListener(ComponentEventListener<ClickEvent<Button>> listener) {
        createEmergencyButton.addClickListener(listener);
    }
    
    public void addUpdateEmergencyListener(ComponentEventListener<ClickEvent<Button>> listener) {
        updateStatusButton.addClickListener(listener);
    }
    
    public void addCreateWeatherAlertListener(ComponentEventListener<ClickEvent<Button>> listener) {
        createWeatherAlertButton.addClickListener(listener);
    }
    
    public void addRefreshListener(ComponentEventListener<ClickEvent<Button>> listener) {
        refreshButton.addClickListener(listener);
    }
    
    public void addDeleteOldDataListener(ComponentEventListener<ClickEvent<Button>> listener) {
        deleteOldDataButton.addClickListener(listener);
    }
    
    public void addVerifyDatabaseListener(ComponentEventListener<ClickEvent<Button>> listener) {
        verifyDatabaseButton.addClickListener(listener);
    }
    
    // --- Getters for form values ---
    
    public String getEmergencyType() {
        return emergencyTypeField.getValue();
    }
    
    public String getEmergencyLocation() {
        return emergencyLocationField.getValue();
    }
    
    public String getEmergencyDescription() {
        return emergencyDescriptionField.getValue();
    }
    
    public int getEmergencySeverity() {
        return emergencySeverityField.getValue() != null ? emergencySeverityField.getValue() : 3;
    }
    
    public String getWeatherAlertType() {
        return weatherAlertTypeField.getValue();
    }
    
    public String getWeatherAlertDescription() {
        return weatherAlertDescriptionField.getValue();
    }
    
    public int getWeatherAlertSeverity() {
        return weatherAlertSeverityField.getValue() != null ? weatherAlertSeverityField.getValue() : 3;
    }
    
    public Long getSelectedEmergencyId() {
        Emergency selected = emergencyGrid.asSingleSelect().getValue();
        return selected != null ? selected.getId() : null;
    }
    
    public String getSelectedStatus() {
        return statusUpdateField.getValue();
    }
    
    public int getDaysToKeep() {
        return daysToKeepField.getValue() != null ? daysToKeepField.getValue() : 30;
    }
    
    // --- Setters and UI update methods ---
    
    public void updateEmergencyList(List<Emergency> emergencies) {
        emergencyGrid.setItems(emergencies);
    }
    
    public void updateWeatherAlertList(List<WeatherAlert> alerts) {
        weatherAlertGrid.setItems(alerts);
    }
    
    public void setLatestAlertInfo(String info) {
        latestAlertInfo.setValue(info);
    }
    
    public void setLatestAlertInfo(String info, String alertType) {
        latestAlertInfo.setValue(info);
        
        // Style based on alert type
        if (alertType != null) {
            alertType = alertType.toLowerCase();
            if (alertType.contains("storm") || alertType.contains("hurricane") || alertType.contains("tornado")) {
                latestAlertInfo.getStyle().set("background-color", "#ffcccc");
                latestAlertInfo.getStyle().set("color", "#990000");
                latestAlertInfo.getStyle().set("border", "2px solid #990000");
            } else if (alertType.contains("rain") || alertType.contains("snow") || alertType.contains("flood")) {
                latestAlertInfo.getStyle().set("background-color", "#fff2cc");
                latestAlertInfo.getStyle().set("color", "#996600");
                latestAlertInfo.getStyle().set("border", "2px solid #996600");
            } else if (alertType.contains("sun") || alertType.contains("clear") || alertType.contains("hot")) {
                latestAlertInfo.getStyle().set("background-color", "#ffffcc");
                latestAlertInfo.getStyle().set("color", "#999900");
                latestAlertInfo.getStyle().set("border", "2px solid #999900");
            } else if (alertType.contains("cloud") || alertType.contains("fog") || alertType.contains("mist")) {
                latestAlertInfo.getStyle().set("background-color", "#e6e6e6");
                latestAlertInfo.getStyle().set("color", "#666666");
                latestAlertInfo.getStyle().set("border", "2px solid #666666");
            } else {
                // Reset styles for other types
                latestAlertInfo.getStyle().set("background-color", "white");
                latestAlertInfo.getStyle().set("color", "black");
                latestAlertInfo.getStyle().set("border", "1px solid #ccc");
            }
        }
    }
    
    public void clearEmergencyForm() {
        emergencyTypeField.clear();
        emergencyLocationField.clear();
        emergencyDescriptionField.clear();
        emergencySeverityField.setValue(3);
    }
    
    public void clearWeatherAlertForm() {
        weatherAlertTypeField.clear();
        weatherAlertDescriptionField.clear();
        weatherAlertSeverityField.setValue(3);
    }
    
    public void showNotification(String message, boolean isError) {
        Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
        if (isError) {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }
    
    // Method to style emergency grid based on severity
    private void styleEmergencyGrid() {
        emergencyGrid.setClassNameGenerator(emergency -> {
            int severity = emergency.getSeverity();
            
            if (severity >= 4) {
                return "emergency-critical";
            } else if (severity == 3) {
                return "emergency-high";
            } else if (severity == 2) {
                return "emergency-medium";
            } else {
                return "emergency-low";
            }
        });
        
        // Add CSS styles
        String styles = 
            ".emergency-critical { background-color: #ff9999; font-weight: bold; }" +
            ".emergency-high { background-color: #ffcc99; }" +
            ".emergency-medium { background-color: #ffffcc; }" +
            ".emergency-low { background-color: #e6ffe6; }";
        
        UI.getCurrent().getPage().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = $0;" +
            "document.head.appendChild(style);", 
            styles
        );
    }
    
    // Method to play emergency alert sound
    public void playEmergencyAlert() {
        UI.getCurrent().getPage().executeJs(
            "const audio = new Audio('https://assets.mixkit.co/sfx/preview/mixkit-alert-alarm-1005.mp3');" +
            "audio.play();"
        );
    }

}