package com.example.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class JunctionState {
    private String junctionId;
    private int lane1Vehicles;
    private int lane2Vehicles;
    private int lane3Vehicles;
    private int lane4Vehicles;
    private int greenLaneId;
    private LocalDateTime lastUpdated;

    public JunctionState(String junctionId, int lane1Vehicles, int lane2Vehicles, int lane3Vehicles, int lane4Vehicles, int greenLaneId, Timestamp lastUpdatedTimestamp) {
        this.junctionId = junctionId;
        this.lane1Vehicles = lane1Vehicles;
        this.lane2Vehicles = lane2Vehicles;
        this.lane3Vehicles = lane3Vehicles;
        this.lane4Vehicles = lane4Vehicles;
        this.greenLaneId = greenLaneId;
        this.lastUpdated = (lastUpdatedTimestamp != null) ? lastUpdatedTimestamp.toLocalDateTime() : null;
    }

    public String getJunctionId() { return junctionId; }
    public int getLane1Vehicles() { return lane1Vehicles; }
    public int getLane2Vehicles() { return lane2Vehicles; }
    public int getLane3Vehicles() { return lane3Vehicles; }
    public int getLane4Vehicles() { return lane4Vehicles; }
    public int getGreenLaneId() { return greenLaneId; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }

    @Override
    public String toString() {
        return "JunctionState{" +
                "junctionId='" + junctionId + '\'' +
                ", lane1Vehicles=" + lane1Vehicles +
                ", lane2Vehicles=" + lane2Vehicles +
                ", lane3Vehicles=" + lane3Vehicles +
                ", lane4Vehicles=" + lane4Vehicles +
                ", greenLaneId=" + greenLaneId +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}