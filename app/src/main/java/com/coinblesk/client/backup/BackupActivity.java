/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client.backup;


import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.coinblesk.client.R;
import com.coinblesk.client.utils.AppUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Andreas Albrecht
 */
public class BackupActivity extends AppCompatActivity {

    private final static String TAG = BackupActivity.class.getName();
    private final static String[] BACKUP_RESTORE_PERMISSIONS = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final static int BACKUP_PERMISSIONS_REQUEST_CODE = 89; // arbitrary integer.
    public final static Pattern[] BACKUP_FILE_EXCLUDE_FILTER = new Pattern[] {
            Pattern.compile(".+\\.spvchain"), /* Spvchain  - new instance is created during restore */
            Pattern.compile("instant-run/.*") /* Files of the Android instant-run feature... */
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_backup);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        findViewById(R.id.backup_backup_touch).setOnClickListener(new BackupClickListener());
        findViewById(R.id.backup_restore_touch).setOnClickListener(new RestoreClickListener());
        findViewById(R.id.backup_refresh_touch).setOnClickListener(new RefreshClickListener());
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        checkAndRequestPermissions();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    private class BackupClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Backup: Backup.");
            if (AppUtils.hasPermissions(BackupActivity.this, BACKUP_RESTORE_PERMISSIONS)) {
                showBackupDialog();
            } else {
                showPermissionRationale();
            }
        }
    }

    private class RestoreClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Backup: Restore.");
            if (AppUtils.hasPermissions(BackupActivity.this, BACKUP_RESTORE_PERMISSIONS)) {
                showRestoreDialog();
            } else {
                showPermissionRationale();
            }
        }
    }

    private class RefreshClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Backup: Refresh.");
            showRefreshDialog();
        }
    }

    private void showBackupDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment backupDialog = BackupDialogFragment.newInstance();
        backupDialog.show(fm, "fragment_backup_dialog");
    }

    private void showRestoreDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment restoreDialog = BackupRestoreDialogFragment.newInstance();
        restoreDialog.show(fm, "fragment_backup_restore_dialog");
    }

    private void showRefreshDialog() {
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment refreshDialog = RefreshDialogFragment.newInstance();
        refreshDialog.show(fm, "fragment_refresh_dialog");
    }

    private void checkAndRequestPermissions() {
        if (!AppUtils.hasPermissions(this, BACKUP_RESTORE_PERMISSIONS)) {
                Log.d(TAG, "Request backup permissions: " + Arrays.toString(BACKUP_RESTORE_PERMISSIONS));
                ActivityCompat.requestPermissions(this, BACKUP_RESTORE_PERMISSIONS, BACKUP_PERMISSIONS_REQUEST_CODE);
        } else {
            Log.d(TAG, "Backup permissions already granted: " + Arrays.toString(BACKUP_RESTORE_PERMISSIONS));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case BACKUP_PERMISSIONS_REQUEST_CODE:
                if (AppUtils.allPermissionsGranted(grantResults)) {
                    Log.d(TAG, "Permissions granted: " + Arrays.toString(permissions));
                } else {
                    Log.d(TAG, "Permissions denied: " + Arrays.toString(permissions) + ", " + Arrays.toString(grantResults));
                    if (AppUtils.shouldShowPermissionRationale(this, permissions)) {
                        showPermissionRationale();
                    }
                };
                break;
            default:
                Log.w(TAG, "onRequestPermissionsResult: Received unknown requestCode. Missing case?");
        }
    }

    private void showPermissionRationale() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogAccent)
                .setTitle(R.string.backup_storage_permissions_title)
                .setMessage(R.string.backup_storage_permissions_rationale)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .create();
        dialog.show();
    }

}