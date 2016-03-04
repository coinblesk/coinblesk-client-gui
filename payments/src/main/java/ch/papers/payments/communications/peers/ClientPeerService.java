package ch.papers.payments.communications.peers;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ch.papers.payments.WalletService;
import ch.papers.payments.communications.peers.bluetooth.BluetoothLEClient;
import ch.papers.payments.communications.peers.bluetooth.BluetoothRFCommClient;
import ch.papers.payments.communications.peers.wifi.WiFiClient;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class ClientPeerService extends Service {

    public class PeerServiceBinder extends Binder {

        public void setIsReadyForInstantPayment(){
            for (AbstractClient client:ClientPeerService.this.clients) {
                if(client.isRunning()) {
                    client.setReadyForInstantPayment(true);
                }
            }
        }
    }

    private final PeerServiceBinder peerServiceBinder = new PeerServiceBinder();

    private final List<AbstractClient> clients = new ArrayList<AbstractClient>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.peerServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent serviceIntet = new Intent(this, WalletService.class);
        this.bindService(serviceIntet, serviceConnection, Context.BIND_AUTO_CREATE);

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Peer peer:this.clients) {
            peer.stop();
        }
        this.unbindService(serviceConnection);
    }

    private WalletService.WalletServiceBinder walletServiceBinder;

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            walletServiceBinder = (WalletService.WalletServiceBinder) binder;
            clients.add(new WiFiClient(ClientPeerService.this,walletServiceBinder));
            clients.add(new BluetoothRFCommClient(ClientPeerService.this,walletServiceBinder));
            clients.add(new BluetoothLEClient(ClientPeerService.this,walletServiceBinder));
            //clients.add(new NFCClient(ClientPeerService.this,walletServiceBinder));

            for (Peer peer:clients) {
                if(peer.isSupported()) {
                    peer.start();
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            walletServiceBinder = null;
        }
    };
}
