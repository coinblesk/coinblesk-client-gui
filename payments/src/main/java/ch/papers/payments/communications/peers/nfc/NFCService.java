package ch.papers.payments.communications.peers.nfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;
import ch.papers.payments.communications.peers.handlers.DHKeyExchangeHandler;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class NFCService extends HostApduService {
    private final static String TAG = NFCService.class.getSimpleName();

    final byte[] buffer = new byte[Constants.BUFFER_SIZE];
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Constants.BUFFER_SIZE);

    @Override
    public void onCreate() {
        super.onCreate();
        new DHKeyExchangeHandler(byteArrayInputStream, byteArrayOutputStream, new OnResultListener<SecretKeySpec>() {
            @Override
            public void onSuccess(SecretKeySpec secretKeySpec) {
                Log.d(TAG,"exchange successful");
            }

            @Override
            public void onError(String s) {
                Log.d(TAG, "error during key exchange:" + s);
            }
        });
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {

        if (this.selectAidApdu(commandApdu)) {
            Log.d(TAG, "hanshake");
        }

        return new byte[0];
    }

    @Override
    public void onDeactivated(int reason) {

    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte) 0 && apdu[1] == (byte) 0xa4;
    }
}
