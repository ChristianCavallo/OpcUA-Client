package com.ccdev.opcua_client.wrappers;

import org.opcfoundation.ua.core.CreateSubscriptionRequest;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;

import java.util.ArrayList;

public class ExtendedSubscription {

    String name;

    CreateSubscriptionRequest request;
    CreateSubscriptionResponse response;

    ArrayList<ExtendedMonitoredItem> monitoredItems;

    public ExtendedSubscription(String name, CreateSubscriptionRequest request) {
        this.name = name;
        this.request = request;
        monitoredItems = new ArrayList<>();
    }

    public CreateSubscriptionRequest getRequest() {
        return request;
    }

    public CreateSubscriptionResponse getResponse() {
        return response;
    }

    public String getName() {
        return name;
    }

    public ArrayList<ExtendedMonitoredItem> getMonitoredItems() {
        return monitoredItems;
    }

    public void setResponse(CreateSubscriptionResponse response) {
        this.response = response;
    }
}
