package com.example;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

@Route("")
public class MainView extends VerticalLayout {

    public MainView() {
        H1 title = new H1("Smart City Management");
        title.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER, LumoUtility.Margin.Top.MEDIUM);

        Button trafficButton = new Button("Traffic Management", e -> UI.getCurrent().navigate("traffic"));
        Button utilityButton = new Button("Utility Management", e -> UI.getCurrent().navigate("utility"));
        Button safetyButton = new Button("Public Safety Management", e -> UI.getCurrent().navigate("safety"));
        Button environmentButton = new Button("Environmental Management", e -> UI.getCurrent().navigate("environment"));

        for (Button btn : new Button[]{trafficButton, utilityButton, safetyButton, environmentButton}) {
            btn.setWidth("250px");
            btn.addClassName(LumoUtility.Margin.Vertical.SMALL);
        }

        VerticalLayout buttonLayout = new VerticalLayout(
                trafficButton,
                utilityButton,
                safetyButton,
                environmentButton
        );
        buttonLayout.setPadding(false);
        buttonLayout.setSpacing(false);
        buttonLayout.setWidth(null);
        buttonLayout.addClassName(LumoUtility.Margin.Top.LARGE);

        setAlignItems(Alignment.CENTER);
        add(title, buttonLayout);

        setSizeFull();
    }
}