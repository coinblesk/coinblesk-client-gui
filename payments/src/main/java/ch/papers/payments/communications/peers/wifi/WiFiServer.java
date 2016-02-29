package ch.papers.payments.communications.peers.wifi;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;
import ch.papers.payments.communications.peers.AbstractPeer;
import ch.papers.payments.communications.peers.handlers.DHKeyExchangeHandler;


/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class WiFiServer extends AbstractPeer {
    private final static String TAG = WiFiServer.class.getSimpleName();

    private ServerSocket serverSocket;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_wifidemotest";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";


    private Map<SecretKeySpec, Socket> secureConnections = new ConcurrentHashMap<SecretKeySpec, Socket>();


    public WiFiServer(Context context) {
        super(context);
    }

    @Override
    public void start() {
        WifiManager wifiManager = (WifiManager) this.getContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        this.makeDiscoverable();
    }

    @Override
    public void stop() {
        this.manager.removeGroup(channel,new LogActionListener("removeGroup"));
        this.manager.clearLocalServices(channel,new LogActionListener("clearLocalServices"));
    }

    @Override
    public boolean isSupported() {
        // see http://stackoverflow.com/questions/23828487/how-can-i-check-my-android-device-support-wifi-direct
        PackageManager pm = this.getContext().getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo info : features) {
            if (info != null && info.name != null && info.name.equalsIgnoreCase("android.hardware.wifi.direct")) {
                return true;
            }
        }
        return false;
    }

    private final WifiP2pManager.ActionListener groupCreationActionListener = new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
            final Map<String, String> record = new HashMap<String, String>();
            record.put(TXTRECORD_PROP_AVAILABLE, "visible");
            manager.addLocalService(channel, WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_REG_TYPE, record),new LogActionListener("addLocalService"));
        }

        @Override
        public void onFailure(int reason) {
            new LogActionListener("groupCreation").onFailure(reason);
        }
    };

    @Override
    public void broadcastPaymentRequest(BitcoinURI paymentUri) {
        for (Map.Entry<SecretKeySpec, Socket> entry : secureConnections.entrySet()) {
            try {
                final Cipher writeCipher = Cipher.getInstance("AES");
                writeCipher.init(Cipher.ENCRYPT_MODE, entry.getKey());

                final Cipher readCipher = Cipher.getInstance("AES");
                readCipher.init(Cipher.DECRYPT_MODE, entry.getKey());

                final OutputStream encrytpedOutputStream = new CipherOutputStream(entry.getValue().getOutputStream(), writeCipher);
                final InputStream encryptedInputStream = new CipherInputStream(entry.getValue().getInputStream(), readCipher);

                byte[] amountBytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(paymentUri.getAmount().getValue()).array();
                encrytpedOutputStream.write(paymentUri.getAddress().getHash160());
                encrytpedOutputStream.write(amountBytes);
                encrytpedOutputStream.flush();
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
    }

    private void makeDiscoverable() {
        this.manager = (WifiP2pManager) this.getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(this.getContext(), this.getContext().getMainLooper(), null);

        this.manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(WifiP2pGroup group) {
                if(group != null){

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
                setRunning(true);
                try {
                    serverSocket = new ServerSocket(Constants.SERVICE_PORT);
                    Socket socket;
                    while ((socket = serverSocket.accept()) != null && isRunning()) {
                        Log.d(TAG, "new socket just connected");
                        final Socket clientSocket = socket;
                        new Thread(new DHKeyExchangeHandler(clientSocket.getInputStream(), clientSocket.getOutputStream(), new OnResultListener<SecretKeySpec>() {
                            @Override
                            public void onSuccess(SecretKeySpec secretKeySpec) {
                                secureConnections.put(secretKeySpec, clientSocket);
                            }

                            @Override
                            public void onError(String s) {
                                Log.d(TAG, "error during key exchange:" + s);
                            }
                        })).start();
                    }
                } catch (IOException e) {
                }
            }
        });
    }
}
