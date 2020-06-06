package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public abstract class CustomizedElement {

    int resourceId;
    ExtendedMonitoredItem monitoredItem;

    public CustomizedElement(int resourceId, ExtendedMonitoredItem monitoredItem) {
        this.resourceId = resourceId;
        this.monitoredItem = monitoredItem;
    }

    public int getResourceId() {
        return resourceId;
    }

    public ExtendedMonitoredItem getMonitoredItem() {
        return monitoredItem;
    }
}
