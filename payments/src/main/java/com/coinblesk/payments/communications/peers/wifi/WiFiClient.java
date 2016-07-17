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

package com.coinblesk.payments.communications.peers.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.OnResultListener;
import com.coinblesk.payments.communications.peers.AbstractClient;
import com.coinblesk.payments.communications.peers.handlers.DHKeyExchangeClientHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class WiFiClient extends AbstractClient implements WifiP2pManager.ConnectionInfoListener {
    private final static String TAG = WiFiClient.class.getName();

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private ExecutorService singleThreadExecutor;

    private long startTime;
    private long duration;


    public WiFiClient(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // When discovery finds a device
            switch (action) {
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: /* fall through */
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                    manager.requestConnectionInfo(channel, WiFiClient.this);
                    break;
                default:
                    Log.d(TAG, "Received action (will not be handled): " + action);
            }
        }
    };

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        startTime = System.currentTimeMillis();
        singleThreadExecutor = Executors.newSingleThreadExecutor();

        WifiManager wifiManager = (WifiManager) this.getContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        manager = (WifiP2pManager) getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(getContext(), getContext().getMainLooper(), null);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        getContext().registerReceiver(broadcastReceiver, filter);

        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group != null) {
                    manager.removeGroup(channel, new LogActionListener("WiFiClient.onStart - removeGroup"));
                }
            }
        });

        manager.setDnsSdResponseListeners(channel, new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                connect(srcDevice);
            }
        }, new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {
                //connect(srcDevice);
            }
        });
        manager.addServiceRequest(channel, WifiP2pDnsSdServiceRequest.newInstance(), new LogActionListener("WiFiClient - addServiceRequest"));
        manager.discoverServices(channel, new LogActionListener("WiFiClient - discoverServices"));
    }

    @Override
    public void onStop() {
        try {
            manager.clearServiceRequests(channel, new LogActionListener("WiFiClient - clearServiceRequests"));
            manager.stopPeerDiscovery(channel, new LogActionListener("WiFiClient - stopPeerDiscovery"));
            manager.cancelConnect(channel, new LogActionListener("WiFiClient - cancelConnect"));
            manager.removeGroup(channel, new LogActionListener("WiFiClient - removeGroup"));
            manager.clearLocalServices(channel, new LogActionListener("WiFiClient - clearLocalServices"));

            singleThreadExecutor.shutdown();
            getContext().unregisterReceiver(broadcastReceiver);

        } catch (Exception e) {
            Log.d(TAG, "Exception in onStop: ", e);
        }

        manager = null;
        channel = null;
        singleThreadExecutor = null;
    }

    private void connect(WifiP2pDevice device) {
        duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "connect WifiP2pDevice: starting connection (duration: "+duration+" ms)");
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        manager.connect(channel, config, new LogActionListener("WiFiClient.connect"));
    }

    @Override
    public boolean isSupported() {
        return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "onConnectionInfoAvailable: " + info + " (duration "+duration+" ms");
        if (!info.isGroupOwner && info.groupFormed) {
            singleThreadExecutor.submit(new WifiClientRunnable(info.groupOwnerAddress, Constants.WIFI_SERVICE_PORT));
        }
    }

    private class WifiClientRunnable implements Runnable {
        private final InetAddress groupOwnerAddress;
        private final int servicePort;
        private Socket socket;

        public WifiClientRunnable(InetAddress groupOwnerAddress, int servicePort) {
            this.groupOwnerAddress = groupOwnerAddress;
            this.servicePort = servicePort;
        }

        @Override
        public void run() {
            try {
                // TODO: close socket and streams!
                socket = new Socket(groupOwnerAddress, servicePort);
                DHKeyExchangeClientHandler dhKeyExchange = new DHKeyExchangeClientHandler(
                        socket.getInputStream(),
                        socket.getOutputStream(),
                        new OnKeyExchange());
                dhKeyExchange.run();
            } catch (Exception e) {
                Log.w(TAG, "Exception during connection: ", e);
            }
        }

        private class OnKeyExchange implements OnResultListener<SecretKeySpec> {
            @Override
            public void onSuccess(SecretKeySpec secretKeySpec) {
                duration = System.currentTimeMillis() - startTime;
                Log.d(TAG, "OnKeyExchange: exchange successful (duration "+duration+" ms)");
                try {
                    final byte[] iv = new byte[16];
                    Arrays.fill(iv, (byte) 0x00);
                    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

                    final Cipher writeCipher = Cipher.getInstance(Constants.SYMMETRIC_CIPHER_MODE);
                    writeCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

                    final Cipher readCipher = Cipher.getInstance(Constants.SYMMETRIC_CIPHER_MODE);
                    readCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

                    final InputStream encryptedInputStream = new CipherInputStream(socket.getInputStream(), readCipher);
                    final OutputStream encrytpedOutputStream = new CipherOutputStream(socket.getOutputStream(), writeCipher);

                    Log.d(TAG, "Start WifiDirect payment handler");
                    Thread t = new Thread(
                            new InstantPaymentClientHandlerCLTV(
                                    encryptedInputStream,
                                    encrytpedOutputStream,
                                    getWalletServiceBinder(),
                                    getPaymentRequestDelegate()),
                            "WiFiClient.InstantPaymentClientHandler");
                    t.start();
                } catch (Exception e) {
                    Log.w(TAG, "Exception onSuccess: ", e);
                }
            }

            @Override
            public void onError(String s) {
                Log.d(TAG, "OnKeyExchange: error during key exchange:" + s);
            }
        }
    }
}