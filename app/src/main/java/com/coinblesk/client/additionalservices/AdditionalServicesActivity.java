package com.coinblesk.client.additionalservices;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.coinblesk.client.R;
import com.coinblesk.json.UserAccountTO;
import com.coinblesk.payments.WalletService;

import java.util.ArrayList;
import java.util.List;

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

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "onServiceConnected");
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            AdditionalServiceGUIState state = new AdditionalServiceGUIState(AdditionalServicesActivity.this);
            new AdditionalServicesTasks.GetAccountTask(state).execute();
            AdditionalServicesAdapter adapter = new AdditionalServicesAdapter(AdditionalServicesActivity.this, walletServiceBinder, state);
            ListView listView = (ListView) findViewById(R.id.additional_services_list);
            listView.setAdapter(adapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        this.unbindService(serviceConnection);
    }

    static class AdditionalServiceGUIState {

        final private Activity activity;

        private List<CheckBox> checkBoxes = new ArrayList<>(2);
        private UserAccountTO userAccountTO;
        private List<TextView> balances  = new ArrayList<>(2);
        private List<TextView> textViews  = new ArrayList<>(2);

        public AdditionalServiceGUIState(Activity activity) {
            this.activity = activity;
        }

        public AdditionalServiceGUIState addCheckBox(CheckBox checkBox) {
            checkBoxes.add(checkBox);
            updateState();
            return this;
        }

        public AdditionalServiceGUIState addBalance(TextView balance) {
            balances.add(balance);
            updateState();
            return this;
        }

        public AdditionalServiceGUIState addTextView(TextView balance) {
            textViews.add(balance);
            updateState();
            return this;
        }

        public UserAccountTO userAccountTO() {
            return userAccountTO;
        }

        public AdditionalServiceGUIState userAccountTO(UserAccountTO userAccountTO) {
            this.userAccountTO = userAccountTO;
            updateState();
            return this;
        }

        private void updateState() {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    boolean success = false;
                    for(CheckBox checkBox:checkBoxes) {
                        if (userAccountTO != null && checkBox != null) {
                            checkBox.setEnabled(true);
                            checkBox.setChecked(userAccountTO.isSuccess());
                            success = true;
                        } else if (checkBox != null) {
                            checkBox.setEnabled(false);
                            checkBox.setChecked(false);
                        }
                    }
                    if(success) {
                        for(TextView textView:textViews) {
                            textView.setText(R.string.additional_services_titel_logout);
                        }
                    }
                    for(TextView balance:balances) {
                        if (userAccountTO != null && balance != null) {
                            balance.setText(Long.toString(userAccountTO.balance()));
                        } else if (balance != null) {
                            balance.setText("no balance");
                        }
                    }
                }
            });
        }
    }
}
