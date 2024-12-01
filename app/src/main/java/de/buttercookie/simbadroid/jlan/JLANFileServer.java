/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.content.Context;
import android.net.LinkAddress;

import org.filesys.netbios.server.NetBIOSNameServer;
import org.filesys.server.NetworkServer;
import org.filesys.server.config.InvalidConfigurationException;
import org.filesys.smb.server.SMBServer;

public class JLANFileServer {
    JLANFileServerConfiguration mCfg;
    boolean mStarted = false;

    public JLANFileServer(Context context, String hostName) throws Exception {
        mCfg = new JLANFileServerConfiguration(context, hostName);
        mCfg.addServer(new NetBIOSNameServer(mCfg));
        mCfg.addServer(new SMBServer(mCfg));
    }

    public void start() {
        if (mStarted) {
            return;
        }

        for (int i = 0; i < mCfg.numberOfServers(); i++) {
            NetworkServer server = mCfg.getServer(i);
            server.startServer();
        }
        mStarted = true;
    }

    public void stop() {
        if (!mStarted) {
            return;
        }

        for (int i = 0; i < mCfg.numberOfServers(); i++) {
            NetworkServer server = mCfg.getServer(i);
            server.shutdownServer(false);
        }
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
}
