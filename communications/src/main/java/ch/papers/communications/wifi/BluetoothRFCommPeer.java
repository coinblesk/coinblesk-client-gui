package ch.papers.communications.wifi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;

import ch.papers.communications.AbstractPeer;
import ch.papers.communications.Constants;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class BluetoothRFCommPeer extends AbstractPeer {
    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothServerSocket serverSocket;

    public BluetoothRFCommPeer(Context context) {
        super(context);
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public void start() {
        makeDiscoverable();

        this.startThread(new Runnable() {
            @Override
            public void run() {
                if (!isRunning()) {
                    setRunning(true);
                    try {
                        serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(Constants.SERVICE_UUID.toString(), Constants.SERVICE_UUID);
                        BluetoothSocket socket;
                        while ((socket = serverSocket.accept()) != null && isRunning()) {
                        }
                    } catch (IOException e) {
                    }
                    setRunning(false);
                }
            }
        });
    }

    @Override
    public void stop() {

    }

    private void makeDiscoverable() {
        if (!this.bluetoothAdapter.isEnabled()) {
            this.bluetoothAdapter.enable();
        }

        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Constants.DISCOVERABLE_DURATION);
        this.getContext().startActivity(discoverableIntent);
    }
}
