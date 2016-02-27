package ch.papers.communications.wifi;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import ch.papers.communications.AbstractPeer;
import ch.papers.communications.Constants;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class WiFiPeer extends AbstractPeer {
    private ServerSocket serverSocket;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    public WiFiPeer(Context context) {
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
        this.manager.removeGroup(channel, null);
        this.manager.clearLocalServices(channel,null);
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

    private void makeDiscoverable() {
        this.manager = (WifiP2pManager) this.getContext().getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = this.manager.initialize(this.getContext(), this.getContext().getMainLooper(), null);

        this.manager.stopPeerDiscovery(this.channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                manager.addLocalService(channel, WifiP2pDnsSdServiceInfo.newInstance(Constants.SERVICE_UUID.toString(),Constants.SERVICE_UUID.toString(),new HashMap<String, String>()), new WifiP2pManager.ActionListener() {
                                    @Override
                                    public void onSuccess() {
                                        startThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                setRunning(true);
                                                try {
                                                    serverSocket = new ServerSocket(Constants.SERVICE_PORT);
                                                    Socket socket;
                                                    while ((socket = serverSocket.accept()) != null && isRunning()) {
                                                        //new Thread(new EchoServerHandler(socket.getInputStream(), socket.getOutputStream())).start();
                                                    }
                                                } catch (IOException e) {
                                                }
                                            }
                                        });

                                    }

                                    @Override
                                    public void onFailure(int reason) {
                                        this.onSuccess();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(int reason) {
                                this.onSuccess();
                            }
                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        this.onSuccess();
                    }
                });
            }

            @Override
            public void onFailure(int reason) {
                this.onSuccess();
            }
        });
    }
}
