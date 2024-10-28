/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import de.buttercookie.simbadroid.permissions.Permissions;
import de.buttercookie.simbadroid.service.SmbService;
import de.buttercookie.simbadroid.service.SmbServiceConnection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SmbService mService;
    private boolean mBound = false;
    private final SmbServiceConnection mSmbSrvConn = new SmbServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            super.onServiceConnected(name, service);
            mService = getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            super.onServiceDisconnected(name);
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.start_service).setOnClickListener(this);
        findViewById(R.id.stop_service).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, SmbService.class);
        bindService(intent, mSmbSrvConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(mSmbSrvConn);
        mBound = false;
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.start_service) {
            Intent intent = new Intent(this, SmbService.class);
            Permissions.from(this)
                    .withPermissions(Manifest.permission.POST_NOTIFICATIONS)
                    .alwaysRun(() -> {
                        String storagePermission =
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ?
                                        Manifest.permission.MANAGE_EXTERNAL_STORAGE :
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE;
                        Permissions.from(this)
                                .withPermissions(storagePermission)
                                .andFallback(() -> Toast.makeText(this,
                                        "Manage storage permission required",
                                        Toast.LENGTH_SHORT).show())
                                .run(() -> startService(intent));
                    });
        } else if (v.getId() == R.id.stop_service) {
            if (mBound) {
                mService.stop();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permissions.onRequestPermissionsResult(this, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case Permissions.ACTIVITY_MANAGE_STORAGE_RESULT_CODE: {
                Permissions.onManageStorageActivityResult(this);
            }
        }
    }
}