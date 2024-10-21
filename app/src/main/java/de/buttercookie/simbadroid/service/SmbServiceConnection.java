package de.buttercookie.simbadroid.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import de.buttercookie.simbadroid.service.SmbService.SmbBinder;

public class SmbServiceConnection implements ServiceConnection {
    private Service mService;

    public Service getService() {
        return mService;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        SmbBinder binder = (SmbBinder) service;
        mService = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }
}
