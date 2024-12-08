/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import de.buttercookie.simbadroid.service.SmbService.SmbBinder;

public class SmbServiceConnection implements ServiceConnection {
    private SmbService mService;

    public SmbService getService() {
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
