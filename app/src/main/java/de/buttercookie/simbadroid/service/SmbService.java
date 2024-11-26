/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.service;

import static de.buttercookie.simbadroid.Intents.ACTION_STOP;
import static de.buttercookie.simbadroid.util.Iptables.iptables;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import de.buttercookie.simbadroid.MainActivity;
import de.buttercookie.simbadroid.R;
import de.buttercookie.simbadroid.jlan.JLANFileServer;
import de.buttercookie.simbadroid.permissions.Permissions;
import de.buttercookie.simbadroid.util.ThreadUtils;

public class SmbService extends Service {
    private static final String LOGTAG = "SmbService";

    private static final String NOTIFICATION_CHANNEL = SmbService.class.getName();
    private static final int NOTIFICATION_ID = 1;
    private static final long WIFI_UNAVAILABLE_STARTUP_TIMEOUT_MS = 5 * 60 * 1000;
    private static final long WIFI_UNAVAILABLE_TIMEOUT_MS = 20 * 60 * 1000;

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
    private Runnable mWifiTimeoutRunnable;
    private long mWifiTimeoutMs = WIFI_UNAVAILABLE_STARTUP_TIMEOUT_MS;

    private void setIsRunning(boolean isRunning) {
        if (mRunning != isRunning) {
            mRunning = isRunning;
            updateServerState();
        }
    }

    private void setWifiAvailable(boolean wifiAvailable) {
        if (mWifiAvailable != wifiAvailable) {
            mWifiAvailable = wifiAvailable;
            if (wifiAvailable) {
                mWifiTimeoutMs = WIFI_UNAVAILABLE_TIMEOUT_MS;
            }
            updateServerState();
        }
    }

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
        if (handleAction(intent) || mRunning) {
            return START_NOT_STICKY;
        }

        try {
            mServer = new JLANFileServer(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Notification notification = getServiceNotification();
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
        acquireLocks();

        iptables(false, "nat", "A", "PREROUTING -p tcp --dport 445 -j REDIRECT --to-port 4450");
        iptables(false, "nat", "A", "PREROUTING -p udp --dport 137 -j REDIRECT --to-port 1137");
        iptables(false, "nat", "A", "PREROUTING -p udp --dport 138 -j REDIRECT --to-port 1138");
        iptables(false, "nat", "A", "PREROUTING -p tcp --dport 139 -j REDIRECT --to-port 1139");

        setIsRunning(true);

        return START_NOT_STICKY;
    }

    private boolean handleAction(Intent intent) {
        boolean handled = false;
        final String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stop();
            handled = true;
        }
        return handled;
    }

    public void stop() {
        if (!mRunning) {
            return;
        }

        setIsRunning(false);

        iptables(false, "nat", "D", "PREROUTING -p tcp --dport 445 -j REDIRECT --to-port 4450");
        iptables(false, "nat", "D", "PREROUTING -p udp --dport 137 -j REDIRECT --to-port 1137");
        iptables(false, "nat", "D", "PREROUTING -p udp --dport 138 -j REDIRECT --to-port 1138");
        iptables(false, "nat", "D", "PREROUTING -p tcp --dport 139 -j REDIRECT --to-port 1139");

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        releaseLocks();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        unmonitorWifi();
        stopWifiTimeout();
        super.onDestroy();
    }

    public boolean isWifiAvailable() {
        return mWifiAvailable;
    }

    private void updateServerState() {
        if (mWifiAvailable) {
            stopWifiTimeout();
        } else {
            startWifiTimeout();
        }

        if (mServer == null) {
            return;
        }

        if (mRunning && mWifiAvailable) {
            Log.d(LOGTAG, "Starting SMB server");
            mServer.start();
            getSystemService(NotificationManager.class)
                    .notify(NOTIFICATION_ID, getServiceNotification());
        } else {
            Log.d(LOGTAG, "Stopping SMB server");
            mServer.stop();
            if (!mWifiAvailable) {
                getSystemService(NotificationManager.class)
                        .notify(NOTIFICATION_ID, getServiceNotification());
            }
        }
    }

    private void startWifiTimeout() {
        stopWifiTimeout();
        mWifiTimeoutRunnable = this::stop;
        ThreadUtils.postDelayedToUiThread(mWifiTimeoutRunnable, mWifiTimeoutMs);
    }

    private void stopWifiTimeout() {
        if (mWifiTimeoutRunnable != null) {
            ThreadUtils.removeCallbacksFromUiThread(mWifiTimeoutRunnable);
            mWifiTimeoutRunnable = null;
        }
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
                    setWifiAvailable(true);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    setWifiAvailable(false);
                    connMgr.bindProcessToNetwork(null);
                }
            };
        }
        connMgr.requestNetwork(
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                mNetCallback
        );
    }

    private void unmonitorWifi() {
        if (mNetCallback != null) {
            ConnectivityManager connMgr = getSystemService(ConnectivityManager.class);
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

    private Notification getServiceNotification() {
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                activityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action stopAction = getStopAction();
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getServiceNotificationText())
                .setContentIntent(pendingIntent)
                .addAction(stopAction)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOngoing(true)
                .build();
    }

    private @Nullable NotificationCompat.Action getStopAction() {
        NotificationCompat.Action action = null;
        if (mWifiAvailable) {
            Intent stopIntent = new Intent(this, SmbService.class)
                    .setAction(ACTION_STOP);
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0,
                    stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            action = new NotificationCompat.Action.Builder(R.drawable.ic_stop,
                    getString(R.string.action_stop), pendingIntent)
                    .build();
        }
        return action;
    }

    private String getServiceNotificationText() {
        return mWifiAvailable ?
                getString(R.string.message_server_running) :
                getString(R.string.message_server_waiting_wifi);
    }

    /**
     * Start the service, optionally prompting for the necessary permissions.
     *
     * @param context Must be an <code>Activity</code> context if <code>promptForPermissions</code>
     *                is <code>true</code>.
     * @param promptForPermissions Whether to prompt for the required Android permissions.
     */
    @SuppressLint("InlinedApi")
    public static void startService(final Context context, final boolean promptForPermissions) {
        Intent intent = new Intent(context, SmbService.class);

        var notifPerm = Permissions.from(context);
        if (!promptForPermissions) {
            notifPerm.doNotPrompt();
        }
        notifPerm.withPermissions(Manifest.permission.POST_NOTIFICATIONS)
                .alwaysRun(() -> ThreadUtils.postToUiThread(() -> {
                    String storagePermission =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                                    Manifest.permission.MANAGE_EXTERNAL_STORAGE :
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE;
                    var storagePerm = Permissions.from(context);
                    if (!promptForPermissions) {
                        storagePerm.doNotPrompt();
                    }
                    storagePerm.withPermissions(storagePermission)
                            .andFallback(() -> Toast.makeText(context,
                                    R.string.toast_need_storage_permission,
                                    Toast.LENGTH_SHORT).show())
                            .run(() -> context.startService(intent));
                }));
    }
}
