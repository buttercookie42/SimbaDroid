/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.topjohnwu.superuser.Shell;
import de.buttercookie.simbadroid.MainActivity;
import de.buttercookie.simbadroid.R;

public class SmbService extends Service {
    private static final String LOGTAG = "SmbService";

    private static final String NOTIFICATION_CHANNEL = SmbService.class.getName();
    private static final int NOTIFICATION_ID = 1;

    private final IBinder binder = new SmbBinder();

    public class SmbBinder extends Binder {
        SmbService getService() {
            return SmbService.this;
        }
    }

    static {
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @SuppressLint("InlinedApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("The SMB server is running.")
                .setContentIntent(pendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOngoing(true)
                .build();
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);

        return START_NOT_STICKY;
    }

    public void stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "SMB Server";
            String description = "SMB server service notification";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
            channel.setDescription(description);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
