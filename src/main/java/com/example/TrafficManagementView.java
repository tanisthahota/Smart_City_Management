package com.example;

import com.example.controller.TrafficController;
import com.example.model.JunctionState;
import com.example.model.ParkingSpot;
import com.example.model.TrafficService;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("traffic")
@PageTitle("Traffic & Parking Management")
public class TrafficManagementView extends VerticalLayout {

    private final Button refreshButton;
    private final Button parkingButton;
    private final Button deleteButton;
    private final Button backButton;
    private final VerticalLayout junctionDisplayLayout;
    private final VerticalLayout parkingDisplayLayout;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public TrafficManagementView() {
        addClassName("traffic-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);

        getStyle()
                .set("background-image", "url(images/trafficmgmet_bg.png)")
                .set("background-size", "cover")
                .set("background-position", "center")
                .set("background-repeat", "no-repeat")
                .set("min-height", "100vh");

        H2 title = new H2("Smart City Traffic & Parking");

        junctionDisplayLayout = new VerticalLayout();
        junctionDisplayLayout.setSpacing(false);
        junctionDisplayLayout.setPadding(false);
        junctionDisplayLayout.setWidth("100%");
        junctionDisplayLayout.add(new Paragraph("Click 'Refresh Data' to load junction status..."));

        parkingDisplayLayout = new VerticalLayout();
        parkingDisplayLayout.setSpacing(true);
        parkingDisplayLayout.setPadding(false);
        parkingDisplayLayout.setWidth("100%");
        parkingDisplayLayout.add(new Paragraph("Click 'Show Parking' to load status..."));
        parkingDisplayLayout.getStyle()
                .set("max-height", "400px")
                .set("overflow-y", "auto");


        refreshButton = new Button("Refresh Data");
        parkingButton = new Button("Show Parking");
        deleteButton = new Button("Delete Old Junction Data");
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        backButton = new Button("Back to Main Menu");
        backButton.addClickListener(e -> backButton.getUI().ifPresent(ui -> ui.navigate("")));

        HorizontalLayout buttonBar = new HorizontalLayout(refreshButton, parkingButton, deleteButton, backButton);
        buttonBar.setSpacing(true);

        VerticalLayout contentBox = new VerticalLayout(
                new H2("Live Junction Status"),
                junctionDisplayLayout,
                new H2("Parking Status"),
                parkingDisplayLayout,
                buttonBar
        );
        contentBox.setAlignItems(Alignment.CENTER);
        contentBox.getStyle()
                .set("background-color", "rgba(255, 255, 255, 0.85)")
                .set("padding", "20px")
                .set("border-radius", "10px");
        contentBox.setWidth("80%");
        contentBox.setHeight("auto");

        add(title, contentBox);
        setJustifyContentMode(JustifyContentMode.CENTER);

        new TrafficController(TrafficService.getInstance(), this);
    }

    public void updateJunctionDisplay(Map<String, JunctionState> latestJunctions) {
        junctionDisplayLayout.removeAll();
        if (latestJunctions == null || latestJunctions.isEmpty()) {
            junctionDisplayLayout.add(new Paragraph("No junction data available."));
            return;
        }

        latestJunctions.values().stream()
                .sorted(Comparator.comparing(JunctionState::getJunctionId))
                .forEach(state -> {
                    VerticalLayout junctionLayout = new VerticalLayout();
                    junctionLayout.setSpacing(false);
                    junctionLayout.setPadding(false);
                    junctionLayout.setWidth("100%");
                    junctionLayout.getStyle().set("border", "1px solid #eee").set("padding", "10px").set("margin-bottom", "10px");

                    junctionLayout.add(new Span("Junction: " + state.getJunctionId() +
                                                " (Updated: " + (state.getLastUpdated() != null ? state.getLastUpdated().format(dtf) : "N/A") + ")"));

                    HorizontalLayout lanesLayout = new HorizontalLayout();
                    lanesLayout.setSpacing(true);
                    lanesLayout.setWidthFull();

                    Span lane1 = createLaneSpan(1, state.getLane1Vehicles(), state.getGreenLaneId());
                    Span lane2 = createLaneSpan(2, state.getLane2Vehicles(), state.getGreenLaneId());
                    Span lane3 = createLaneSpan(3, state.getLane3Vehicles(), state.getGreenLaneId());
                    Span lane4 = createLaneSpan(4, state.getLane4Vehicles(), state.getGreenLaneId());

                    lanesLayout.add(lane1, lane2, lane3, lane4);
                    lanesLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);

                    junctionLayout.add(lanesLayout);
                    junctionDisplayLayout.add(junctionLayout);
                });
    }

