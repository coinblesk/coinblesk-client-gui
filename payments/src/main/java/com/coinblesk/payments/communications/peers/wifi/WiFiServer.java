package com.coinblesk.payments.communications.peers.wifi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.peers.handlers.DHKeyExchangeServerHandler;
import com.coinblesk.payments.communications.peers.handlers.InstantPaymentServerHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;


/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class WiFiServer extends AbstractServer {
    private final static String TAG = WiFiServer.class.getSimpleName();

    private ServerSocket serverSocket;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private static final String TXTRECORD_PROP_AVAILABLE = "available";
    private static final String SERVICE_INSTANCE = "_wifidemotest";
    private static final String SERVICE_REG_TYPE = "_presence._tcp";

    public WiFiServer(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
    }

    @Override
    public void onStart() {
        WifiManager wifiManager = (WifiManager) this.getContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        this.makeDiscoverable();
    }

    @Override
    public void onStop() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                Log.w(TAG, "Exception while closing socket: ", e);
            }
        }

        this.manager.removeGroup(channel, new LogActionListener("removeGroup"));
        this.manager.clearLocalServices(channel, new LogActionListener("clearLocalServices"));
    }

    @Override
    public boolean isSupported() {
        return this.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);
    }

    private final WifiP2pManager.ActionListener groupCreationActionListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            final Map<String, String> record = new HashMap<String, String>();
            record.put(TXTRECORD_PROP_AVAILABLE, "visible");
            manager.addLocalService(channel, WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_REG_TYPE, record), new LogActionListener("addLocalService"));
        }

        @Override
        public void onFailure(int reason) {
            new LogActionListener("groupCreation").onFailure(reason);
        }
    };


    private void makeDiscoverable() {
        this.manager = (WifiP2pManager) this.getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(this.getContext(), this.getContext().getMainLooper(), null);

        this.manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if (group != null) {

                    manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            manager.createGroup(channel, groupCreationActionListener);
                        }

                        @Override
                        public void onFailure(int reason) {
                            new LogActionListener("removeGroup").onFailure(reason);
                        }
                    });
                } else {
                    manager.createGroup(channel, groupCreationActionListener);
                }
            }
        });

        this.startThread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(Constants.WIFI_SERVICE_PORT);
                    Socket socket;
                    while ((socket = serverSocket.accept()) != null && isRunning()) {
                        Log.d(TAG, "new socket just connected");
                        final Socket clientSocket = socket;
                        new Thread(new DHKeyExchangeServerHandler(clientSocket.getInputStream(), clientSocket.getOutputStream(), new OnResultListener<SecretKeySpec>() {
                            @Override
                            public void onSuccess(SecretKeySpec secretKeySpec) {

                                try {
                                    final byte[] iv = new byte[16];
                                    Arrays.fill(iv, (byte) 0x00);
                                    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

                                    final Cipher writeCipher = Cipher.getInstance(Constants.SYMMETRIC_CIPHER_MODE);
                                    writeCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

                                    final Cipher readCipher = Cipher.getInstance(Constants.SYMMETRIC_CIPHER_MODE);
                                    readCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

                                    final OutputStream encrytpedOutputStream = new CipherOutputStream(clientSocket.getOutputStream(), writeCipher);
                                    final InputStream encryptedInputStream = new CipherInputStream(clientSocket.getInputStream(), readCipher);
                                    new Thread(new InstantPaymentServerHandler(encryptedInputStream, encrytpedOutputStream, getPaymentRequestUri(), getPaymentRequestDelegate(), getWalletServiceBinder()), "WiFiServer.InstantPaymentServerHandler").start();
                                } catch (Exception e) {
                                    Log.e(TAG, "Exception in onSuccess: ", e);
                                }
                            }

                            @Override
                            public void onError(String s) {
                                Log.d(TAG, "error during key exchange:" + s);
                            }
                        }), "WiFiServer.DHKeyExchange").start();
                    }
                } catch (Exception e) {
                    // SocketException closed is expected here, when onStop is called
                    Log.e(TAG, "Exception in WiFi server: ", e);
                }
            }
        });
    }
}
