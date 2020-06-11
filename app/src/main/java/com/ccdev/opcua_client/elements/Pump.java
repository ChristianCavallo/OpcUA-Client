package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Pump extends CustomizedElement {

    double maxValue;
    double minValue;

    public Pump(ExtendedMonitoredItem monitoredItem, String name) {
        super(monitoredItem, name);
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }
}
