package com.ccdev.opcua_client.wrappers;

import org.opcfoundation.ua.core.CreateSubscriptionRequest;
import org.opcfoundation.ua.core.CreateSubscriptionResponse;

import java.util.ArrayList;

public class ExtendedSubscription {

    CreateSubscriptionRequest request;
    CreateSubscriptionResponse response;

    ArrayList<ExtendedMonitoredItem> monitoredItems;

    public ExtendedSubscription(CreateSubscriptionRequest request, CreateSubscriptionResponse response) {
        this.request = request;
        this.response = response;
    }

    public CreateSubscriptionRequest getRequest() {
        return request;
    }

    public CreateSubscriptionResponse getResponse() {
        return response;
    }

    public ArrayList<ExtendedMonitoredItem> getMonitoredItems() {
        return monitoredItems;
    }
}
