package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Pump extends  CustomizedElement {

    double maxRPM;
    double minRPM;

    public Pump(ExtendedMonitoredItem monitoredItem) {
        super(R.drawable.ic_folder_24dp, monitoredItem);
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
