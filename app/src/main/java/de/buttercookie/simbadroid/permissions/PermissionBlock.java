/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// Imported from https://hg.mozilla.org/releases/mozilla-esr68/file/bb3f440689f395ed15d6a6177650c0b0ad37146a/mobile/android/geckoview/src/main/java/org/mozilla/gecko/permissions/PermissionBlock.java

package de.buttercookie.simbadroid.permissions;

import de.buttercookie.simbadroid.util.ThreadUtils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Helper class to run code blocks depending on whether a user has granted or denied certain runtime permissions.
 */
public class PermissionBlock {
    private final PermissionsHelper mHelper;

    private Context mContext;
    private String[] mPermissions;
    private boolean mOnUiThread;
    private boolean mOnBackgroundThread;
    private Runnable mOnPermissionsGranted;
    private Runnable mOnPermissionsDenied;
    private boolean mDoNotPrompt;

    /* package-private */ PermissionBlock(final Context context, final PermissionsHelper helper) {
        mContext = context;
        mHelper = helper;
    }

    /**
     * Determine whether the app has been granted the specified permissions.
     * <p>
     * Note: The MANAGE_EXTERNAL_STORAGE permission cannot be requested together with other
     * permissions.
     */
    public PermissionBlock withPermissions(final @NonNull String... permissions) {
        if (permissions.length > 1) {
            for (String permission : permissions) {
                if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
                    throw new IllegalStateException("MANAGE_EXTERNAL_STORAGE must be requested alone");
                }
            }
        }
        mPermissions = permissions;
        return this;
    }

    /**
     * Execute all callbacks on the UI thread.
     */
    public PermissionBlock onUIThread() {
        mOnUiThread = true;
        return this;
    }

    /**
     * Execute all callbacks on the background thread.
     */
    public PermissionBlock onBackgroundThread() {
        mOnBackgroundThread = true;
        return this;
    }

    /**
     * Do not prompt the user to accept the permission if it has not been granted yet.
     * This also guarantees that the callback will run on the current thread if no callback
     * thread has been explicitly specified.
     */
    public PermissionBlock doNotPrompt() {
        mDoNotPrompt = true;
        return this;
    }

    /**
     * If the condition is true then do not prompt the user to accept the permission if it has not
     * been granted yet.
     */
    public PermissionBlock doNotPromptIf(final boolean condition) {
        if (condition) {
            doNotPrompt();
        }

        return this;
    }

    /**
     * Execute this permission block. Calling this method will prompt the user if needed.
     */
    public void run() {
        run(null);
    }

    /**
     * Execute the specified runnable if the app has been granted all permissions. Calling this method will prompt the
     * user if needed.
     */
    public void run(final Runnable onPermissionsGranted) {
        if (!mDoNotPrompt && !(mContext instanceof Activity)) {
            throw new IllegalStateException("You need to either specify doNotPrompt() or pass in an Activity context");
        }

        mOnPermissionsGranted = onPermissionsGranted;

        if (hasPermissions(mContext)) {
            onPermissionsGranted();
        } else if (mDoNotPrompt) {
            onPermissionsDenied();
        } else {
            Permissions.prompt((Activity) mContext, this);
        }

        // This reference is no longer needed. Let's clear it now to avoid memory leaks.
        mContext = null;
    }

    /**
     * Always execute the specified runnable regardless of success or failure, e.g. for treating
     * the notification permission as an optional permission.
     */
    public void alwaysRun(final @NonNull Runnable onPermissionsHandled) {
        if (mOnPermissionsDenied != null) {
            throw new IllegalStateException("Do not specify a separate fallback runnable when using alwaysRun()");
        }

        mOnPermissionsDenied = onPermissionsHandled;
        run(onPermissionsHandled);
    }

    /**
     * Execute this fallback if at least one permission has not been granted.
     */
    public PermissionBlock andFallback(final @NonNull Runnable onPermissionsDenied) {
        mOnPermissionsDenied = onPermissionsDenied;
        return this;
    }

    /* package-private */ void onPermissionsGranted() {
        executeRunnable(mOnPermissionsGranted);
    }

    /* package-private */ void onPermissionsDenied() {
        executeRunnable(mOnPermissionsDenied);
    }

    private void executeRunnable(final Runnable runnable) {
        if (runnable == null) {
            return;
        }

        if (mOnUiThread && mOnBackgroundThread) {
            throw new IllegalStateException("Cannot run callback on more than one thread");
        }

        if (mOnUiThread && !ThreadUtils.isOnUiThread()) {
            ThreadUtils.postToUiThread(runnable);
        } else if (mOnBackgroundThread && !ThreadUtils.isOnBackgroundThread()) {
            ThreadUtils.postToBackgroundThread(runnable);
        } else {
            runnable.run();
        }
    }

    /* package-private */ String[] getPermissions() {
        return mPermissions;
    }

    /* package-private */ boolean hasPermissions(final Context context) {
        return mHelper.hasPermissions(context, mPermissions);
    }
}