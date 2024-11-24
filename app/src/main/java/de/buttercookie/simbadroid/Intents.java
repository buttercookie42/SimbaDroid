/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid;

public class Intents {
    private static final String prefix = BuildConfig.APPLICATION_ID;

    // Start the SMB server
    public static final String ACTION_START = prefix + ".START_SERVER";
    // Stop the SMB server
    public static final String ACTION_STOP = prefix + ".STOP_SERVER";

    private Intents() {}
}
