/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid.util;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Arrays;

public class FileUtils {

    private static final String TRASHCAN_FOLDER  = ".Trashcan";

    private FileUtils() {}

    public static File getTrashcanPath(Context context, @NonNull File baseVolume) {
        File[] externalStorage = context.getExternalCacheDirs();
        File target = Arrays.stream(externalStorage).filter(file -> isAncestor(baseVolume, file))
                .findFirst().orElse(baseVolume);

        target = new File(target, TRASHCAN_FOLDER);
        if (!ensureDir(target)) {
            target = null;
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