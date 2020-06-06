package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Valve extends CustomizedElement {

    String openValue;
    String closedValue;

    public Valve(ExtendedMonitoredItem monitoredItem, String name) {
        super(R.drawable.ic_valve, monitoredItem, name);
        openValue = "true";
        closedValue = "false";
    }

    public String getOpenValue() {
        return openValue;
    }

    public void setOpenValue(String openValue) {
        this.openValue = openValue;
    }

    public String getClosedValue() {
        return closedValue;
    }

    public void setClosedValue(String closedValue) {
        this.closedValue = closedValue;
    }

}
