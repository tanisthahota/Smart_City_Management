package com.example.controller;

import com.example.UtilityManagementView;
import com.example.model.PowerReading;
import com.example.model.UtilityService;


import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class UtilityController {

    private final UtilityService service;
    private final UtilityManagementView view;

    public UtilityController(UtilityService service, UtilityManagementView view) {
        this.service = service;
        this.view = view;

        this.view.addTrackButtonListener(e -> handleTrackButtonClick());
        this.view.addReportButtonListener(e -> handleReportButtonClick());
        this.view.addDeleteButtonListener(e -> handleDeleteButtonClick());
    }

    private void handleTrackButtonClick() {
        Optional<PowerReading> latestReadingOpt = service.getLatestReading();

        if (latestReadingOpt.isPresent()) {
            PowerReading reading = latestReadingOpt.get();
            String status = String.format(
                "Latest Reading (ID: %d, Date: %s):\n - Power Consumed: %.2f kWh\n - Fault Detected: %s",
                reading.getId(),
                reading.getDate().format(DateTimeFormatter.ISO_DATE),
                reading.getPowerConsumed(),
                reading.isFaultDetected() ? "YES" : "NO"
            );
            view.setTrackingStatus(status);
        } else {
            view.setTrackingStatus("No data available yet.");
        }
    }

    private void handleReportButtonClick() {
        String report = service.generateLatestMonthlyReport();

        view.setReportContent(report);
    }

    private void handleDeleteButtonClick() {
        String resultMessage = service.deleteReadingsBeforeLatestMonth();

        boolean isError = resultMessage.toLowerCase().startsWith("error");
        view.showNotification(resultMessage, isError);

    }
}