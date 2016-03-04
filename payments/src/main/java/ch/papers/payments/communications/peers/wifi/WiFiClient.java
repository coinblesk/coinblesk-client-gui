package ch.papers.payments.communications.peers.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.peers.AbstractClient;
import ch.papers.payments.communications.peers.bluetooth.BluetoothRFCommClient;
import ch.papers.payments.communications.peers.handlers.DHKeyExchangeHandler;
import ch.papers.payments.communications.peers.handlers.InstantPaymentClientHandler;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class WiFiClient extends AbstractClient implements WifiP2pManager.ConnectionInfoListener {
    private final static String TAG = BluetoothRFCommClient.class.getSimpleName();

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Socket socket;

    private boolean isConnected = false;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // When discovery finds a device
            if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                manager.requestConnectionInfo(channel, WiFiClient.this);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                manager.requestConnectionInfo(channel, WiFiClient.this);
            }
        }
    };

    public WiFiClient(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
    }

    @Override
    public void start() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        this.getContext().registerReceiver(this.broadcastReceiver, filter);

        WifiManager wifiManager = (WifiManager) this.getContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        manager = (WifiP2pManager) this.getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this.getContext(), this.getContext().getMainLooper(), null);
        this.manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if(group != null){
                    manager.removeGroup(channel,new LogActionListener("removeGroup"));
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
                connect(srcDevice);
            }
        });
        manager.addServiceRequest(channel, WifiP2pDnsSdServiceRequest.newInstance(), new LogActionListener("addServiceRequest"));
        manager.discoverServices(channel, new LogActionListener("discoverServices"));
    }

    @Override
    public void stop() {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });

        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {

            }
        });

        this.setRunning(false);
    }

    private void connect(WifiP2pDevice device) {
        Log.d(TAG,"starting connection");
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        manager.connect(channel, config, new LogActionListener("connect"));
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (!info.isGroupOwner && info.groupFormed && !isConnected) {
            isConnected = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket = new Socket(info.groupOwnerAddress, Constants.SERVICE_PORT);
                        new DHKeyExchangeHandler(socket.getInputStream(), socket.getOutputStream(), new OnResultListener<SecretKeySpec>() {
                            @Override
                            public void onSuccess(SecretKeySpec secretKeySpec) {
                                Log.d(TAG,"exchange successful");
                                try {

                                    final Cipher writeCipher = Cipher.getInstance("AES");
                                    writeCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

                                    final Cipher readCipher = Cipher.getInstance("AES");
                                    readCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

                                    final OutputStream encrytpedOutputStream = new CipherOutputStream(socket.getOutputStream(), writeCipher);
                                    final InputStream encryptedInputStream = new CipherInputStream(socket.getInputStream(), readCipher);

                                    new Thread(new InstantPaymentClientHandler(encryptedInputStream,encrytpedOutputStream,getWalletServiceBinder())).start();
                                    setRunning(true);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                } catch (NoSuchPaddingException e) {
                                    e.printStackTrace();
                                } catch (InvalidKeyException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onError(String s) {
                                Log.d(TAG, "error during key exchange:" + s);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    @Override
    public void onIsReadyForInstantPaymentChange() {

    }
}
