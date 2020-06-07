package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public abstract class CustomizedElement {

    ExtendedMonitoredItem monitoredItem;
    String name;

    public CustomizedElement(ExtendedMonitoredItem monitoredItem, String name) {
        this.monitoredItem = monitoredItem;
        this.name = name;
    }

    public ExtendedMonitoredItem getMonitoredItem() {
        return monitoredItem;
    }

    public String getName() {
        return name;
    }
}
