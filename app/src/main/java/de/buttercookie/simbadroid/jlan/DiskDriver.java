/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.os.StatFs;

import org.filesys.server.filesys.DiskDeviceContext;
import org.filesys.server.filesys.DiskSizeInterface;
import org.filesys.server.filesys.SrvDiskInfo;
import org.filesys.smb.server.disk.JavaNIODiskDriver;

public class DiskDriver extends JavaNIODiskDriver implements DiskSizeInterface {
    private static final int BLOCK_SIZE = 512;

    @Override
    public void getDiskInformation(DiskDeviceContext ctx, SrvDiskInfo diskDev) {
        StatFs statFs = new StatFs(ctx.getDeviceName());

        diskDev.setBlockSize(BLOCK_SIZE);
        diskDev.setBlocksPerAllocationUnit(statFs.getBlockSizeLong() / BLOCK_SIZE);
        diskDev.setTotalUnits(statFs.getBlockCountLong());
        diskDev.setFreeUnits(statFs.getAvailableBlocksLong());
    }
}
