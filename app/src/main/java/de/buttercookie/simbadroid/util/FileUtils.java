/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Arrays;

public class FileUtils {
    private static final String LOGTAG = "FileUtils";

    private static final String TRASHCAN_FOLDER  = ".Trashcan";

    private FileUtils() {}

    public static File getTrashcanPath(Context context, @NonNull File baseVolume) {
        File target = baseVolume;
        // The trashcan folder needs to reside on the same FileStore so that files can simply be
        // moved into the trashcan by renaming them. On removable SD cards that's the case even when
        // using the external cache directory (because a removable SD card is just one single file
        // system), but on the internal storage the general part (getExternalStorageDirectory())
        // resides on a different FileStore than the app-private directories, making the latter
        // unsuitable as a trashcan location.
        if (!Environment.isExternalStorageEmulated(baseVolume)) {
            File[] externalStorage = context.getExternalCacheDirs();
            target = Arrays.stream(externalStorage).filter(file -> isAncestor(baseVolume, file))
                    .findFirst().orElse(baseVolume);
        }

        target = new File(target, TRASHCAN_FOLDER);
        if (!ensureDir(target)) {
            target = null;
            Log.w(LOGTAG, "Couldn't create trashcan folder!");
        }
        return target;
    }

    private static boolean isAncestor(File ancestor, File target) {
        return target.getAbsolutePath().startsWith(ancestor.getAbsolutePath());
    }

    private static boolean ensureDir(File dir) {
        return (!dir.exists() || !dir.isFile()) && (dir.isDirectory() || dir.mkdir());
    }

}