/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.client;


import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.coinblesk.client.about.AboutActivity;
import com.coinblesk.client.additionalservices.AdditionalServiceUtils;
import com.coinblesk.client.additionalservices.AdditionalServicesActivity;
import com.coinblesk.client.additionalservices.AdditionalServicesUsernameDialog;
import com.coinblesk.client.addresses.AddressActivity;
import com.coinblesk.client.backup.BackupActivity;
import com.coinblesk.client.config.AppConfig;
import com.coinblesk.client.config.Constants;
import com.coinblesk.client.settings.SettingsActivity;
import com.coinblesk.client.ui.authview.AuthenticationDialog;
import com.coinblesk.client.ui.dialogs.ProgressSuccessOrFailDialog;
import com.coinblesk.client.ui.dialogs.QrDialogFragment;
import com.coinblesk.client.ui.dialogs.SendDialogFragment;
import com.coinblesk.client.utils.AppUtils;
import com.coinblesk.client.utils.PaymentFutureCallback;
import com.coinblesk.client.utils.SharedPrefUtils;
import com.coinblesk.client.utils.UIUtils;
import com.coinblesk.client.utils.upgrade.Multisig2of2ToCltvForwardTask;
import com.coinblesk.client.utils.upgrade.UpgradeUtils;
import com.coinblesk.client.wallet.WalletActivity;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.peers.AbstractClient;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.peers.PaymentRequestDelegate;
import com.coinblesk.payments.communications.peers.bluetooth.cltv.BluetoothLEClient;
import com.coinblesk.payments.communications.peers.bluetooth.cltv.BluetoothLEServer;
import com.coinblesk.payments.communications.peers.nfc.NFCClient;
import com.coinblesk.payments.communications.peers.nfc.NFCServerACSCLTV;
import com.coinblesk.payments.communications.peers.nfc.NFCServerCLTV;
import com.coinblesk.payments.communications.peers.wifi.WiFiClient;
import com.coinblesk.payments.communications.peers.wifi.WiFiServer;
import com.coinblesk.util.SerializeUtils;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * @author Christian Killer
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 * @author Thomas Bocek
 */

