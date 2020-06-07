package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Sensor extends CustomizedElement {

    double minValue;
    double maxValue;

    public Sensor(ExtendedMonitoredItem monitoredItem, String name) {
        super(monitoredItem, name);
        this.minValue = -1;
        this.maxValue = -1;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }
}
