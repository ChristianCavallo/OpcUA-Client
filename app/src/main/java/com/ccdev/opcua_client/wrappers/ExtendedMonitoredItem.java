package com.ccdev.opcua_client.wrappers;

import org.opcfoundation.ua.core.CreateMonitoredItemsRequest;
import org.opcfoundation.ua.core.MonitoredItemCreateResult;
import org.opcfoundation.ua.core.MonitoredItemNotification;

import java.util.ArrayList;

public class ExtendedMonitoredItem {

    CreateMonitoredItemsRequest request;
    MonitoredItemCreateResult monitoredItem;
    int id;
    ArrayList<MonitoredItemNotification> notifies;

    public ExtendedMonitoredItem(int id, CreateMonitoredItemsRequest request, MonitoredItemCreateResult monitoredItem) {
        this.request = request;
        this.monitoredItem = monitoredItem;
        this.notifies = new ArrayList<>(monitoredItem.getRevisedQueueSize().intValue());
        this.id = id;
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

    public void addRead(MonitoredItemNotification n){
        if(notifies.size() == monitoredItem.getRevisedQueueSize().intValue()){
            notifies.remove(0);
        }
        notifies.add(n);
    }

    public void reset(){
        notifies.clear();
    }

    public int getId() {
        return id;
    }
}
