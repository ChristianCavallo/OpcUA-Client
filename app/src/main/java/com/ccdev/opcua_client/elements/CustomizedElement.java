package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public abstract class CustomizedElement {

    int resourceId;
    ExtendedMonitoredItem monitoredItem;
    String name;

    public CustomizedElement(int resourceId, ExtendedMonitoredItem monitoredItem, String name) {
        this.resourceId = resourceId;
        this.monitoredItem = monitoredItem;
        this.name = name;
    }

    public int getResourceId() {
        return resourceId;
    }

    public ExtendedMonitoredItem getMonitoredItem() {
        return monitoredItem;
    }

    public String getName() {
        return name;
    }
}
