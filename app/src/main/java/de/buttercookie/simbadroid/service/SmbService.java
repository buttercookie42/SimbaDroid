/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.service;

import static de.buttercookie.simbadroid.util.Iptables.iptables;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import de.buttercookie.simbadroid.MainActivity;
import de.buttercookie.simbadroid.R;
import de.buttercookie.simbadroid.jlan.JLANFileServer;

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

    private boolean mRunning = false;
    private boolean mWifiAvailable = false;

    private JLANFileServer mServer;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;
    private ConnectivityManager.NetworkCallback mNetCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initLocks();
        monitorWifi();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @SuppressLint("InlinedApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            mServer = new JLANFileServer(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.message_server_running))
                .setContentIntent(pendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOngoing(true)
                .build();
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
        acquireLocks();

        iptables(false, "nat", "A", "PREROUTING -p tcp --dport 445 -j REDIRECT --to-port 4450");
        iptables(false, "nat", "A", "PREROUTING -p udp --dport 137 -j REDIRECT --to-port 1137");
        iptables(false, "nat", "A", "PREROUTING -p udp --dport 138 -j REDIRECT --to-port 1138");
        iptables(false, "nat", "A", "PREROUTING -p tcp --dport 139 -j REDIRECT --to-port 1139");

        mServer.start();

        mRunning = true;
        return START_NOT_STICKY;
    }

    public void stop() {
        if (!mRunning) {
            return;
        }

        if (mServer != null) {
            mServer.stop();
        }

        iptables(false, "nat", "D", "PREROUTING -p tcp --dport 445 -j REDIRECT --to-port 4450");
        iptables(false, "nat", "D", "PREROUTING -p udp --dport 137 -j REDIRECT --to-port 1137");
        iptables(false, "nat", "D", "PREROUTING -p udp --dport 138 -j REDIRECT --to-port 1138");
        iptables(false, "nat", "D", "PREROUTING -p tcp --dport 139 -j REDIRECT --to-port 1139");

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        releaseLocks();
        stopSelf();
        mRunning = false;
    }

    @Override
    public void onDestroy() {
        unmonitorWifi();
        super.onDestroy();
    }

    public boolean isWifiAvailable() {
        return mWifiAvailable;
    }

    @SuppressWarnings("deprecation")
    private void initLocks() {
        final String tag = getString(R.string.app_name) + "::SmbService";

        PowerManager pwrMgr = getSystemService(PowerManager.class);
        mWakeLock = pwrMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);

        WifiManager wifiMgr = getSystemService(WifiManager.class);
        mWifiLock = wifiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL, tag);
    }

    @SuppressLint("WakelockTimeout")
    private void acquireLocks() {
        mWakeLock.acquire();
        mWifiLock.acquire();
    }

    private void releaseLocks() {
        mWifiLock.release();
        mWakeLock.release();
    }

    private void monitorWifi() {
        ConnectivityManager connMgr = getSystemService(ConnectivityManager.class);
        if (mNetCallback == null) {
            mNetCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    connMgr.bindProcessToNetwork(network);
                    mWifiAvailable = true;
                }

                @Override
                public void onLost(@NonNull Network network) {
                    mWifiAvailable = false;
                    connMgr.bindProcessToNetwork(null);
                }
            };
        }
        connMgr.requestNetwork(
                new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                mNetCallback
        );
    }

    private void unmonitorWifi() {
        ConnectivityManager connMgr = getSystemService(ConnectivityManager.class);
        if (mNetCallback != null) {
            mWifiAvailable = false;
            connMgr.unregisterNetworkCallback(mNetCallback);
            connMgr.bindProcessToNetwork(null);
            mNetCallback = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, importance);
            channel.setDescription(description);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
