/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.os.StatFs;

import org.filesys.server.core.DeviceContextException;
import org.filesys.server.filesys.DiskDeviceContext;
import org.filesys.server.filesys.DiskSizeInterface;
import org.filesys.server.filesys.SrvDiskInfo;
import org.filesys.smb.server.disk.JavaNIODeviceContext;
import org.filesys.smb.server.disk.JavaNIODiskDriver;
import org.springframework.extensions.config.ConfigElement;

public class SimbaDiskDriver extends JavaNIODiskDriver implements DiskSizeInterface {

    private static final int BLOCK_SIZE = 512;

    @Override
    public void getDiskInformation(DiskDeviceContext ctx, SrvDiskInfo diskDev) {
        StatFs statFs = new StatFs(ctx.getDeviceName());

        diskDev.setBlockSize(BLOCK_SIZE);
        diskDev.setBlocksPerAllocationUnit(statFs.getBlockSizeLong() / BLOCK_SIZE);
        diskDev.setTotalUnits(statFs.getBlockCountLong());
        diskDev.setFreeUnits(statFs.getAvailableBlocksLong());
    }

    @Override
    protected JavaNIODeviceContext createJavaNIODeviceContext(String shareName, ConfigElement args)
            throws DeviceContextException {
        return new SimbaDiskDeviceContext(shareName, args);
    }
}
