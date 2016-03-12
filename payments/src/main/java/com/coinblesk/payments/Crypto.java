package com.coinblesk.payments;

import org.bitcoinj.core.ECKey;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.zip.GZIPOutputStream;

import javax.crypto.KeyAgreement;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 06/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class Crypto {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static byte[] generateCommonSecret(ECKey privateKey, ECKey publicKey) {
        try {
            final KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "SC");
            final KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "SC");
            final ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");
            final ECPublicKeySpec pubSpec = new ECPublicKeySpec(publicKey.getPubKeyPoint(), ecSpec);
            final ECPrivateKeySpec privSpec = new ECPrivateKeySpec(privateKey.getPrivKey(), ecSpec);

            final PublicKey pubKey = keyFactory.generatePublic(pubSpec);
            final PrivateKey privKey = keyFactory.generatePrivate(privSpec);

            keyAgreement.init(privKey);
            keyAgreement.doPhase(pubKey, true);

            return keyAgreement.generateSecret();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] compress(byte[] payload) {
        try {
            ByteArrayOutputStream byteStream =
                    new ByteArrayOutputStream(payload.length);
            GZIPOutputStream zipStream =
                    new GZIPOutputStream(byteStream);
            zipStream.write(payload);
            zipStream.close();
            byteStream.close();
            return byteStream.toByteArray();
        } catch (IOException e) {

        }
        return null;
    }
}
