package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Sensor extends CustomizedElement {

    public Sensor(int resourceId, ExtendedMonitoredItem monitoredItem) {
        super(resourceId, monitoredItem);
    }

}
