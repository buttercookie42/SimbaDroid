/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.content.Context;

import org.filesys.netbios.server.NetBIOSNameServer;
import org.filesys.server.NetworkServer;
import org.filesys.server.config.ServerConfiguration;
import org.filesys.smb.server.SMBServer;

public class JLANFileServer {
    ServerConfiguration mCfg;

    public JLANFileServer(Context context) throws Exception {
        mCfg = new JLANFileServerConfiguration(context);
        mCfg.addServer(new NetBIOSNameServer(mCfg));
        mCfg.addServer(new SMBServer(mCfg));
    }

    public void start() {
        for (int i = 0; i < mCfg.numberOfServers(); i++) {
            NetworkServer server = mCfg.getServer(i);
            server.startServer();
        }
    }

    public void stop() {
        for (int i = 0; i < mCfg.numberOfServers(); i++) {
            NetworkServer server = mCfg.getServer(i);
            server.shutdownServer(false);
        }
    }
}
