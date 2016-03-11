package com.uzh.ckiller.coinblesk_client_gui;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import ch.papers.payments.WalletService;
import com.uzh.ckiller.coinblesk_client_gui.ui.dialogs.BackupDialogFragment;


public class BackupActivity extends AppCompatActivity {

    private final static String TAG = BackupActivity.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_backup);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        findViewById(R.id.backup_backup_touch).setOnClickListener(new BackupClickListener());
        findViewById(R.id.backup_restore_touch).setOnClickListener(new BackupRestoreClickListener());
        findViewById(R.id.backup_refresh_touch).setOnClickListener(new BackupRefreshClickListener());
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

    private class BackupRestoreClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Backup: Restore.");
        }
    }

    private class BackupRefreshClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Backup: Refresh.");
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
        Intent intent = new Intent(this, WalletService.class);
        this.startService(intent);
        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.unbindService(serviceConnection);
    }

}