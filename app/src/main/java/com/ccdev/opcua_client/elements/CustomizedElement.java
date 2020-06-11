package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public abstract class CustomizedElement {

    public enum VisualizationType {
        PROGRESS_BAR,
        INDICATOR,
        BOTH
    }

    ExtendedMonitoredItem monitoredItem;
    String name;
    String unit;
    VisualizationType visualization;

    public CustomizedElement(ExtendedMonitoredItem monitoredItem, String name) {
        this.monitoredItem = monitoredItem;
        this.name = name;
        this.unit = "";
        this.visualization = VisualizationType.PROGRESS_BAR;
    }

    public ExtendedMonitoredItem getMonitoredItem() {
        return monitoredItem;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public VisualizationType getVisualization() {
        return visualization;
    }

    public void setVisualization(VisualizationType visualization) {
        this.visualization = visualization;
    }
}
