package com.example;

import com.example.controller.UtilityController;
import com.example.model.UtilityService;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;

@Route("utility")
public class UtilityManagementView extends VerticalLayout {

    private final TextArea trackingOutput;
    private final TextArea reportOutput;
    private final Button trackButton;
    private final Button reportButton;
    private final Button deleteButton;

    public UtilityManagementView() {
        setPadding(true);
        setSpacing(true);

        setMaxWidth("1200px");
        setWidthFull();
        getStyle().set("margin", "0 auto");

        H2 title = new H2("Utility Management Dashboard");
        Paragraph description = new Paragraph(
                "Manage power consumption tracking, fault detection, and reporting."
        );
        setAlignSelf(Alignment.STRETCH, title, description);

        add(title, description);

        Hr headerSeparator = new Hr();
        headerSeparator.getStyle().set("margin", "var(--lumo-space-l) 0");
        add(headerSeparator);


        HorizontalLayout contentLayout = new HorizontalLayout();
        contentLayout.setWidthFull();
        contentLayout.setSpacing(true);

        H3 trackingHeader = new H3("Tracking & Fault Detection");
        VerticalLayout trackingSection = new VerticalLayout(trackingHeader);
        trackingSection.setPadding(true);
        trackingSection.setSpacing(true);
        trackingSection.setWidthFull();
        contentLayout.setFlexGrow(1, trackingSection);
        trackingSection.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        trackingSection.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

        trackButton = new Button("Refresh");
        trackButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        trackingOutput = new TextArea("Status");
        trackingOutput.setWidthFull();
        trackingOutput.setReadOnly(true);
        trackingOutput.setPlaceholder("Click 'Start Tracking' to see the latest status...");
        trackingOutput.setHeight("200px");

        trackingSection.add(trackButton, trackingOutput);


        H3 reportingHeader = new H3("Monthly Power Report");
        VerticalLayout reportingSection = new VerticalLayout(reportingHeader);
        reportingSection.setPadding(true);
        reportingSection.setSpacing(true);
        reportingSection.setWidthFull();
        contentLayout.setFlexGrow(1, reportingSection);
        reportingSection.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        reportingSection.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

        reportButton = new Button("Generate Report");
        reportButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        reportOutput = new TextArea("Report Content");
        reportOutput.setWidthFull();
        reportOutput.setReadOnly(true);
        reportOutput.setPlaceholder("Click 'Generate Report' to view analysis...");
        reportOutput.setHeight("250px");

        reportingSection.add(reportButton, reportOutput);

        contentLayout.add(trackingSection, reportingSection);

        add(contentLayout);


        Hr contentSeparator = new Hr();
        contentSeparator.getStyle().set("margin", "var(--lumo-space-l) 0");
        add(contentSeparator);

        H3 dataManagementHeader = new H3("Data Management");
        VerticalLayout dataManagementSection = new VerticalLayout(dataManagementHeader);
        dataManagementSection.setPadding(true);
        dataManagementSection.setSpacing(true);
        dataManagementSection.setWidthFull();
        dataManagementSection.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        dataManagementSection.getStyle().set("border-radius", "var(--lumo-border-radius-m)");


        Paragraph deleteWarning = new Paragraph("Caution: This action will permanently delete historical data before the latest month.");
        deleteWarning.getStyle().set("color", "var(--lumo-error-text-color)");

        deleteButton = new Button("Delete Old Data");
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.getStyle().set("margin-top", "var(--lumo-space-s)");

        dataManagementSection.add(deleteWarning, deleteButton);

        add(dataManagementSection);

        new UtilityController(UtilityService.getInstance(), this);
    }


    public void addTrackButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
        trackButton.addClickListener(listener);
    }

    public void addReportButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
        reportButton.addClickListener(listener);
    }

    public void addDeleteButtonListener(ComponentEventListener<ClickEvent<Button>> listener) {
        deleteButton.addClickListener(listener);
    }

    public void setTrackingStatus(String status) {
        trackingOutput.setValue(status);
    }

    public void setReportContent(String report) {
        reportOutput.setValue(report);
    }

    public void showNotification(String message, boolean isError) {
        Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
        if (isError) {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }
}