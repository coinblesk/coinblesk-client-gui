package ch.papers.payments.communications.peers.handlers;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DHKeyExchangeHandler implements Runnable {
    private final static String TAG = DHKeyExchangeHandler.class.getSimpleName();

    private final static int BUFFER_SIZE = 1024;
    private final static int KEY_SIZE = 80;

    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final OnResultListener<SecretKeySpec> resultListener;
    private KeyPair keyPair;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public DHKeyExchangeHandler(InputStream inputStream, OutputStream outputStream, OnResultListener<SecretKeySpec> resultListener) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.resultListener = resultListener;

        Log.d(TAG,"starting exchange");
        try {
            final ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp224k1");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "SC");
            keyPairGenerator.initialize(ecGenParameterSpec);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        final Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] encodedKey = keyPair.getPublic().getEncoded();
                    outputStream.write(encodedKey);
                    outputStream.flush();
                } catch (IOException e) {
                    resultListener.onError(e.getMessage());
                }
            }
        });

        final Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] keyBytes = new byte[KEY_SIZE];
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesReadCounter = 0;
                    int totalBytesRead = 0;

                    while (totalBytesRead < KEY_SIZE && (bytesReadCounter = inputStream.read(buffer)) > 0) {
                        System.arraycopy(buffer,
                                0,
                                keyBytes,
                                totalBytesRead,
                                bytesReadCounter);
                        totalBytesRead+=bytesReadCounter;
                    }

                    KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "SC");
                    X509EncodedKeySpec x509ks = new X509EncodedKeySpec(keyBytes);
                    PublicKey publicKey = keyFactory.generatePublic(x509ks);

                    KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "SC");
                    keyAgreement.init(keyPair.getPrivate());
                    keyAgreement.doPhase(publicKey, true);
                    final byte[] sharedKey = keyAgreement.generateSecret();

                    resultListener.onSuccess(new SecretKeySpec(sharedKey, Constants.SYMMETRIC_CIPHER_ALGORITH));
                } catch (Exception e) {
                    resultListener.onError(e.getMessage());
                }
            }
        });

        writeThread.start();
        readThread.start();

        try {
            writeThread.join();
            readThread.join();
        } catch (InterruptedException e) {
            this.resultListener.onError(e.getMessage());
        }
    }
}
