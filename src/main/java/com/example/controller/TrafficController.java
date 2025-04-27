package com.example.controller;

import com.example.TrafficManagementView;
import com.example.model.JunctionState;
import com.example.model.ParkingSpot;
import com.example.model.TrafficService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TrafficController {

    private final TrafficService service;
    private final TrafficManagementView view;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TrafficController(TrafficService service, TrafficManagementView view) {
        this.service = service;
        this.view = view;

        this.view.addRefreshButtonListener(e -> handleRefreshClick());
        this.view.addParkingButtonListener(e -> handleParkingClick());
        this.view.addDeleteButtonListener(e -> handleDeleteJunctionStatesClick());

        handleRefreshClick();
    }

    private void handleRefreshClick() {
        Map<String, JunctionState> latestJunctions = service.getLatestJunctionStates();
        view.updateJunctionDisplay(latestJunctions);

        handleParkingClick();
    }

    private void handleParkingClick() {
        List<ParkingSpot> spots = service.getAllParkingSpots();
        view.displayParkingInfo(spots);
    }

    private void handleDeleteJunctionStatesClick() {
        String resultMessage = service.deleteOldJunctionStates();
        boolean isError = resultMessage.toLowerCase().startsWith("error");
        view.showNotification(resultMessage, isError);

        handleRefreshClick();
    }

    public void startContinuousUpdates() {
        System.out.println("Continuous update logic to be implemented here.");
    }
}