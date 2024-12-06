/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.util.Log;

import org.filesys.server.core.DeviceContextException;
import org.filesys.smb.server.disk.JavaNIODeviceContext;
import org.springframework.extensions.config.ConfigElement;

import java.io.File;

public class SimbaDiskDeviceContext extends JavaNIODeviceContext {
    private static final String LOGTAG = "SimbaDiskDeviceContext";

    public SimbaDiskDeviceContext(String name, ConfigElement args) throws DeviceContextException {
        super(name, args);
    }

    @Override
    protected boolean isTrashcanOnSameVolume(File rootDir, File trashCan) {
        boolean result;
        try {
            StructStat rootStat = Os.stat(rootDir.getAbsolutePath());
            StructStat trashStat = Os.stat(trashCan.getAbsolutePath());
            result = rootStat.st_dev == trashStat.st_dev;
        } catch (ErrnoException e) {
            result = false;
        }
        return result;
    }

    public void removeTrashcanFolderIfEmpty() {
        if (hasTrashFolder()) {
            if (!getTrashFolder().delete()) {
                Log.d(LOGTAG, "Couldn't delete trashcan folder - maybe not empty?");
            }
        }
    }
}
