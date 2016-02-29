package ch.papers.payments.communications.peers.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;

import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;
import ch.papers.payments.communications.peers.AbstractPeer;
import ch.papers.payments.communications.peers.handlers.DHKeyExchangeHandler;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class BluetoothRFCommClient extends AbstractPeer {
    private final static String TAG = BluetoothRFCommClient.class.getSimpleName();


    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BluetoothSocket socket;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "device found:"+device.getAddress());
                try {
                    BluetoothSocket clientSocket = device.createInsecureRfcommSocketToServiceRecord(Constants.SERVICE_UUID);
                    clientSocket.connect();

                    new Thread(new DHKeyExchangeHandler(clientSocket.getInputStream(), clientSocket.getOutputStream(), new OnResultListener<SecretKeySpec>() {
                        @Override
                        public void onSuccess(SecretKeySpec secretKeySpec) {
                            Log.d(TAG, "exchange successful!");
                        }

                        @Override
                        public void onError(String s) {
                            Log.d(TAG, "error during key exchange:" + s);
                        }
                    })).start();
                } catch (Exception e) {
                }
            }
        }
    };

    public BluetoothRFCommClient(Context context) {
        super(context);
    }

    @Override
    public void start() {
        if (!this.bluetoothAdapter.isEnabled()) {
            this.bluetoothAdapter.enable();
        }

        final IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.getContext().registerReceiver(this.broadcastReceiver, filter);
        this.bluetoothAdapter.startDiscovery();
    }

    @Override
    public void stop() {
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public void broadcastPaymentRequest(BitcoinURI paymentUri) {

    }

}
