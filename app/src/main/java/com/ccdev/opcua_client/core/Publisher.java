package com.ccdev.opcua_client.core;

import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.PublishRequest;
import org.opcfoundation.ua.core.PublishResponse;
import org.opcfoundation.ua.core.SubscriptionAcknowledgement;

public class Publisher implements Runnable {
    boolean isEnabled;

    public Publisher() {
        this.isEnabled = true;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public void run() {
        SubscriptionAcknowledgement[] acks = null;
        while(isEnabled){

            if(Core.getInstance().getSubscriptions().size() > 0){

                acks = new SubscriptionAcknowledgement[Core.getInstance().getSubscriptions().size()];
                for (int i = 0; i < Core.getInstance().getSubscriptions().size(); i++) {
                    acks[i] = Core.getInstance().getSubscriptions().get(i).getAck();
                }

                PublishRequest request = new PublishRequest(null, acks);
                try {
                    PublishResponse response = Core.getInstance().getSessionChannel().Publish(request);
                    Core.getInstance().updateSubscription(response);
                    if(response.getNotificationMessage().getNotificationData().length > 0){
                        Core.getInstance().notifyUpdate();
                    }
                } catch (ServiceResultException e) {
                    e.printStackTrace();
                }


            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
