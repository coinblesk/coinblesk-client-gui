package com.coinblesk.payments.communications.peers.handlers;

import android.util.Log;

import com.coinblesk.payments.communications.OnResultListener;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import javax.crypto.spec.SecretKeySpec;


/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class DHKeyExchangeHandler extends DERObjectStreamHandler {
    private final static String TAG = DHKeyExchangeHandler.class.getSimpleName();
    private final OnResultListener<SecretKeySpec> resultListener;
    private KeyPair keyPair;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public DHKeyExchangeHandler(InputStream inputStream, OutputStream outputStream, OnResultListener<SecretKeySpec> resultListener) {
        super(inputStream,outputStream);
        this.resultListener = resultListener;

        Log.d(TAG,"starting exchange");
        try {
            final ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp224k1");
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "SC");
            keyPairGenerator.initialize(ecGenParameterSpec);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            Log.w(TAG, "Could not initialize DHKeyExchange: ", e);
            // TODO: we should not catch the exception here because we cannot continue with an error and we cannot know what went wrong.
            // FIXME: catch outside and abort.
        }
    }

    public OnResultListener<SecretKeySpec> getResultListener() {
        return resultListener;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }
}
