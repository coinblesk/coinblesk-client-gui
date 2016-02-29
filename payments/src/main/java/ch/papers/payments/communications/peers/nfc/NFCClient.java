package ch.papers.payments.communications.peers.nfc;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.provider.Settings;
import android.util.Log;

import org.bitcoinj.uri.BitcoinURI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;
import ch.papers.payments.communications.peers.AbstractPeer;
import ch.papers.payments.communications.peers.handlers.DHKeyExchangeHandler;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class NFCClient extends AbstractPeer{
    private final static String TAG = NFCClient.class.getSimpleName();

    private static final byte[] CLA_INS_P1_P2 = { (byte)0x00, (byte)0xA4, (byte)0x04, (byte)0x00 };
    private static final byte[] AID_ANDROID = { (byte)0xF0, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 };


    private static final byte[] selectCommand = {
            (byte)0x00, // CLA
            (byte)0xA4, // INS
            (byte)0x04, // P1
            (byte)0x00, // P2
            (byte)0x0A, // LC
            (byte)0x01,(byte)0x02,(byte)0x03,(byte)0x04,(byte)0x05,(byte)0x06,(byte)0x07,(byte)0x08,(byte)0x09,(byte)0xFF, // AID
            (byte)0x7F  // LE
    };

    private final Activity activity;
    private final NfcAdapter nfcAdapter;

    final byte[] buffer = new byte[Constants.BUFFER_SIZE];
    final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(Constants.BUFFER_SIZE);

    public NFCClient(Activity activity) {
        super(activity);
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.activity);
    }

    @Override
    public boolean isSupported() {
        return (this.nfcAdapter != null);
    }

    @Override
    public void broadcastPaymentRequest(BitcoinURI paymentUri) {

    }

    @Override
    public void start() {
        if (!nfcAdapter.isEnabled())
        {
            this.activity.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            return;
        }


        nfcAdapter.enableReaderMode(this.activity, new NfcAdapter.ReaderCallback() {
                    @Override
                    public void onTagDiscovered(Tag tag) {
                        try {
                            IsoDep isoDep = IsoDep.get(tag);
                            isoDep.connect();

                            byte[] response = isoDep.transceive(createSelectAidApdu(AID_ANDROID));

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

                            int readByteCounter = 0;
                            byte[] payload = new byte[Constants.BUFFER_SIZE];
                            while((readByteCounter=byteArrayInputStream.read(payload))>0){
                                int byteCounter = 0;
                                while(byteCounter < readByteCounter){
                                    byte[] fragment = Arrays.copyOfRange(payload,byteCounter,byteCounter+isoDep.getMaxTransceiveLength());


                                    byteArrayOutputStream.write(isoDep.transceive(fragment));
                                    byteCounter += fragment.length;
                                }
                            }

                            isoDep.close();
                        } catch (IOException e) {
                            Log.d(TAG, e.getMessage());
                        }
                    }
                }, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null);
    }

    @Override
    public void stop() {
        nfcAdapter.disableReaderMode(this.activity);
    }

    private byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte)aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }
}
