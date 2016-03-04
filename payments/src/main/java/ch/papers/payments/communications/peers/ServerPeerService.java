package ch.papers.payments.communications.peers;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.bitcoinj.uri.BitcoinURI;

import java.util.ArrayList;
import java.util.List;

import ch.papers.payments.communications.peers.bluetooth.BluetoothLEServer;
import ch.papers.payments.communications.peers.bluetooth.BluetoothRFCommServer;
import ch.papers.payments.communications.peers.nfc.NFCServer;
import ch.papers.payments.communications.peers.wifi.WiFiServer;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ServerPeerService extends Service {

    public class ServerServiceBinder extends Binder {
        public void broadcastPaymentRequest(BitcoinURI paymentUri){
            for (AbstractServer server:ServerPeerService.this.servers) {
                if(server.isRunning()) {
                    server.broadcastPaymentRequest(paymentUri);
                }
            }
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
        this.servers.add(new WiFiServer(this));
        this.servers.add(new BluetoothRFCommServer(this));
        this.servers.add(new BluetoothLEServer(this));
        this.servers.add(new NFCServer(this));

        for (Peer server:this.servers) {
            if(server.isSupported()) {
                server.start();
            }
        }

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
