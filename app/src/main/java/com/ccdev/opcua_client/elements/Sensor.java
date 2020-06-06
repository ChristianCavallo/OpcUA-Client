package com.ccdev.opcua_client.elements;

import com.ccdev.opcua_client.R;
import com.ccdev.opcua_client.wrappers.ExtendedMonitoredItem;

public class Sensor extends CustomizedElement {

    public Sensor(ExtendedMonitoredItem monitoredItem, String name) {
        super(R.drawable.ic_reply_white_24dp, monitoredItem, name);
    }

}
