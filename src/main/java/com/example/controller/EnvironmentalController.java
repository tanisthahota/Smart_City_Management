package com.example.controller;

import com.example.EnvironmentalManagementView;
import com.example.model.EnvironmentalService;
import com.example.model.EnvironmentalService.AirQualityReading;
import com.example.model.EnvironmentalService.NoiseLevelReading;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class EnvironmentalController {

    private final EnvironmentalService service;
    private final EnvironmentalManagementView view;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public EnvironmentalController(EnvironmentalService service, EnvironmentalManagementView view) {
        this.service = service;
        this.view = view;

        this.view.addRefreshButtonListener(e -> handleRefreshClick());
        this.view.addAlertsButtonListener(e -> handleAlertsClick());
        this.view.addReportButtonListener(e -> handleReportClick());
        this.view.addDeleteButtonListener(e -> handleDeleteClick());

        handleRefreshClick();
    }

    private void handleRefreshClick() {
        Map<String, AirQualityReading> airQualityReadings = service.getLatestAirQualityReadings();
        view.updateAirQualityGrid(airQualityReadings);

        List<NoiseLevelReading> noiseLevelReadings = service.getLatestNoiseLevelReadings();
        view.updateNoiseLevelGrid(noiseLevelReadings);

        if (airQualityReadings.isEmpty() && noiseLevelReadings.isEmpty()) {
            view.showNotification("No environmental data available.", true);
        } else {
            view.showNotification("Environmental data refreshed successfully.", false);
        }
    }

    private void handleAlertsClick() {
        int daysToInclude = view.getSelectedDays();
        List<AirQualityReading> alerts = service.getAirQualityAlerts(daysToInclude);

        if (alerts.isEmpty()) {
            view.setAlertsContent("No air quality alerts in the last " + daysToInclude + " days.");
            return;
        }

        StringBuilder alertsText = new StringBuilder();
        alertsText.append(String.format("Air Quality Alerts (Last %d Days):\n", daysToInclude));
        alertsText.append("--------------------------------------------------\n");

        for (AirQualityReading alert : alerts) {
            alertsText.append(String.format(
                "Location: %s\n" +
                "Time: %s\n" +
                "Quality Index: %s\n" +
                "PM2.5: %.2f μg/m³, PM10: %.2f μg/m³, Ozone: %.2f ppb\n" +
                "--------------------------------------------------\n",
                alert.getLocation(),
                alert.getTimestamp().format(dtf),
                alert.getQualityIndex(),
                alert.getPm25Level(),
                alert.getPm10Level(),
                alert.getOzoneLevel()
            ));
        }

        view.setAlertsContent(alertsText.toString());
    }

    private void handleReportClick() {
        String selectedLocation = view.getSelectedLocation();
        int daysToInclude = view.getSelectedDays();

        if (selectedLocation == null || selectedLocation.isEmpty()) {
            view.showNotification("Please select a location first.", true);
            return;
        }

        String report = service.generateAirQualityReport(selectedLocation, daysToInclude);
        view.setReportContent(report);
    }

    private void handleDeleteClick() {
        int daysToKeep = 30;
        String resultMessage = service.deleteOldEnvironmentalData(daysToKeep);

        boolean isError = resultMessage.toLowerCase().startsWith("error");
        view.showNotification(resultMessage, isError);

        if (!isError) {
            handleRefreshClick();
        }
    }

    public void startContinuousUpdates() {
        System.out.println("Continuous environmental monitoring updates to be implemented.");
    }
}