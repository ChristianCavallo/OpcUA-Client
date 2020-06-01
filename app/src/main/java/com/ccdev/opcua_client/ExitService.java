package com.ccdev.opcua_client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class ExitService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Core.getInstance().ShutDown();
            }
        }).start();
    }
}
