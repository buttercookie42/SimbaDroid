/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// Imported from https://hg.mozilla.org/releases/mozilla-esr68/file/bb3f440689f395ed15d6a6177650c0b0ad37146a/mobile/android/geckoview/src/main/java/org/mozilla/gecko/permissions/PermissionsHelper.java

package de.buttercookie.simbadroid.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/* package-private */ class PermissionsHelper {
    private static final int PERMISSIONS_REQUEST_CODE = 212;

    public boolean hasPermissions(final Context context, final String... permissions) {
        for (String permission : permissions) {
            final int permissionCheck = ContextCompat.checkSelfPermission(context, permission);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    public void prompt(final Activity activity, final String[] permissions) {
        ActivityCompat.requestPermissions(activity, permissions, PERMISSIONS_REQUEST_CODE);
    }

}