    private Span createLaneSpan(int laneId, int vehicleCount, int greenLaneId) {
        Span laneSpan = new Span(String.format("Lane %d: %d vehicles", laneId, vehicleCount));
        laneSpan.getStyle()
                .set("border", "1px solid #ccc")
                .set("padding", "8px")
                .set("border-radius", "4px")
                .set("text-align", "center")
                .set("flex-grow", "1");

        if (laneId == greenLaneId) {
            laneSpan.getStyle()
                    .set("background-color", "#90EE90")
                    .set("font-weight", "bold")
                    .set("border-color", "#2E8B57");
        } else {
            laneSpan.getStyle().set("background-color", "#f0f0f0");
        }
        return laneSpan;
    }

    public void displayParkingInfo(List<ParkingSpot> spots) {
        parkingDisplayLayout.removeAll();

        if (spots == null || spots.isEmpty()) {
            parkingDisplayLayout.add(new Paragraph("No parking spot data available."));
            return;
        }

        long availableCount = spots.stream().filter(s -> !s.isOccupied()).count();
        long totalCount = spots.size();

        H2 parkingHeader = new H2(String.format("Parking Availability (%d / %d Available)", availableCount, totalCount));
        parkingDisplayLayout.add(parkingHeader);


        spots.stream()
                .sorted(Comparator.comparing(ParkingSpot::getSpotId))
                .forEach(spot -> {
                    HorizontalLayout spotLayout = new HorizontalLayout();
                    spotLayout.setWidthFull();
                    spotLayout.setAlignItems(Alignment.CENTER);
                    spotLayout.getStyle()
                            .set("border-bottom", "1px solid #eee")
                            .set("padding", "10px 0");

                    Icon statusIcon;
                    Span statusText = new Span();
                    statusText.getStyle().set("font-weight", "bold");

                    if (spot.isOccupied()) {
                        statusIcon = VaadinIcon.CLOSE_CIRCLE.create();
                        statusIcon.setColor("red");
                        statusText.setText("Occupied");
                        statusText.getStyle().set("color", "red");
                    } else {
                        statusIcon = VaadinIcon.CHECK_CIRCLE.create();
                        statusIcon.setColor("green");
                        statusText.setText("Available");
                        statusText.getStyle().set("color", "green");
                    }

                    String description = spot.getLocationDescription() != null && !spot.getLocationDescription().isEmpty()
                                         ? " (" + spot.getLocationDescription() + ")" : "";
                    Span spotIdSpan = new Span(spot.getSpotId() + description);
                    spotIdSpan.getStyle().set("flex-grow", "1");

                    String updatedTime = spot.getLastUpdated() != null
                                         ? spot.getLastUpdated().format(dtf) : "N/A";
                    Span timeSpan = new Span("Updated: " + updatedTime);
                    timeSpan.getStyle().set("font-size", "small").set("color", "gray");

                    spotLayout.add(statusIcon, statusText, spotIdSpan, timeSpan);
                    parkingDisplayLayout.add(spotLayout);
                });
    }

    public void showNotification(String message, boolean isError) {
        Notification notification = new Notification(message, 3000, Notification.Position.MIDDLE);
        if (isError) {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
        notification.open();
    }

    public void addRefreshButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
        refreshButton.addClickListener(listener);
    }

    public void addParkingButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
        parkingButton.addClickListener(listener);
    }

    public void addDeleteButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
        deleteButton.addClickListener(listener);
    }
}