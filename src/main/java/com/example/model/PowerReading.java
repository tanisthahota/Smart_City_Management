package com.example.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class PowerReading {
    private int id;
    private LocalDate date;
    private double powerConsumed;
    private boolean faultDetected;

    public PowerReading(LocalDate date, double powerConsumed, boolean faultDetected) {
        this.date = date;
        this.powerConsumed = powerConsumed;
        this.faultDetected = faultDetected;
    }

    public PowerReading(int id, LocalDate date, double powerConsumed, boolean faultDetected) {
        this.id = id;
        this.date = date;
        this.powerConsumed = powerConsumed;
        this.faultDetected = faultDetected;
    }

    public int getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getPowerConsumed() {
        return powerConsumed;
    }

    public boolean isFaultDetected() {
        return faultDetected;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Date: %s, Power: %.2f kWh, Fault: %s",
                id, date.format(DateTimeFormatter.ISO_DATE), powerConsumed, faultDetected);
    }
}