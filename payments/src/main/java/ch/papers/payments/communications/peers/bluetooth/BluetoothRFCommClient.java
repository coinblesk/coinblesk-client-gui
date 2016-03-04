package ch.papers.payments.communications.peers.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.peers.AbstractClient;
import ch.papers.payments.communications.peers.handlers.DHKeyExchangeHandler;
import ch.papers.payments.communications.peers.handlers.InstantPaymentClientHandler;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class BluetoothRFCommClient extends AbstractClient {
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
                    final BluetoothSocket clientSocket = device.createInsecureRfcommSocketToServiceRecord(Constants.SERVICE_UUID);
                    clientSocket.connect();

                    new Thread(new DHKeyExchangeHandler(clientSocket.getInputStream(), clientSocket.getOutputStream(), new OnResultListener<SecretKeySpec>() {
                        @Override
                        public void onSuccess(SecretKeySpec secretKeySpec) {
                            Log.d(TAG,"exchange successful");
                            try {
                                final Cipher writeCipher = Cipher.getInstance("AES");
                                writeCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

                                final Cipher readCipher = Cipher.getInstance("AES");
                                readCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

                                final OutputStream encrytpedOutputStream = new CipherOutputStream(clientSocket.getOutputStream(), writeCipher);
                                final InputStream encryptedInputStream = new CipherInputStream(clientSocket.getInputStream(), readCipher);

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
                    })).start();
                } catch (Exception e) {
                }
            }
        }
    };

    public BluetoothRFCommClient(Context context,WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
    }

    @Override
    public void onIsReadyForInstantPaymentChange() {

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
        this.setRunning(false);
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
