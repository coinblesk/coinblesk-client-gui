package com.coinblesk.payments.communications.peers.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.coinblesk.payments.Constants;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.peers.handlers.DHKeyExchangeServerHandler;
import com.coinblesk.payments.communications.peers.handlers.InstantPaymentServerHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;


/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class BluetoothRFCommServer extends AbstractServer {
    private final static String TAG = BluetoothRFCommServer.class.getSimpleName();

    private final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothServerSocket serverSocket;

    private Map<SecretKeySpec, BluetoothSocket> secureConnections = new ConcurrentHashMap<SecretKeySpec, BluetoothSocket>();

    public BluetoothRFCommServer(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context, walletServiceBinder);
    }

    @Override
    public boolean isSupported() {
        return bluetoothAdapter != null;
    }

    @Override
    public void onChangePaymentRequest() {
        if(this.hasPaymentRequestUri()) {
            for (Map.Entry<SecretKeySpec, BluetoothSocket> entry : secureConnections.entrySet()) {
                try {
                    final byte[] iv = new byte[16];
                    Arrays.fill(iv, (byte) 0x00);
                    IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

                    final Cipher writeCipher = Cipher.getInstance(Constants.SYMMETRIC_CIPHER_MODE);
                    writeCipher.init(Cipher.ENCRYPT_MODE, entry.getKey(),ivParameterSpec);

                    final Cipher readCipher = Cipher.getInstance(Constants.SYMMETRIC_CIPHER_MODE);
                    readCipher.init(Cipher.DECRYPT_MODE, entry.getKey(),ivParameterSpec);

                    final OutputStream encrytpedOutputStream = new CipherOutputStream(entry.getValue().getOutputStream(), writeCipher);
                    final InputStream encryptedInputStream = new CipherInputStream(entry.getValue().getInputStream(), readCipher);

                    Log.d(TAG,"setting up secure connection");
                    this.secureConnections.remove(entry);
                    new Thread(new InstantPaymentServerHandler(encryptedInputStream, encrytpedOutputStream, this.getPaymentRequestUri(), this.getPaymentRequestAuthorizer(), this.getWalletServiceBinder())).start();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }
            }
        }
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
                            final BluetoothSocket clientSocket = socket;
                            new Thread(new DHKeyExchangeServerHandler(clientSocket.getInputStream(), clientSocket.getOutputStream(), new OnResultListener<SecretKeySpec>() {
                                @Override
                                public void onSuccess(SecretKeySpec secretKeySpec) {
                                    secureConnections.put(secretKeySpec, clientSocket);
                                    onChangePaymentRequest();
                                }

                                @Override
                                public void onError(String s) {
                                    Log.d(TAG, "error during key exchange:" + s);
                                }
                            })).start();
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
        for (BluetoothSocket socket : this.secureConnections.values()) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
        this.secureConnections.clear();
        this.setRunning(false);
    }

    private void makeDiscoverable() {
        if (!this.bluetoothAdapter.isEnabled()) {
            this.bluetoothAdapter.enable();
        }

        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, Constants.DISCOVERABLE_DURATION);
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.getContext().startActivity(discoverableIntent);
        }
    }
}
