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
    ArrayList<MonitoredItemNotification> notifications;

    public ExtendedMonitoredItem(String name, int id, CreateMonitoredItemsRequest request, MonitoredItemCreateResult monitoredItem) {
        this.request = request;
        this.monitoredItem = monitoredItem;
        this.notifications = new ArrayList<>(notifiesListSize);
        this.id = id;
        this.nodeName = name;
    }

    public CreateMonitoredItemsRequest getRequest() {
        return request;
    }

    public MonitoredItemCreateResult getMonitoredItem() {
        return monitoredItem;
    }

    public ArrayList<MonitoredItemNotification> getNotifications() {
        return notifications;
    }

    public synchronized void addRead(MonitoredItemNotification n) {
        if (notifications.size() == notifiesListSize) {
            notifications.remove(notifications.size() - 1);
        }
        notifications.add(0, n);
    }

    public void reset() {
        notifications.clear();
    }

    public int getId() {
        return id;
    }

    public String getNodeName() {
        return nodeName;
    }
}
