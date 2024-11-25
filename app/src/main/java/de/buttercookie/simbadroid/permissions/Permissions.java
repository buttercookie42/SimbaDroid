/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// Imported from https://hg.mozilla.org/releases/mozilla-esr68/file/bb3f440689f395ed15d6a6177650c0b0ad37146a/mobile/android/geckoview/src/main/java/org/mozilla/gecko/permissions/Permissions.java

package de.buttercookie.simbadroid.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;

import de.buttercookie.simbadroid.util.ThreadUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Convenience class for checking and prompting for runtime permissions.
 * <p>
 * Example:<br>
 * <code>
 *    Permissions.from(activity)
 *               .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
 *               .onUiThread()
 *               .andFallback(onPermissionDenied())
 *               .run(onPermissionGranted())
 * </code>
 * <p>
 * This example will run the runnable returned by onPermissionGranted() if the WRITE_EXTERNAL_STORAGE permission is
 * already granted. Otherwise it will prompt the user and run the runnable returned by onPermissionGranted() or
 * onPermissionDenied() depending on whether the user accepted or not.
 * <p>
 * If onUiThread()/onBackgroundThread() is specified, all callbacks will be run on the respective
 * thread. Otherwise, the callback may run on the current thread, but will switch to the UI thread
 * if a permissions prompt is required.
 */
public class Permissions {
    public static final int ACTIVITY_MANAGE_STORAGE_RESULT_CODE = 27294;

    private static final Queue<PermissionBlock> waiting = new LinkedList<>();
    private static final Queue<PermissionBlock> prompt = new LinkedList<>();

    private static PermissionsHelper permissionHelper = new PermissionsHelper();

    /**
     * Entry point for checking (and optionally prompting for) runtime permissions.
     * <p>
     * Note: The provided context needs to be an Activity context in order to prompt. Use doNotPrompt()
     * for all other contexts.
     */
    public static PermissionBlock from(final @NonNull Context context) {
        return new PermissionBlock(context, permissionHelper);
    }

    /**
     * This method will block until the specified permissions have been granted or denied by the user.
     * If needed the user will be prompted.
     *
     * @return true if all of the permissions have been granted. False if any of the permissions have been denied.
     */
    public static boolean waitFor(final @NonNull Activity activity, final String... permissions) {
        ThreadUtils.assertNotOnUiThread(); // We do not want to block the UI thread.

        // This task will block until all of the permissions have been granted
        final FutureTask<Boolean> blockingTask = new FutureTask<>(() -> true);

        // This runnable will cancel the task if any of the permissions have been denied
        Runnable cancelBlockingTask = () -> blockingTask.cancel(true);

        Permissions.from(activity)
                .withPermissions(permissions)
                .andFallback(cancelBlockingTask)
                .run(blockingTask);

        try {
            return blockingTask.get();
        } catch (InterruptedException | ExecutionException | CancellationException e) {
            return false;
        }
    }

    /**
     * Determine whether you have been granted particular permissions.
     */
    public static boolean has(final Context context, final String... permissions) {
        return permissionHelper.hasPermissions(context, permissions);
    }

    /* package-private */ static void setPermissionHelper(final PermissionsHelper permissionHelper) {
        Permissions.permissionHelper = permissionHelper;
    }

    /**
     * Callback for Activity.onRequestPermissionsResult(). All activities that prompt for permissions using this class
     * should implement onRequestPermissionsResult() and call this method.
     */
    public static synchronized void onRequestPermissionsResult(final @NonNull Activity activity,
                                                               final @NonNull String[] permissions,
                                                               final @NonNull int[] grantResults) {
        processGrantResults(permissions, grantResults);

        processQueue(activity, permissions, grantResults);
    }

    /**
     * Callback for Activity.onActivityResult(). All activities that prompt for the MANAGE_EXTERNAL_STORAGE
     * permission using this class should implement onActivityResult() and call this class if the
     * request code is Permissions.ACTIVITY_MANAGE_STORAGE_RESULT_CODE.
     */
    public static void onManageStorageActivityResult(final @NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String[] permission = {Manifest.permission.MANAGE_EXTERNAL_STORAGE};
            int[] granted = {Environment.isExternalStorageManager() ?
                    PackageManager.PERMISSION_GRANTED :
                    PackageManager.PERMISSION_DENIED};
            Permissions.onRequestPermissionsResult(
                    activity, permission, granted);
        }
    }

    /* package-private */ static synchronized void prompt(final Activity activity,
                                                          final PermissionBlock block) {
        if (prompt.isEmpty()) {
            prompt.add(block);
            showPrompt(activity);
        } else {
            waiting.add(block);
        }
    }

    private static synchronized void processGrantResults(final @NonNull String[] permissions,
                                                         final @NonNull int[] grantResults) {
        final HashSet<String> grantedPermissions = collectGrantedPermissions(permissions, grantResults);

        while (!prompt.isEmpty()) {
            final PermissionBlock block = prompt.poll();

            if (allPermissionsGranted(block, grantedPermissions)) {
                block.onPermissionsGranted();
            } else {
                block.onPermissionsDenied();
            }
        }
    }

    private static synchronized void processQueue(final Activity activity,
                                                  final String[] permissions,
                                                  final int[] grantResults) {
        final HashSet<String> deniedPermissions = collectDeniedPermissions(permissions, grantResults);

        while (!waiting.isEmpty()) {
            final PermissionBlock block = waiting.poll();

            if (block.hasPermissions(activity)) {
                block.onPermissionsGranted();
            } else {
                if (atLeastOnePermissionDenied(block, deniedPermissions)) {
                    // We just prompted the user and one of the permissions of this block has been denied:
                    // There's no reason to instantly prompt again; Just reject without prompting.
                    block.onPermissionsDenied();
                } else {
                    prompt.add(block);
                }
            }
        }

        if (!prompt.isEmpty()) {
            showPrompt(activity);
        }
    }

    private static synchronized void showPrompt(final Activity activity) {
        HashSet<String> permissions = new HashSet<>();

        for (PermissionBlock block : prompt) {
            Collections.addAll(permissions, block.getPermissions());
        }

        permissionHelper.prompt(activity, permissions.toArray(new String[0]));
    }

    private static HashSet<String> collectGrantedPermissions(final @NonNull String[] permissions,
                                                             final @NonNull int[] grantResults) {
        return filterPermissionsByResult(permissions, grantResults, PackageManager.PERMISSION_GRANTED);
    }

    private static HashSet<String> collectDeniedPermissions(final @NonNull String[] permissions,
                                                            final @NonNull int[] grantResults) {
        return filterPermissionsByResult(permissions, grantResults, PackageManager.PERMISSION_DENIED);
    }

    private static HashSet<String> filterPermissionsByResult(final @NonNull String[] permissions,
                                                             final @NonNull int[] grantResults,
                                                             final int result) {
        HashSet<String> grantedPermissions = new HashSet<>(permissions.length);
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == result) {
                grantedPermissions.add(permissions[i]);
            }
        }
        return grantedPermissions;
    }

    private static boolean allPermissionsGranted(final PermissionBlock block,
                                                 final HashSet<String> grantedPermissions) {
        for (String permission : block.getPermissions()) {
            if (!grantedPermissions.contains(permission)) {
                return false;
            }
        }

        return true;
    }

    private static boolean atLeastOnePermissionDenied(final PermissionBlock block,
                                                      final HashSet<String> deniedPermissions) {
        for (String permission : block.getPermissions()) {
            if (deniedPermissions.contains(permission)) {
                return true;
            }
        }

        return false;
    }
}