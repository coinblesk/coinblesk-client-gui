package com.coinblesk.payments.communications.peers.handlers;

import android.util.Log;

import com.coinblesk.client.config.Constants;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.OnResultListener;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by Alessandro De Carli (@a_d_c_) on 05/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DHKeyExchangeClientHandler extends DHKeyExchangeHandler {
    private final static String TAG = DHKeyExchangeClientHandler.class.getSimpleName();

    public DHKeyExchangeClientHandler(InputStream inputStream, OutputStream outputStream, OnResultListener<SecretKeySpec> resultListener) {
        super(inputStream, outputStream, resultListener);
    }

    @Override
    public void run() {
        try {
            Log.d(TAG, "write start!");
            writeDERObject(new DERObject(this.getKeyPair().getPublic().getEncoded()));
            Log.d(TAG, "write finished!");
            Log.d(TAG, "read start!");
            DERObject responseDERObject = readDERObject();
            Log.d(TAG, "read finished!");
            KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "SC");
            X509EncodedKeySpec x509ks = new X509EncodedKeySpec(responseDERObject.getPayload());
            PublicKey publicKey = keyFactory.generatePublic(x509ks);
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "SC");
            keyAgreement.init(this.getKeyPair().getPrivate());
            keyAgreement.doPhase(publicKey, true);
            final byte[] sharedKey = Arrays.copyOfRange(keyAgreement.generateSecret(), 0, Constants.SYMMETRIC_KEY_SIZE);
            Log.d(TAG, "exchange finished!");
            this.getResultListener().onSuccess(new SecretKeySpec(sharedKey, Constants.SYMMETRIC_CIPHER_ALGORITH));
        } catch (Exception e) {
            this.getResultListener().onError(e.getMessage());
        }
    }
}
