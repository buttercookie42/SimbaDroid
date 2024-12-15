/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package de.buttercookie.simbadroid;

import static de.buttercookie.simbadroid.util.StyledTextUtils.getStyledText;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import org.apache.commons.lang3.StringUtils;

import de.buttercookie.simbadroid.databinding.ActivityMainBinding;
import de.buttercookie.simbadroid.permissions.Permissions;
import de.buttercookie.simbadroid.service.SmbService;
import de.buttercookie.simbadroid.service.SmbServiceConnection;
import de.buttercookie.simbadroid.service.SmbServiceStatusLiveData;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

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

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toggleService.setOnClickListener(v -> toggleSmbService());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.toggleService.setAllowClickWhenDisabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SmbServiceStatusLiveData.get().observe(this, status -> {
            updateButtonState(status);
            if (!status.serviceRunning()) {
                binding.serviceStatus.setText(R.string.status_server_off);
            } else if (!status.serverRunning()) {
                binding.serviceStatus.setText(R.string.message_server_waiting_wifi);
            } else if (StringUtils.isBlank(status.mdnsAddress()) ||
                    StringUtils.isBlank(status.ipAddress())) {
                binding.serviceStatus.setText(R.string.message_server_running);
            } else {
                binding.serviceStatus.setText(getStyledText(this,
                        R.string.status_server_running,
                        status.mdnsAddress(),
                        status.netBiosAddress(),
                        status.ipAddress()));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent serviceIntent = new Intent(this, SmbService.class);
        bindService(serviceIntent, mSmbSrvConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(mSmbSrvConn);
        mBound = false;
    }

    private void updateButtonState(SmbService.Status status) {
        final MaterialButton button = binding.toggleService;

        if (status.serviceRunning()) {
            button.setText(R.string.button_stop_server);
            button.setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_stop));
            button.setEnabled(true);
        } else {
            button.setText(R.string.button_start_server);
            button.setIcon(AppCompatResources.getDrawable(this, R.drawable.ic_start));
            button.setEnabled(mBound && mService.isWifiAvailable());
        }
    }

    private void toggleSmbService() {
        if (!mBound) {
            Toast.makeText(this,
                    R.string.toast_error_starting_server,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mService.isRunning()) {
            startSmbService();
        } else {
            stopSmbService();
        }
    }

    private void startSmbService() {
        if (!mService.isWifiAvailable()) {
            Toast.makeText(this,
                    R.string.toast_error_no_wifi,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        SmbService.startService(this, true);
    }

    private void stopSmbService() {
        mService.stop();
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