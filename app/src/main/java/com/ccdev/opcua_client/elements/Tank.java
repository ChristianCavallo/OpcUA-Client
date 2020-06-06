package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Tank extends  CustomizedElement {

    int minValue;
    int maxValue;

    public Tank(ExtendedMonitoredItem monitoredItem, String name) {
        super(R.drawable.ic_tank, monitoredItem, name);
        this.minValue = 0;
        this.maxValue = 100;
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
}
