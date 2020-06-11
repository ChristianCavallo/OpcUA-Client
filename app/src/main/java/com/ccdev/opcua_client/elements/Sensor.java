package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Sensor extends CustomizedElement {

    public enum Category {
        GENERIC,
        TEMPERATURE,
        PRESSURE,
        HUMIDITY
    }

    double minValue;
    double maxValue;
    Category category;

    public Sensor(ExtendedMonitoredItem monitoredItem, String name, Category category) {
        super(monitoredItem, name);
        this.minValue = -1;
        this.maxValue = -1;
        this.category = category;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
