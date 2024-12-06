/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.content.Context;
import android.net.LinkAddress;

import com.google.common.util.concurrent.Monitor;

import org.filesys.netbios.server.NetBIOSNameServer;
import org.filesys.server.NetworkServer;
import org.filesys.server.config.InvalidConfigurationException;
import org.filesys.smb.server.SMBServer;

import de.buttercookie.simbadroid.util.ThreadUtils;

public class JLANFileServer {
    private final JLANFileServerConfiguration mCfg;
    private boolean mStarted = false;
    private final Monitor startupMonitor = new Monitor();

    public JLANFileServer(Context context, String hostName) throws Exception {
        mCfg = new JLANFileServerConfiguration(context, hostName);
    }

    public void start() {
        ThreadUtils.assertOnUiThread();
        startupMonitor.enter();
        try {
            if (mStarted) {
                return;
            }

            try {
                mCfg.addServer(new NetBIOSNameServer(mCfg));
                mCfg.addServer(new SMBServer(mCfg));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < mCfg.numberOfServers(); i++) {
                NetworkServer server = mCfg.getServer(i);
                server.startServer();
            }
            mStarted = true;
        } finally {
            startupMonitor.leave();
        }
    }

    public void stop() {
        ThreadUtils.assertOnUiThread();
        if (!mStarted) {
            return;
        }

        for (int i = 0; i < mCfg.numberOfServers(); i++) {
            NetworkServer server = mCfg.getServer(i);
            server.shutdownServer(false);
        }
        mCfg.removeAllServers();
        ThreadUtils.postToBackgroundThread(this::tryRemoveTrashcanFolders);
        mStarted = false;
    }

    public boolean running() {
        return mStarted;
    }

    public void setBindAddress(LinkAddress address) {
        try {
            mCfg.setBindAddress(address);
        } catch (InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void tryRemoveTrashcanFolders() {
        // If the server is already starting up again, there's no point in trying to remove the
        // trashcan folders.
        if (startupMonitor.tryEnter()) {
            try {
                if (!mStarted) {
                    mCfg.removeTrashcanFolders();
                }
            } finally {
                startupMonitor.leave();
            }
        }
    }
}
