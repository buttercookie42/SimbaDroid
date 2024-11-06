/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import org.filesys.server.filesys.DiskDeviceContext;
import org.filesys.server.filesys.DiskSizeInterface;
import org.filesys.server.filesys.SrvDiskInfo;
import org.filesys.smb.server.disk.JavaNIODiskDriver;

import java.io.IOException;

public class DiskDriver extends JavaNIODiskDriver implements DiskSizeInterface {

    @Override
    public void getDiskInformation(DiskDeviceContext ctx, SrvDiskInfo diskDev) throws IOException {
        diskDev.setBlockSize(512);
        diskDev.setBlocksPerAllocationUnit(64);
        diskDev.setTotalUnits(2560000);
        diskDev.setFreeUnits(2304000);
    }
}
