package ch.papers.payments.communications.peers;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.bitcoinj.uri.BitcoinURI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ClientPeerService extends Service {

    public class PeerServiceBinder extends Binder {

        public void broadcastPaymentRequest(BitcoinURI paymentUri){
            for (Peer peer:ClientPeerService.this.peers) {
                peer.broadcastPaymentRequest(paymentUri);
            }
        }
    }

    private final PeerServiceBinder peerServiceBinder = new PeerServiceBinder();

    private final List<Peer> peers = new ArrayList<Peer>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.peerServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //this.peers.add(new WiFiClient(this));
        //this.peers.add(new BluetoothRFCommClient(this));
        for (Peer peer:this.peers) {
            peer.start();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Peer peer:this.peers) {
            peer.stop();
        }
    }
}
