package com.ccdev.opcua_client;

public interface CoreInterface {

    void onSubscriptionCreated();

    void onSubscriptionRemoved();

    void onMonitorItemAdded();

    void onMonitorItemRemoved();

}
