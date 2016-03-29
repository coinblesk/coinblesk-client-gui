package com.coinblesk.client;


import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import com.coinblesk.client.ui.dialogs.BackupDialogFragment;
import com.coinblesk.client.ui.dialogs.BackupRestoreDialogFragment;
import com.coinblesk.payments.WalletService;

public class BackupActivity extends AppCompatActivity {

    private final static String TAG = BackupActivity.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_backup);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        findViewById(R.id.backup_backup_touch).setOnClickListener(new BackupClickListener());
        findViewById(R.id.backup_restore_touch).setOnClickListener(new RestoreClickListener());
        findViewById(R.id.backup_refresh_touch).setOnClickListener(new RefreshClickListener());
    }


    private class BackupClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Backup: Backup.");
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment backupDialog = BackupDialogFragment.newInstance();
            backupDialog.show(fm, "fragment_backup_dialog");
        }
    }

    private class RestoreClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Backup: Restore.");
            FragmentManager fm = getSupportFragmentManager();
            DialogFragment restoreDialog = BackupRestoreDialogFragment.newInstance();
            restoreDialog.show(fm, "fragment_backup_restore_dialog");
        }
    }

    private class RefreshClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Backup: Refresh.");
            AlertDialog.Builder builder = new AlertDialog.Builder(BackupActivity.this)
                    .setTitle(R.string.backup_refresh_dialog_title)
                    .setMessage(R.string.backup_refresh_dialog_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            walletServiceBinder.prepareWalletReset();
                            dialog.dismiss();
                            // close app such that wallet service is "restarted".
                            finishAffinity();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private WalletService.WalletServiceBinder walletServiceBinder;
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        Intent intent = new Intent(this, WalletService.class);
        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        this.unbindService(serviceConnection);
    }

}