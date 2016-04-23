package com.coinblesk.client;


import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
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
import android.widget.Toast;

import com.coinblesk.client.addresses.AddressActivity;
import com.coinblesk.client.authview.AuthenticationDialog;
import com.coinblesk.client.ui.dialogs.QrDialogFragment;
import com.coinblesk.client.ui.dialogs.SendDialogFragment;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.peers.AbstractClient;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.peers.PaymentRequestDelegate;
import com.coinblesk.payments.communications.peers.bluetooth.BluetoothLEClient;
import com.coinblesk.payments.communications.peers.bluetooth.BluetoothLEServer;
import com.coinblesk.payments.communications.peers.nfc.NFCClient;
import com.coinblesk.payments.communications.peers.nfc.NFCServer;
import com.coinblesk.payments.communications.peers.nfc.NFCServerACS;
import com.coinblesk.payments.communications.peers.wifi.WiFiClient;
import com.coinblesk.payments.communications.peers.wifi.WiFiServer;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import ch.papers.objectstorage.UuidObjectStorage;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by ckiller
 */

public class MainActivity extends AppCompatActivity implements AuthenticationDialog.AuthenticationDialogListener {
    private final static String TAG = MainActivity.class.getName();
    private final static int FINE_LOCATION_PERMISSION_REQUEST = 1;

    private final String NETWORK_SETTINGS_PREF_KEY = "pref_network_list";

    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    private final List<AbstractClient> clients = new ArrayList<AbstractClient>();
    private final List<AbstractServer> servers = new ArrayList<AbstractServer>();
    // if true, servers are started onStart (e.g. when user switches back from settings to coinblesk).
    private boolean restartServers;

