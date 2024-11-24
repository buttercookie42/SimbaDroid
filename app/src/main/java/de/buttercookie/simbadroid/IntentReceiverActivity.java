/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid;

import static de.buttercookie.simbadroid.Intents.ACTION_START;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import de.buttercookie.simbadroid.permissions.Permissions;
import de.buttercookie.simbadroid.service.SmbService;
import de.buttercookie.simbadroid.util.ThreadUtils;

public class IntentReceiverActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            startService();
        }

        finish();
    }

    @SuppressLint("InlinedApi")
    private void startService() {
        Intent intent = new Intent(this, SmbService.class);
        Permissions.from(this)
                .withPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
                .doNotPrompt()
                .alwaysRun(() -> ThreadUtils.postToUiThread(() -> {
                    String storagePermission =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                                    android.Manifest.permission.MANAGE_EXTERNAL_STORAGE :
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE;
                    Permissions.from(this)
                            .withPermissions(storagePermission)
                            .doNotPrompt()
                            .andFallback(() -> Toast.makeText(this,
                                    R.string.toast_need_storage_permission,
                                    Toast.LENGTH_SHORT).show())
                            .run(() -> startService(intent));
                }));
    }
}
