package ch.papers.payments.communications.peers;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.bitcoinj.uri.BitcoinURI;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.papers.payments.Constants;
import ch.papers.payments.communications.peers.bluetooth.BluetoothLEServer;
import ch.papers.payments.communications.peers.nfc.NFCServer;
import ch.papers.payments.communications.peers.wifi.WiFiServer;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ServerPeerService extends Service {
    private final String CONNECTION_SETTINGS_PREF_KEY = "pref_connection_settings";
    private final String NFC_ACTIVATED = "nfc-checked";
    private final String BT_ACTIVATED = "bt-checked";
    private final String WIFIDIRECT_ACTIVATED = "wifi-checked";

    public class ServerServiceBinder extends Binder {
        public void broadcastPaymentRequest(BitcoinURI paymentUri){
            for (AbstractServer server:ServerPeerService.this.servers) {
                if(server.isRunning()) {
                    server.setPaymentRequestUri(paymentUri);
                }
            }
        }

        public void cancelPaymentRequest() {
            for (AbstractServer server:ServerPeerService.this.servers) {
                if(server.isRunning()) {
                    server.setPaymentRequestUri(null);
                }
                //server.stop();
            }
        }

        public boolean hasSupportedServers() {
            for (AbstractServer server:ServerPeerService.this.servers) {
                if(server.isSupported()){
                    return true;
                }
            }
            return false;
        }
    }

    private final ServerServiceBinder serverServiceBinder = new ServerServiceBinder();

    private final List<AbstractServer> servers = new ArrayList<AbstractServer>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.serverServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                final Set<String> connectionSettings = sharedPreferences.getStringSet(CONNECTION_SETTINGS_PREF_KEY, new HashSet<String>());

                if(connectionSettings.contains(NFC_ACTIVATED)){
                    ServerPeerService.this.servers.add(new NFCServer(ServerPeerService.this));
                }

                if(connectionSettings.contains(BT_ACTIVATED)){
                    //ServerPeerService.this.servers.add(new BluetoothRFCommServer(ServerPeerService.this));
                    ServerPeerService.this.servers.add(new BluetoothLEServer(ServerPeerService.this));
                }

                if(connectionSettings.contains(WIFIDIRECT_ACTIVATED)) {
                    ServerPeerService.this.servers.add(new WiFiServer(ServerPeerService.this));
                }

                for (AbstractServer server:ServerPeerService.this.servers) {
                    if(server.isSupported()) {
                        server.setPaymentRequestAuthorizer(new PaymentRequestAuthorizer() {
                            @Override
                            public boolean isPaymentRequestAuthorized(BitcoinURI paymentRequest) {
                                return true;
                            }

                            @Override
                            public void onPaymentSuccess() {
                                final Intent instantPaymentSucess = new Intent(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION);
                                LocalBroadcastManager.getInstance(ServerPeerService.this).sendBroadcast(instantPaymentSucess);
                            }

                            @Override
                            public void onPaymentError(String errorMessage) {
                                final Intent instantPaymentFailed = new Intent(Constants.INSTANT_PAYMENT_FAILED_ACTION);
                                instantPaymentFailed.putExtra(Constants.ERROR_MESSAGE_KEY,errorMessage);
                                LocalBroadcastManager.getInstance(ServerPeerService.this).sendBroadcast(instantPaymentFailed);
                            }
                        });
                        server.start();
                    }
                }
            }
        }).start();

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Peer server:this.servers) {
            if(server.isSupported()) {
                server.stop();
            }
        }
    }
}
