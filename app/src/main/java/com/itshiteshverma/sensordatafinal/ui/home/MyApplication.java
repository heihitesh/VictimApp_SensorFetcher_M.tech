package com.itshiteshverma.sensordatafinal.ui.home;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;


public class MyApplication extends android.app.Application {

    public static final String NOTIFICATION_CHANNEL_AUTO_BACKUP = "SENSOR_DATA";
    private static MyApplication mInstance;

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        createNotificationChannels();

    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(
                    NOTIFICATION_CHANNEL_AUTO_BACKUP,
                    "Auto BackUp",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel1.setDescription("Used To Display The AutoBackUp Status");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel1);
            }
        }
    }


}
