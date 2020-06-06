package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Pump extends  CustomizedElement {

    double maxRPM;
    double minRPM;

    public Pump(ExtendedMonitoredItem monitoredItem, String name) {
        super(R.drawable.ic_pump, monitoredItem, name);
        this.maxRPM = 10000;
        this.minRPM = 0;
    }

    public double getMaxRPM() {
        return maxRPM;
    }

    public void setMaxRPM(double maxRPM) {
        this.maxRPM = maxRPM;
    }

    public double getMinRPM() {
        return minRPM;
    }

    public void setMinRPM(double minRPM) {
        this.minRPM = minRPM;
    }
}
