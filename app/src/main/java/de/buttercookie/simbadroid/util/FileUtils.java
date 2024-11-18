/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.util;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageVolume;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public class FileUtils {
    public static String getStoragePath(final @NonNull Context context) {
        String storagePath = getSdCardStoragePath(context, null);
        if (storagePath == null) {
            storagePath = Environment.getExternalStorageDirectory().toString();
        }
        return storagePath;
    }

    /**
     * Attempts to find the root path of an external (removable) SD card.
     *
     * @param uuid If you know the file system UUID (as returned e.g. by
     *             {@link StorageVolume#getUuid()}) of the storage device you're looking for, this
     *             may be used to filter down the selection of available non-emulated storage
     *             devices. If no storage device matching the given UUID was found, the first
     *             non-emulated storage device will be returned.
     * @return The root path of the storage device, or <code>null</code> if none could be found.
     */
    // Imported from https://hg.mozilla.org/releases/mozilla-esr68/file/bb3f440689f395ed15d6a6177650c0b0ad37146a/mobile/android/geckoview/src/main/java/org/mozilla/gecko/util/FileUtils.java
    public static @Nullable String getSdCardStoragePath(final @NonNull Context context,
                                                        final @Nullable String uuid) {
        // Since around the time of Lollipop or Marshmallow, the common convention is for external
        // SD cards to be mounted at /storage/<file system UUID>/, however this pattern is still not
        // guaranteed to be 100 % reliable. Therefore we need another way of getting all potential
        // mount points for external storage devices.
        // StorageManager.getStorageVolumes() might possibly do the trick and be just what we need
        // to enumerate all mount points, but it only works on API24+.
        // So instead, we use the output of getExternalFilesDirs for this purpose, which works on
        // API19 and up.
        File[] externalStorages = context.getExternalFilesDirs(null);
        String uuidDir = !TextUtils.isEmpty(uuid) ? '/' + uuid + '/' : null;

        String firstNonEmulatedStorage = null;
        String targetStorage = null;
        for (File externalStorage : externalStorages) {
            if (Environment.isExternalStorageEmulated(externalStorage)) {
                // The paths returned by getExternalFilesDirs also include locations that actually
                // sit on the internal "external" storage, so we need to filter them out again.
                continue;
            }
            String storagePath = externalStorage.getAbsolutePath();
            /* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
             * NOTE: This is our big assumption in this function: That the folders returned by   *
             * context.getExternalFilesDir() will always be located somewhere inside             *
             * /<storage root path>/Android/<app specific directories>, so that we can retrieve  *
             * the storage root by simply snipping off everything starting from "/Android".      *
             * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
            storagePath = storagePath.substring(0, storagePath.indexOf("/Android"));
            if (firstNonEmulatedStorage == null) {
                firstNonEmulatedStorage = storagePath;
            }
            if (!TextUtils.isEmpty(uuidDir) && storagePath.contains(uuidDir)) {
                targetStorage = storagePath;
                break;
            }
        }
        if (targetStorage == null) {
            // Either no UUID to narrow down the selection was given, or else this device doesn't
            // mount its SD cards using the file system UUID, so we just fall back to the first
            // non-emulated storage path we found.
            targetStorage = firstNonEmulatedStorage;
        }
        return targetStorage;
    }
}