package com.ccdev.opcua_client.wrappers;

import org.opcfoundation.ua.core.CreateMonitoredItemsRequest;
import org.opcfoundation.ua.core.MonitoredItemCreateResult;
import org.opcfoundation.ua.core.MonitoredItemNotification;

import java.util.ArrayList;

public class ExtendedMonitoredItem {

    public static int notifiesListSize = 10; // 10 samples

    CreateMonitoredItemsRequest request;
    MonitoredItemCreateResult monitoredItem;
    String nodeName;
    int id;
    ArrayList<MonitoredItemNotification> notifies;

    public ExtendedMonitoredItem(String name, int id, CreateMonitoredItemsRequest request, MonitoredItemCreateResult monitoredItem) {
        this.request = request;
        this.monitoredItem = monitoredItem;
        this.notifies = new ArrayList<>(notifiesListSize);
        this.id = id;
        this.nodeName = name;
    }

    public CreateMonitoredItemsRequest getRequest() {
        return request;
    }

    public MonitoredItemCreateResult getMonitoredItem() {
        return monitoredItem;
    }

    public ArrayList<MonitoredItemNotification> getNotifies() {
        return notifies;
    }

    public void addRead(MonitoredItemNotification n) {
        if (notifies.size() == notifiesListSize) {
            notifies.remove(notifies.size() - 1);
        }
        notifies.add(0, n);
    }

    public void reset() {
        notifies.clear();
    }

    public int getId() {
        return id;
    }

    public String getNodeName() {
        return nodeName;
    }
}