public class MainActivity extends AppCompatActivity
                            implements AuthenticationDialog.AuthenticationDialogListener,
                                        SendDialogFragment.SendDialogListener {

    private final static String TAG = MainActivity.class.getName();
    private final static int FINE_LOCATION_PERMISSION_REQUEST = 1;
    
    private DrawerLayout drawerLayout;

    private final List<AbstractClient> clients = new ArrayList<>();
    private final List<AbstractServer> servers = new ArrayList<>();
    // if true, servers are started onStart (e.g. when user switches back from settings to coinblesk).
    private boolean restartServers;

    @Override
    protected void onResume() {
        super.onResume();
        if (NfcAdapter.getDefaultAdapter(this) != null) {
            NfcAdapter.getDefaultAdapter(this).setNdefPushMessage(null, this);
        }
        registerPaymentRequestReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterPaymentRequestReceiver();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPrefUtils.initDefaults(this, R.xml.settings_pref, false);
        final AppConfig appConfig = ((CoinbleskApp) getApplication()).getAppConfig();

        try {
            AdditionalServiceUtils.setSessionID(this, null);
        } catch (Exception e) {
            Log.e(TAG, "Could not set sessionID: ", e);
        }

        UpgradeUtils upgradeUtils = new UpgradeUtils();
        upgradeUtils.checkUpgrade(this, appConfig.getNetworkParameters());

        startWalletService(false);

        setContentView(R.layout.activity_main);
        initToolbar();
        initNavigationView();
        initViewPager();


        final Intent intent = getIntent();
        final String scheme = intent.getScheme();
        if (scheme != null && scheme.equals(appConfig.getNetworkParameters().getUriScheme())) {
            final String uri = intent.getDataString();
            try {
                BitcoinURI bitcoinURI = new BitcoinURI(uri);
                SendDialogFragment.newInstance(
                        bitcoinURI.getAddress(),
                        bitcoinURI.getAmount())
                        .show(getSupportFragmentManager(), "send-dialog");
            } catch (BitcoinURIParseException e) {
                Log.w(TAG, "Could not parse Bitcoin URI: " + uri);
            }
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                FINE_LOCATION_PERMISSION_REQUEST);

        checkVersionCompatibility(appConfig);
    }



    private void checkVersionCompatibility(AppConfig appConfig) {
        // message is only displayed if request succeeds and answer from server is negative in order
        // to av
        // oid annoying message dialogs. (the client or the server may just be temporary offline).
        new VersionCheckTask(appConfig, AppUtils.getAppVersion(), this).execute();
    }

    private void startWalletService(boolean bindService) {
        Intent walletServiceIntent = new Intent(this, WalletService.class);
        startService(walletServiceIntent);
        if (bindService) {
            bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void initViewPager() {
        // Get the ViewPager and set its PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        if (viewPager != null) {
            viewPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));
        }

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        if (tabLayout != null) {
            tabLayout.setTabMode(TabLayout.MODE_FIXED);
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            tabLayout.setupWithViewPager(viewPager);
        }
    }

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        if (getActionBar() != null) {
            getActionBar().setTitle(title);
        }
    }


    private void initNavigationView() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        if (navigationView == null) {
            Log.w(TAG, "Did not find navigation view!");
            return;
        }

        //Setting Navigation View Item Selected Listener to handle the item click of the navigation menu
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {

                if (menuItem.isChecked()) menuItem.setChecked(false);
                else menuItem.setChecked(true);

                //Closing drawer on item click
                drawerLayout.closeDrawers();

                switch (menuItem.getItemId()) {
                    case R.id.addresses:
                        Intent addressesAct = new Intent(getApplicationContext(), AddressActivity.class);
                        startActivity(addressesAct);
                        return true;
                    case R.id.additional_services:
                        Intent additionalServicesAct = new Intent(getApplicationContext(), AdditionalServicesActivity.class);
                        startActivity(additionalServicesAct);
                        return true;
                    case R.id.wallet:
                        Intent walletAct = new Intent(getApplicationContext(), WalletActivity.class);
                        startActivity(walletAct);
                        return true;
                    case R.id.backup:
                        Intent backupAct = new Intent(getApplicationContext(), BackupActivity.class);
                        startActivity(backupAct);
                        return true;
                    case R.id.settings:
                        Intent newAct = new Intent(getApplicationContext(), SettingsActivity.class);
                        startActivity(newAct);
                        return (true);
                    case R.id.about_coinblesk:
                        Intent aboutAct = new Intent(getApplicationContext(), AboutActivity.class);
                        startActivity(aboutAct);
                        return true;
                    default:
                        Toast.makeText(getApplicationContext(), "Somethings Wrong", Toast.LENGTH_SHORT).show();
                        return true;

                }
            }
        });

        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, null, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer opens, we don't need anything to happen yet, hence leave it blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer opens, we don't need anything to happen yet, hence leave it blank
                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to the drawerLayout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        //calling sync state is necessary because the menu icon wouldnt show up otheriwse
        actionBarDrawerToggle.syncState();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         /*
         Handle action bar item clicks here. The action bar will
         automatically handle clicks on the Home/Up button, so long
         as you specify a parent activity in AndroidManifest.xml.
         */
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_qr_code:
                showQrDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        initSwitch(menu);
        return true;
    }
    private void initSwitch(Menu menu) {
        MenuItem item = menu.findItem(R.id.myswitch);
        View view = item.getActionView();
        final Switch mySwitch = (Switch) view.findViewById(R.id.switchAB);
        final AtomicReference<CountDownTimer> ref = new AtomicReference<>();
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    //enable BT
                    ref.set(new CountDownTimer(30000, 1000) {
                        int i=0;
                        public void onTick(final long millisUntilFinished) {
                            mySwitch.setButtonDrawable((i++ % 2) == 0 ? R.drawable.bluetooth_onon : R.drawable.bluetooth_on);
                            mySwitch.setTextOn(""+millisUntilFinished / 1000);
                        }

                        public void onFinish() {
                            mySwitch.setButtonDrawable(R.drawable.bluetooth_on);
                            mySwitch.setChecked(false);
                        }

                    });
                    ref.get().start();
                    LocalBroadcastManager
                            .getInstance(MainActivity.this)
                            .sendBroadcast(new Intent(Constants.START_CLIENTS_ACTION));

                } else {
                    //mySwitch.setShowText(false);
                    CountDownTimer tmp;
                    if((tmp = ref.getAndSet(null)) != null) {
                        tmp.cancel();
                    }
                    LocalBroadcastManager
                            .getInstance(MainActivity.this)
                            .sendBroadcast(new Intent(Constants.STOP_CLIENTS_ACTION));
                }
            }
        });
    }

    public void showQrDialog() {
        try {
            Address receiveAddress = walletServiceBinder.getCurrentReceiveAddress();
            String bitcoinUriStr = BitcoinURI.convertToBitcoinURI(receiveAddress, null, null, null);
            QrDialogFragment
                    .newInstance(new BitcoinURI(bitcoinUriStr))
                    .show(getSupportFragmentManager(), "qr_dialog_fragment");
            Log.d(TAG, "showQrDialog - bitcoinUri" + bitcoinUriStr);
        } catch (Exception e) {
            Log.w(TAG, "Error showing QR Code: ", e);
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogAccent);
            Dialog dialog = builder
                    .setTitle(R.string.qr_code_error_title)
                    .setMessage(getString(R.string.qr_code_error_message, e.getMessage()))
                    .setNeutralButton(R.string.ok, null)
                    .create();
            dialog.show();
        }
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private WalletService.WalletServiceBinder walletServiceBinder;

    private final BroadcastReceiver instantPaymentSuccessListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (approvePaymentDialog != null && approvePaymentDialog.isAdded()) {
                approvePaymentDialog.dismiss();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(startClientsBroadcastReceiver, new IntentFilter(Constants.START_CLIENTS_ACTION));
        broadcastManager.registerReceiver(stopClientsBroadcastReceiver, new IntentFilter(Constants.STOP_CLIENTS_ACTION));
        broadcastManager.registerReceiver(startServersBroadcastReceiver, new IntentFilter(Constants.START_SERVERS_ACTION));
        broadcastManager.registerReceiver(walletServiceError, new IntentFilter(Constants.WALLET_ERROR_ACTION));

        broadcastManager.registerReceiver(instantPaymentSuccessListener, new IntentFilter(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION));
        broadcastManager.registerReceiver(instantPaymentSuccessListener, new IntentFilter(Constants.INSTANT_PAYMENT_FAILED_ACTION));
        broadcastManager.registerReceiver(instantPaymentSuccessListener, new IntentFilter(Constants.WALLET_INSUFFICIENT_BALANCE_ACTION));

        startWalletService(true);

        // restart servers if they were running before.
        if (restartServers) {
            Log.i(TAG, "Restart servers (with previous payment request)");
            for (AbstractServer server : servers) {
                server.start();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.unregisterReceiver(startClientsBroadcastReceiver);
        broadcastManager.unregisterReceiver(stopClientsBroadcastReceiver);
        broadcastManager.unregisterReceiver(startServersBroadcastReceiver);

        broadcastManager.unregisterReceiver(instantPaymentSuccessListener);


        this.stopServers();

        unbindService(serviceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            initPeers();

            if (SharedPrefUtils.isMultisig2of2ToCltvForwardingEnabled(MainActivity.this)) {
                new Multisig2of2ToCltvForwardTask(MainActivity.this,
                        walletServiceBinder,
                        walletServiceBinder.getMultisigClientKey(),
                        walletServiceBinder.getMultisigServerKey())
                        .execute();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };

    //http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                                                @NonNull int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.length > 0) &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(TAG, "discovery supported (BLE and BL will be supported)");
                } else {
                    Log.d(TAG, "discovery unsupported (BLE and BL will not be supported)");
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void sendCoins(Address address, Coin amount) {
        final ProgressSuccessOrFailDialog progress = (ProgressSuccessOrFailDialog)
                ProgressSuccessOrFailDialog.newInstance(getString(R.string.fragment_send_dialog_title));
        progress.show(getSupportFragmentManager(), "progress_success_or_fail_dialog");
        ListenableFuture<Transaction> txFuture = walletServiceBinder.sendCoins(address, amount);

        Futures.addCallback(txFuture, new PaymentFutureCallback(progress));
    }



    /**
     * Communication part starts here
     */

    private final BroadcastReceiver walletServiceError = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(Constants.ERROR_MESSAGE_KEY);
            if (message == null || message.isEmpty()) {
                message = "Unknown wallet error";
            }
            Log.e(TAG, "Wallet Error: " + message);

            Dialog dialog = new AlertDialog.Builder(MainActivity.this, R.style.AlertDialogAccent)
                    .setTitle(R.string.wallet_error_title)
                    .setMessage(getString(R.string.wallet_error_message, message))
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
    };

    private final BroadcastReceiver startClientsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startClients();
        }
    };

    private final BroadcastReceiver startServersBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String uri = intent.getStringExtra(Constants.BITCOIN_URI_KEY);
            try {
                final BitcoinURI bitcoinURI = new BitcoinURI(uri);
                startServers(bitcoinURI);
                showAuthViewAndGetResult(bitcoinURI, false, false);
            } catch (BitcoinURIParseException e) {
                Log.w(TAG, "Could not parse Bitcoin URI: " + uri);
            }
        }
    };

    private final BroadcastReceiver stopClientsBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopClients();
        }
    };

    private ApprovePaymentDialog approvePaymentDialog;

    final private BroadcastReceiver approveView = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (approvePaymentDialog != null && approvePaymentDialog.isAdded()) {
                Log.d(TAG, "dismiss old approve dialog");
                approvePaymentDialog.dismiss();
            }
            showApproveDialogIfRequired(intent);

        }
    };

    private void showApproveDialogIfRequired(Intent intent) {
        if(intent.getStringExtra(Constants.PAYMENT_REQUEST_ADDRESS) != null && intent.getStringExtra(Constants.PAYMENT_REQUEST_AMOUNT) != null) {
            approvePaymentDialog = new ApprovePaymentDialog();
            Bundle args = new Bundle();
            args.putString(Constants.PAYMENT_REQUEST_ADDRESS, intent.getStringExtra(Constants.PAYMENT_REQUEST_ADDRESS));
            args.putString(Constants.PAYMENT_REQUEST_AMOUNT, intent.getStringExtra(Constants.PAYMENT_REQUEST_AMOUNT));
            approvePaymentDialog.setArguments(args);
            approvePaymentDialog.show(getFragmentManager(), TAG);
            Log.d(TAG, "show new approve dialog");
        }
    }

    private void registerPaymentRequestReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(approveView,
                new IntentFilter(Constants.PAYMENT_REQUEST));
    }

    private void unregisterPaymentRequestReceiver() {
        Log.d(TAG, "unregister payment request receiver");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(approveView);
    }

    private void initPeers() {
        // TODO: init peers should probably be called in onStart? (e.g. if connection settings change -> need to reload)
        // TODO: do we need to stop the servers/clients first before we lose the references?
        this.servers.clear();
        this.clients.clear();

        if (SharedPrefUtils.isConnectionNfcEnabled(this)) {
            clients.add(new NFCClient(this, walletServiceBinder));
            servers.add(new NFCServerACSCLTV(this, walletServiceBinder));
            servers.add(new NFCServerCLTV(this, walletServiceBinder));

        }

        if (SharedPrefUtils.isConnectionBluetoothLeEnabled(this)) {
            clients.add(new BluetoothLEClient(this, walletServiceBinder));
            servers.add(new BluetoothLEServer(this, walletServiceBinder));
        }

        if (SharedPrefUtils.isConnectionWiFiDirectEnabled(this)) {
            clients.add(new WiFiClient(this, walletServiceBinder));
            servers.add(new WiFiServer(this, walletServiceBinder));
        }

        for (AbstractServer server : servers) {
            server.setPaymentRequestDelegate(getClientPaymentRequestDelegate());
        }

        for (AbstractClient client : clients) {
            client.setPaymentRequestDelegate(getClientPaymentRequestDelegate());
            if(client instanceof  NFCClient) {
                client.start();
            }
        }

    }

    private void startClients() {
        Log.d(TAG, "Start clients.");
        for (AbstractClient client : clients) {
            client.start();
        }
    }

    private void startServers(BitcoinURI bitcoinURI) {
        Log.d(TAG, "Start servers.");
        for (AbstractServer server : servers) {
            server.setPaymentRequestUri(bitcoinURI);
            server.start();
        }
    }

    private void stopClients() {
        Log.d(TAG, "Stop clients.");
        for (AbstractClient client : clients) {
            client.stop();
        }
    }

    private void stopServers() {
        Log.d(TAG, "Stop servers.");
        for (AbstractServer server : servers) {
            /*if(server instanceof  NFCServerACSCLTV) {
                ((NFCServerACSCLTV) server).unregister();
            }*/
            server.stop();
        }
    }

    private PaymentRequestDelegate getClientPaymentRequestDelegate() {
        return new PaymentRequestDelegate() {


            @Override
            public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest) {
                boolean result = showAuthViewAndGetResult(paymentRequest, true, true);
                if (!result) {
                    this.onPaymentError("payment was not authorized!");
                }
                return result;
            }

            @Override
            public void onPaymentSuccess() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "payment success");
                        final Intent instantPaymentSucess = new Intent(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(instantPaymentSucess);
                        //ACS requires this to run in a different thread!
                        stopClients();
                        stopServers();
                        if (authViewDialog != null && authViewDialog.isAdded()) {
                            authViewDialog.dismiss();
                        }

                        if (approvePaymentDialog != null && approvePaymentDialog.isAdded()) {
                            approvePaymentDialog.dismiss();
                        }
                    }
                });

            }

            @Override
            public void onPaymentError(final String errorMessage) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "payment error");
                        final Intent instantPaymentFailed = new Intent(Constants.INSTANT_PAYMENT_FAILED_ACTION);
                        instantPaymentFailed.putExtra(Constants.ERROR_MESSAGE_KEY, errorMessage);
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(instantPaymentFailed);
                        stopClients();
                        stopServers();
                        if (authViewDialog != null && authViewDialog.isAdded()) {
                            authViewDialog.dismiss();
                        }
                        if (approvePaymentDialog != null && approvePaymentDialog.isAdded()) {
                            approvePaymentDialog.dismiss();
                        }
                    }
                });
            }
        };
    }

    private AuthenticationDialog authViewDialog;
    private boolean authviewResponse = false;
    private CountDownLatch countDownLatch;

    private boolean showAuthViewAndGetResult(BitcoinURI paymentRequest, boolean isBlocking, final boolean showAccept) {
        countDownLatch = new CountDownLatch(1); //because we need a synchronous answer
        restartServers = true;
        authViewDialog = AuthenticationDialog.newInstance(paymentRequest, showAccept);
        authViewDialog.show(getSupportFragmentManager(), "auth_view_dialog");
        if (isBlocking) {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting: ", e);
            }
        }
        return authviewResponse;
    }

    @Override
    public void authViewNegativeResponse() {
        Log.d(TAG, "Auth view - payment not accepted.");
        authviewResponse = false;
        countDownLatch.countDown();
    }

    @Override
    public void authViewPositiveResponse() {
        Log.d(TAG, "Auth view - payment accepted.");
        authviewResponse = true;
        countDownLatch.countDown();
    }

    @Override
    public void authViewDestroy() {
        Log.d(TAG, "Auth view - destroyed.");
        stopServers();
        restartServers = false;
        authViewDialog = null;
    }

}