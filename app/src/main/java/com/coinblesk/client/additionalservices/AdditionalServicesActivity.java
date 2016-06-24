package com.coinblesk.client.additionalservices;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;

import com.coinblesk.client.R;
import com.coinblesk.client.TransactionWrapperRecyclerViewAdapter;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.WalletService;

public class AdditionalServicesActivity extends AppCompatActivity {
    private final static String TAG = AdditionalServicesActivity.class.getName();

    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.additional_services);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        initToolbar();

        Intent intent = new Intent(this, WalletService.class);
        this.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.unbindService(serviceConnection);
    }

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            AdditionalServiceAdapter adapter = new AdditionalServiceAdapter(AdditionalServicesActivity.this, walletServiceBinder);
            ListView listView = (ListView) findViewById(R.id.additional_services_list);
            listView.setAdapter(adapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
}
