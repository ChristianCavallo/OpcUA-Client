package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Tank extends  CustomizedElement {

    double minValue;
    double maxValue;

    public Tank(ExtendedMonitoredItem monitoredItem, String name) {
        super(monitoredItem, name);
        this.minValue = 0;
        this.maxValue = 100;
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
