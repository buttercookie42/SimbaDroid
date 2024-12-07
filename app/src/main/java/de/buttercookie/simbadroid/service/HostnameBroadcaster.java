/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.service;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Objects;

import javax.jmdns.JmDNS;

import de.buttercookie.simbadroid.util.ThreadUtils;

/**
 * Advertises a custom .local hostname via mDNS
 */
public class HostnameBroadcaster {
    private static final String LOGTAG = "mDNS";

    private static JmDNS sJmDNS;
    private static String sHostname;

    private HostnameBroadcaster() {}

    /**
     * Set the hostname to be advertised via mDNS.
     *
     * @param addr The IP address to bind to. If <code>null</code>, JmDNS's auto-binding will be
     *             used.
     * @param hostname The hostname to advertise via mDNS. If <code>null</code>, the broadcaster is
     *                 shut down.
     */
    static void setHostname(@Nullable InetAddress addr, @Nullable String hostname) {
        ThreadUtils.postToBackgroundThread(() -> {
            if (Objects.equals(sHostname, hostname)) {
                return;
            }

            if (sJmDNS != null) {
                try {
                    sJmDNS.close();
                } catch (IOException e) {
                    Log.d(LOGTAG, "Error stopping mDNS hostname broadcaster", e);
                }
                sJmDNS = null;
            }

            if (hostname != null) {
                try {
                    sJmDNS = JmDNS.create(addr, hostname);
                } catch (IOException e) {
                    Log.d(LOGTAG, "Error starting mDNS hostname broadcaster", e);
                }
            }

            sHostname = hostname;
        });
    }
}