    @Override
    protected void onResume() {
        super.onResume();
        if (NfcAdapter.getDefaultAdapter(this) != null) {
            NfcAdapter.getDefaultAdapter(this).setNdefPushMessage(null, this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String networkSettings = sharedPreferences.getString(NETWORK_SETTINGS_PREF_KEY, "main-net");

        switch (networkSettings) {
            case "test-net-3":
                Constants.WALLET_FILES_PREFIX = "testnet_wallet_";
                Constants.COINBLESK_SERVER_BASE_URL = "http://bitcoin2-test.csg.uzh.ch/coinblesk-server/";
                Constants.PARAMS = TestNet3Params.get(); // quick and dirty -> dont modify constants
                Constants.RETROFIT = new Retrofit.Builder()
                        .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
                        .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
                        .build();
                break;
            default:
                Constants.WALLET_FILES_PREFIX = "mainnet_wallet_";
                Constants.COINBLESK_SERVER_BASE_URL = "https://bitcoin.csg.uzh.ch/coinblesk-server/";
                Constants.PARAMS = MainNetParams.get(); // quick and dirty -> dont modify constants
                Constants.RETROFIT = new Retrofit.Builder()
                        .addConverterFactory(GsonConverterFactory.create(SerializeUtils.GSON))
                        .baseUrl(Constants.COINBLESK_SERVER_BASE_URL)
                        .build();
                break;
        }

        File objectStorageDir = new File(this.getFilesDir(), Constants.WALLET_FILES_PREFIX + "_uuid_object_storage");
        objectStorageDir.mkdirs();
        UuidObjectStorage.getInstance().init(objectStorageDir);


        setContentView(R.layout.activity_main);
        initToolbar();
        initNavigationView();
        initViewPager();
        PreferenceManager.setDefaultValues(this, R.xml.settings_pref, false);


        final Intent intent = getIntent();
        final String scheme = intent.getScheme();
        if (scheme != null && scheme.equals(Constants.PARAMS.getUriScheme())) {
            final String uri = intent.getDataString();
            try {
                BitcoinURI bitcoinURI = new BitcoinURI(uri);
                SendDialogFragment.newInstance(bitcoinURI.getAddress(), bitcoinURI.getAmount()).show(this.getSupportFragmentManager(), "send-dialog");
            } catch (BitcoinURIParseException e) {
                Log.w(TAG, "Could not parse Bitcoin URI: " + uri);
            }
        }

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                FINE_LOCATION_PERMISSION_REQUEST);
    }

    private void initViewPager() {
        // Get the ViewPager and set its PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));


        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void initToolbar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void setTitle(CharSequence title) {
        getActionBar().setTitle(title);
    }


    private void initNavigationView() {
        navigationView = (NavigationView) findViewById(R.id.navigation_view);

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

        return true;
    }

    public void showQrDialog() {
        String bitcoinUriString = BitcoinURI.convertToBitcoinURI(this.walletServiceBinder.getCurrentReceiveAddress(), null, null, null);
        try {
            QrDialogFragment.newInstance(new BitcoinURI(bitcoinUriString)).show(this.getSupportFragmentManager(), "qr_dialog_fragment");
        } catch (BitcoinURIParseException e) {
            Log.w(TAG, "Could not parse bitcoin URI: " + bitcoinUriString);
        }
    }

    /* ------------------- PAYMENTS INTEGRATION STARTS HERE  ------------------- */
    private WalletService.WalletServiceBinder walletServiceBinder;

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

        LocalBroadcastManager.getInstance(this).registerReceiver(startClientsBroadcastReceiver, new IntentFilter(Constants.START_CLIENTS_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(stopClientsBroadcastReceiver, new IntentFilter(Constants.STOP_CLIENTS_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(startServersBroadcastReceiver, new IntentFilter(Constants.START_SERVERS_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(walletErrorBroadcastReceiver, new IntentFilter(Constants.WALLET_ERROR_ACTION));

        Intent walletServiceIntent = new Intent(this, WalletService.class);
        this.bindService(walletServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(startClientsBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stopClientsBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(startServersBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(walletErrorBroadcastReceiver);
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
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            initPeers();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String currency = prefs.getString("pref_currency_list", "USD");
            walletServiceBinder.setCurrency(currency);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
    /* -------------------- PAYMENTS INTEGRATION ENDS HERE  -------------------- */


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "discovery supported");
                    // BLE and BL will be supported
                } else {
                    Log.d(TAG, "discovery unsupported");
                    // BLE and BL will not be supported
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

    /**
     * Communication part starts here
     */

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

    private final BroadcastReceiver walletErrorBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String errorMessage = intent.getExtras().getString(Constants.ERROR_MESSAGE_KEY);
            new AlertDialog.Builder(context)
                    .setTitle(getString(R.string.wallet_error_title))
                    .setMessage(errorMessage)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivity.this.finish();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    };

    private void initPeers() {
        // TODO: do we need to stop the servers/clients first before we lose the references?
        this.servers.clear();
        this.clients.clear();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final Set<String> connectionSettings = sharedPreferences.getStringSet(AppConstants.CONNECTION_SETTINGS_PREF_KEY, new HashSet<String>());

        if (connectionSettings.contains(AppConstants.NFC_ACTIVATED)) {
            clients.add(new NFCClient(this, walletServiceBinder));
            servers.add(new NFCServerACS(this, walletServiceBinder));
            servers.add(new NFCServer(this, walletServiceBinder));

        }

        if (connectionSettings.contains(AppConstants.BT_ACTIVATED)) {
            clients.add(new BluetoothLEClient(this, walletServiceBinder));
            servers.add(new BluetoothLEServer(this, walletServiceBinder));
        }

        if (connectionSettings.contains(AppConstants.WIFIDIRECT_ACTIVATED)) {
            clients.add(new WiFiClient(this, walletServiceBinder));
            servers.add(new WiFiServer(this, walletServiceBinder));
        }

        for (AbstractServer server : servers) {
            server.setPaymentRequestDelegate(getClientPaymentRequestDelegate());
        }

        for (AbstractClient client : clients) {
            client.setPaymentRequestDelegate(getClientPaymentRequestDelegate());
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
                final Intent instantPaymentSucess = new Intent(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(instantPaymentSucess);
                stopClients();
                stopServers();
                if (authViewDialog != null && authViewDialog.isAdded()) {
                    authViewDialog.dismiss();
                }
            }

            @Override
            public void onPaymentError(String errorMessage) {
                final Intent instantPaymentFailed = new Intent(Constants.INSTANT_PAYMENT_FAILED_ACTION);
                instantPaymentFailed.putExtra(Constants.ERROR_MESSAGE_KEY, errorMessage);
                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(instantPaymentFailed);
                stopClients();
                stopServers();
                if (authViewDialog != null && authViewDialog.isAdded()) {
                    authViewDialog.dismiss();
                }
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

