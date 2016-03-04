package ch.papers.payments.communications.peers.nfc;

import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERParser;
import ch.papers.payments.communications.peers.steps.PaymentAuthorizationReceiveStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestSendStep;
import ch.papers.payments.communications.peers.steps.Step;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class NFCService extends HostApduService {
    private final static String TAG = NFCService.class.getSimpleName();

    private final List<Step> stepList = new ArrayList<Step>();
    private int stepCounter = 0;
    private boolean isProcessing = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stepList.clear();

        // Check if intent has extras
        if(intent.getExtras() != null){
            try {
                String bitcoinUri = intent.getExtras().getString(Constants.BITCOIN_URI_KEY);
                final BitcoinURI bitcoinURI = new BitcoinURI(bitcoinUri);
                stepList.add(new PaymentRequestSendStep(bitcoinURI));
                stepList.add(new PaymentAuthorizationReceiveStep(bitcoinURI));


            } catch (BitcoinURIParseException e) {
                e.printStackTrace();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    byte[] derRequestPayload = new byte[0];
    byte[] derResponsePayload = new byte[0];
    boolean isSending = false;

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.d(TAG,"this is command apdu lenght: "+commandApdu.length);
        int derPayloadStartIndex = 0;
        if (this.selectAidApdu(commandApdu)) {
            Log.d(TAG, "hanshake");
            stepCounter = 0;
            derPayloadStartIndex = 2;
            derRequestPayload = new byte[0];
        }

        byte[] payload = Arrays.copyOfRange(commandApdu,derPayloadStartIndex,commandApdu.length);
        derRequestPayload = Utils.concatBytes(derRequestPayload,payload);

        int responseLength = DERParser.extractPayloadEndIndex(derRequestPayload);

        if(derRequestPayload.length<responseLength){
            return DERObject.NULLOBJECT.serializeToDER();
        } else {
            if(!isProcessing && !isSending) {
                final Thread processingThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isProcessing = true;
                        derResponsePayload = stepList.get(stepCounter++).process(DERParser.parseDER(derRequestPayload)).serializeToDER();
                        derRequestPayload = new byte[0];
                        isSending = true;
                        isProcessing = false;

                    }
                });
                processingThread.start();
                try {
                    processingThread.join(100);
                } catch (InterruptedException e) {
                    Log.d(TAG, "missed this timeout, maybe next time");
                }
            }

            if(this.isProcessing){
                Log.d(TAG,"Ah, ah, ah, ahh, staying alive, staying alive...");
                return DERObject.NULLOBJECT.serializeToDER();
            } else {
                Log.d(TAG,"replying with response exp:"+DERParser.extractPayloadEndIndex(derResponsePayload));
                Log.d(TAG,"replying with response:"+derResponsePayload.length);
                isSending = false;
                return derResponsePayload;
            }
        }
    }

    @Override
    public void onDeactivated(int reason) {

    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte) 0 && apdu[1] == (byte) 0xa4;
    }
}
