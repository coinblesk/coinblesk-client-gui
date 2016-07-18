package com.coinblesk.payments.communications.peers.nfc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERParser;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.steps.cltv.PaymentRequestSendStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentResponseReceiveCompactStep;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCServerCLTV extends AbstractServer {
    private final static String TAG = NFCServerCLTV.class.getName();

    private final Activity activity;
    private final NfcAdapter nfcAdapter;

    public NFCServerCLTV(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, walletServiceBinder);
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    }

    @Override
    public boolean isSupported() {
        return (nfcAdapter != null);
    }

    @Override
    public void onStart() {
        if (!nfcAdapter.isEnabled()) {
            activity.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }

        Log.d(TAG, "onStart - enable reader for NFC");
        nfcAdapter.enableReaderMode(
                activity,
                new NFCPaymentCallback(),
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                Bundle.EMPTY);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop - disable NFC reader mode.");
        nfcAdapter.disableReaderMode(activity);
    }

    private DERObject transceiveDER(IsoDep isoDep, DERObject input, boolean needsSelectAidApdu) throws IOException {
        final long startTimeTransceive = System.currentTimeMillis();
        final byte[] derPayload = input.serializeToDER();
        byte[] derResponse;
        Log.d(TAG, "transceiveDER - start - send derPayload length=" + derPayload.length + " byte");

        derResponse = assembleFragment(isoDep, derPayload, needsSelectAidApdu);
        derResponse = keepAliveLoop(isoDep, derResponse);
        derResponse = concatNextResponseBytes(isoDep, derResponse);
        DERObject result = DERParser.parseDER(derResponse);

        long duration = System.currentTimeMillis() - startTimeTransceive;
        Log.d(TAG, "transceiveDER - end - " +
                            "took " + duration + " ms - " +
                            "received length=" + result.getPayload().length + " byte");
        return result;
    }

    private byte[] assembleFragment(IsoDep isoDep, byte[] derPayload, boolean needsSelectAidApdu) throws IOException {
        int fragmentByte = 0;
        byte[] derResponse = new byte[0];
        while (fragmentByte < derPayload.length) {
            byte[] fragment = new byte[0];
            if (needsSelectAidApdu) {
                Log.d(TAG, "transceiveDER - select Aid Apdu");
                isoDep.transceive(NFCUtils.createSelectAidApdu(NFCUtils.AID_ANDROID));
                needsSelectAidApdu = false;
            }

            int endLength = Math.min(derPayload.length, fragmentByte + NFCUtils.DEFAULT_MAX_FRAGMENT_SIZE);
            byte[] fragmentPart = Arrays.copyOfRange(derPayload, fragmentByte, endLength);
            fragment = ClientUtils.concatBytes(fragment, fragmentPart);

            Log.d(TAG, "transceiveDER - about to send fragment size: " + fragment.length);
            derResponse = isoDep.transceive(fragment);
            Log.d(TAG, "transceiveDER - received payload: " + Arrays.toString(derResponse));
            fragmentByte += fragment.length;
        }
        return derResponse;
    }

    private byte[] concatNextResponseBytes(IsoDep isoDep, byte[] derResponse) throws IOException {
        int responseLength = DERParser.extractPayloadEndIndex(derResponse);
        Log.d(TAG, "transceiveDER - expected response length: " + responseLength + ", actual length: " + derResponse.length);
        while (derResponse.length < responseLength) {
            derResponse = ClientUtils.concatBytes(derResponse, transceiveKeepAlive(isoDep));
            Log.d(TAG, "transceiveDER - had to ask for next bytes, length=" + derResponse.length);
        }
        return derResponse;
    }

    private byte[] keepAliveLoop(IsoDep isoDep, byte[] derResponse) throws IOException {
        while (NFCUtils.isKeepAlive(derResponse)) {
            Log.d(TAG, "transceiveDER - keep alive...");
            derResponse = transceiveKeepAlive(isoDep);
            Log.d(TAG, "transceiveDER - keep alive done, got response, length=" + derResponse.length);
        }
        return derResponse;
    }

    private byte[] transceiveKeepAlive(IsoDep isoDep) throws IOException {
        return isoDep.transceive(NFCUtils.KEEPALIVE);
    }

    private DERObject transceiveDER(IsoDep isoDep, DERObject input) throws IOException {
        return transceiveDER(isoDep, input, false);
    }

    private class NFCPaymentCallback implements NfcAdapter.ReaderCallback {
        private final String TAG = NFCPaymentCallback.class.getName();

        @Override
        public void onTagDiscovered(Tag tag) {
            Log.d(TAG, "onTagDiscovered: " + tag);

            final long startTime = System.currentTimeMillis();
            long duration;

            try {
                IsoDep isoDep = IsoDep.get(tag);
                isoDep.connect();
                boolean done = false;
                final AtomicReference<DERObject> outputPaymentRequestSend = new AtomicReference<>();
                final AtomicReference<DERObject> outputPaymentResponseReceive = new AtomicReference<>();
                final AtomicReference<DERObject> paymentAck = new AtomicReference<>();
                Thread authorization = null;

                Log.d(TAG, "first transmit: payment request, startTime(ms)=" + startTime);
                PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(
                        getPaymentRequestUri(),
                        getWalletServiceBinder().getMultisigClientKey());
                DERObject derPaymentRequest = paymentRequestSendStep.process(DERObject.NULLOBJECT);
                outputPaymentRequestSend.set(derPaymentRequest);
                Log.d(TAG, "transceive outputPaymentRequestSend");
                final DERObject paymentRequestOutput = transceiveDER(isoDep, outputPaymentRequestSend.get(), true);

                while (!done) {
                    if (paymentRequestOutput != null && authorization == null && outputPaymentResponseReceive.get() == null) {
                        duration = System.currentTimeMillis() - startTime;
                        Log.d(TAG, "got payment response ("+duration+" ms since startTime)");
                        Runnable runnable = new PaymentResponseReceiveRunnable(paymentRequestOutput, outputPaymentResponseReceive);
                        authorization = new Thread(runnable, "NFCServer.PaymentResponseReceive");
                        authorization.start();
                        continue;
                    }

                    if (outputPaymentResponseReceive.get() != null && paymentAck.get() == null) {
                        duration = System.currentTimeMillis() - startTime;
                        Log.d(TAG, "send server response ("+duration+" ms since startTime)");
                        DERObject toSend = outputPaymentResponseReceive.get();
                        DERObject ackResponse = transceiveDER(isoDep, toSend);
                        paymentAck.set(ackResponse);
                        continue;
                    }

                    if (paymentAck.get() != null) {
                        duration = System.currentTimeMillis() - startTime;
                        Log.d(TAG, "Send final ACK ("+duration+" ms since startTime)");
                        isoDep.transceive(DERObject.NULLOBJECT.serializeToDER());
                        done = true;
                        continue;
                    }

                    // default: keep alive loop
                    byte[] response = transceiveKeepAlive(isoDep);
                    Log.d(TAG, "transceive keep alive, response length=" + response.length);
                }

                getPaymentRequestDelegate().onPaymentSuccess();
                isoDep.close();
                duration = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Payment finished - total duration: " + duration + " ms");
                
            } catch (TagLostException tle) {
                Log.d(TAG, "Tag lost");
            } catch (Throwable e) {
                Log.e(TAG, "Exception in onTagDiscovered", e);
                getPaymentRequestDelegate().onPaymentError(e.getMessage());
            }
        }
    }

    private class PaymentResponseReceiveRunnable implements Runnable {
        private final DERObject input;
        private final AtomicReference<DERObject> output;

        PaymentResponseReceiveRunnable(DERObject input, AtomicReference<DERObject> output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "PaymentResponseReceiveCompactStep - payment details send, sign tx");
                PaymentResponseReceiveCompactStep responseReceive = new PaymentResponseReceiveCompactStep(
                        getPaymentRequestUri(), getWalletServiceBinder());
                DERObject result = responseReceive.process(input);
                output.set(result);
            } catch (Exception e) {
                Log.w(TAG, "Exception at PaymentResponseReceiveCompactStep: ", e);
            }
        }
    }
}