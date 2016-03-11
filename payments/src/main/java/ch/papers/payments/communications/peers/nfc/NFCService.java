package ch.papers.payments.communications.peers.nfc;

import android.annotation.TargetApi;
import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.Arrays;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.communications.messages.DERParser;
import ch.papers.payments.communications.peers.steps.PaymentAuthorizationReceiveStep;
import ch.papers.payments.communications.peers.steps.PaymentFinalSignatureReceiveStep;
import ch.papers.payments.communications.peers.steps.PaymentRefundReceiveStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestSendStep;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCService extends HostApduService {
    private final static String TAG = NFCService.class.getSimpleName();


    private int stepCounter = 0;
    private boolean isProcessing = false;
    private BitcoinURI bitcoinURI;
    private ECKey clientPublicKey;

    private byte[] derRequestPayload = new byte[0];
    private byte[] derResponsePayload = new byte[0];

    private static final byte[] KEEPALIVE = {1, 2, 3, 4};
    private static final byte[] AID_ANDROID = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x03};
    private static final byte[] AID_ANDROID_ACS = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x04};
    private int maxFragmentSize = 261;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check if intent has extras
        if (intent.getExtras() != null) {
            try {
                String bitcoinUri = intent.getExtras().getString(Constants.BITCOIN_URI_KEY);
                if (!bitcoinUri.equals("")) {
                    bitcoinURI = new BitcoinURI(bitcoinUri);
                }
            } catch (BitcoinURIParseException e) {
                e.printStackTrace();
            }
        }

        return START_NOT_STICKY;
    }


    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.d(TAG, "this is command apdu lenght: " + commandApdu.length);
        int derPayloadStartIndex = 0;
        if (this.selectAidApdu(commandApdu)) {
            Log.d(TAG, "hanshake");
            derPayloadStartIndex = 6+commandApdu[4];
            derRequestPayload = new byte[0];
            derResponsePayload = new byte[0];
            stepCounter = 0;
            clientPublicKey = null;
            isProcessing = false;

            byte[] aid = Arrays.copyOfRange(commandApdu, 5, derPayloadStartIndex-1);
            if (Arrays.equals(aid, AID_ANDROID)) {
                this.maxFragmentSize = 261;
            } else if (Arrays.equals(aid, AID_ANDROID_ACS)) {
                this.maxFragmentSize = 53;
            }
        }

        if (hasNextFragment()) {
            return getNextFragment();
        } else {
            final byte[] payload = Arrays.copyOfRange(commandApdu, derPayloadStartIndex, commandApdu.length);
            if (!Arrays.equals(payload, KEEPALIVE)) {
                derRequestPayload = Utils.concatBytes(derRequestPayload, payload);
                int responseLength = DERParser.extractPayloadEndIndex(derRequestPayload);
                Log.d(TAG, "expecting response length:" + responseLength);
                Log.d(TAG, "actual response length:" + derRequestPayload.length);

                if (derRequestPayload.length >= responseLength && !isProcessing) { // we have an unprocessed request
                    isProcessing = true;
                    derResponsePayload = new byte[0];

                    final Thread processingThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final byte[] requestPayload = derRequestPayload;
                            derRequestPayload = new byte[0];

                            switch (stepCounter) {
                                case 0:
                                    PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(bitcoinURI);
                                    derResponsePayload = paymentRequestSendStep.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                    stepCounter++;
                                    break;
                                case 1:
                                    PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(bitcoinURI);
                                    derResponsePayload = paymentAuthorizationReceiveStep.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                    clientPublicKey = paymentAuthorizationReceiveStep.getClientPublicKey();
                                    stepCounter++;
                                    break;
                                case 2:
                                    final PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(clientPublicKey);
                                    derResponsePayload = paymentRefundReceiveStep.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                    stepCounter++;
                                    break;
                                case 3:
                                    final PaymentFinalSignatureReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureReceiveStep(clientPublicKey, bitcoinURI.getAddress());
                                    derResponsePayload = paymentFinalSignatureReceiveStep.process(DERParser.parseDER(requestPayload)).serializeToDER();
                                    LocalBroadcastManager.getInstance(NFCService.this).sendBroadcast(new Intent(Constants.INSTANT_PAYMENT_SUCCESSFUL_ACTION));
                                    stepCounter++;
                                    break;
                            }
                        }
                    });
                    processingThread.start();
                }
            }
            return KEEPALIVE;
        }
    }

    private boolean hasNextFragment() {
        return derResponsePayload.length > 0;
    }

    private byte[] getNextFragment() {
        byte[] fragment = Arrays.copyOfRange(derResponsePayload, 0, Math.min(derResponsePayload.length, maxFragmentSize));
        derResponsePayload = Arrays.copyOfRange(derResponsePayload, fragment.length, derResponsePayload.length);
        if (derResponsePayload.length == 0) {
            isProcessing = false;
        }
        Log.d(TAG,"sending next fragment:"+fragment.length);
        return fragment;
    }

    @Override
    public void onDeactivated(int reason) {

    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte) 0 && apdu[1] == (byte) 0xa4;
    }
}
