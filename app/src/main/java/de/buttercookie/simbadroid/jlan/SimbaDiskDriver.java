/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.jlan;

import android.os.StatFs;
import android.util.Log;

import org.filesys.server.SrvSession;
import org.filesys.server.core.DeviceContext;
import org.filesys.server.core.DeviceContextException;
import org.filesys.server.filesys.DiskDeviceContext;
import org.filesys.server.filesys.DiskSizeInterface;
import org.filesys.server.filesys.FileName;
import org.filesys.server.filesys.NetworkFile;
import org.filesys.server.filesys.SrvDiskInfo;
import org.filesys.server.filesys.TreeConnection;
import org.filesys.smb.server.disk.JavaNIODeviceContext;
import org.filesys.smb.server.disk.JavaNIODiskDriver;
import org.springframework.extensions.config.ConfigElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

public class SimbaDiskDriver extends JavaNIODiskDriver implements DiskSizeInterface {
    private static final String LOGTAG = "SimbaDiskDriver";

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
    public void renameFile(SrvSession sess, TreeConnection tree, String oldName, String newName, NetworkFile netFile) throws IOException {
        DeviceContext context = tree.getContext();
        Path oldPath = Paths.get( FileName.buildPath(context.getDeviceName(), oldName, null, java.io.File.separatorChar));
        Path newPath = Paths.get( FileName.buildPath(context.getDeviceName(), newName, null, java.io.File.separatorChar));

        // Android sometimes (e.g. on my phone's removable SD card) pretends to preserve the last
        // modified time, but actually only does so in memory while apparently still updating the
        // on-disk file system. This means that after the next reboot at latest, the last modified
        // date will suddenly change to the time of renaming after all. Hence we manually need to
        // act to preserve the last modified date across renames.
        FileTime lastMod;
        try {
            lastMod = Files.getLastModifiedTime(oldPath);
        } catch (IOException ex) {
            lastMod = FileTime.fromMillis(System.currentTimeMillis());
            Log.d(LOGTAG, "Couldn't get last modified date, falling back to current time");
        }
        super.renameFile(sess, tree, oldName, newName, netFile);

        // Because the in-memory file system cache keeps reporting the original last modified date,
        // we need to temporarily set a differing value in order to force-flush the correct time
        // back to the on-disk file system.
        Files.setLastModifiedTime(newPath, FileTime.fromMillis(lastMod.toMillis() - 42 * 1000));
        Files.setLastModifiedTime(newPath, lastMod);
    }

    @Override
    protected JavaNIODeviceContext createJavaNIODeviceContext(String shareName, ConfigElement args)
            throws DeviceContextException {
        return new SimbaDiskDeviceContext(shareName, args);
    }
}
