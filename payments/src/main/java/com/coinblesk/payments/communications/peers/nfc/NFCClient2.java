package com.coinblesk.payments.communications.peers.nfc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERParser;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.peers.steps.PaymentFinalSignatureReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRequestSendStep;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import com.coinblesk.payments.Utils;
import com.coinblesk.payments.communications.peers.steps.PaymentAuthorizationReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRefundReceiveStep;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCClient2 extends AbstractServer {
    private final static String TAG = NFCClient2.class.getSimpleName();

    private static final byte[] CLA_INS_P1_P2 = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};
    private static final byte[] AID_ANDROID = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x03};
    private static final byte[] KEEPALIVE = {1,2,3,4};


    private final Activity activity;
    private final NfcAdapter nfcAdapter;


    private int stepCounter = 0;
    private BitcoinURI bitcoinURI;
    private byte[] derRequestPayload = new byte[0];
    private byte[] derResponsePayload = new byte[0];
    private ECKey clientPublicKey;

    public NFCClient2(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, walletServiceBinder);
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.activity);
        this.bitcoinURI = getPaymentRequestUri();
    }

    @Override
    public boolean isSupported() {
        return (this.nfcAdapter != null);
    }

    @Override
    public void start() {
        if (!nfcAdapter.isEnabled()) {
            this.activity.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }


        this.setRunning(true);
    }

    @Override
    public void stop() {
        nfcAdapter.disableReaderMode(this.activity);
        this.setRunning(false);
    }

    private byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte) aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }

    public void bitcoinURI(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }





    @Override
    public void onChangePaymentRequest() {

        Log.d(TAG, "enable reader for NFC");
        nfcAdapter.enableReaderMode(this.activity, new NfcAdapter.ReaderCallback() {
            @Override
            public void onTagDiscovered(Tag tag) {
                try {
                    IsoDep isoDep = IsoDep.get(tag);
                    isoDep.connect();
                    boolean done = false;

                    Log.d(TAG, "first transmit, payment request");

                    final AtomicReference<DERObject> refundSendInput = new AtomicReference<DERObject>();
                    final AtomicReference<DERObject> finalSendInput = new AtomicReference<DERObject>();

                    final AtomicReference<DERObject> paymentRequestSendStepOutput = new AtomicReference<DERObject>();
                    final AtomicReference<DERObject> paymentAuthorizationReceiveOutput = new AtomicReference<DERObject>();
                    final AtomicReference<DERObject> paymentRefundSendStep = new AtomicReference<DERObject>();
                    final AtomicReference<DERObject> sendFinalSignatureOutput = new AtomicReference<DERObject>();


                    //final AtomicReference<Transaction> tx = new AtomicReference<Transaction>();
                    //final AtomicReference<Transaction> refund = new AtomicReference<Transaction>();


                    Thread authorization = null;
                    Thread refundSend = null;
                    Thread finalSend = null;

                    PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(bitcoinURI);
                    paymentRequestSendStepOutput.set(paymentRequestSendStep.process(null));
                    Log.d(TAG, "got request, received tx, reply with signatures for tx");
                    final DERObject paymentRequestOutput = transceiveDER(isoDep, paymentRequestSendStepOutput.get(), true);


                    while (!done) {
                        if (paymentAuthorizationReceiveOutput.get() == null && paymentRequestOutput != null && authorization == null) {
                            authorization = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "payment details send, sign tx");
                                    //now we have the tx, that we need to sign, send back ok
                                    PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(bitcoinURI);
                                    paymentAuthorizationReceiveOutput.set(paymentAuthorizationReceiveStep.process(paymentRequestOutput));
                                    clientPublicKey = paymentAuthorizationReceiveStep.getClientPublicKey();
                                }
                            });
                            authorization.start();
                        }


                        if (paymentAuthorizationReceiveOutput.get() != null && refundSendInput.get() == null) {
                            Log.d(TAG, "got request, received refund tx, reply with siganture for tx");
                            refundSendInput.set(transceiveDER(isoDep, paymentAuthorizationReceiveOutput.get()));
                        }


                        if (refundSendInput.get() != null && paymentRefundSendStep.get() == null
                                && refundSend == null) {
                            refundSend = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "got request, sign refund");
                                    final PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(clientPublicKey);
                                    paymentRefundSendStep.set(paymentRefundReceiveStep.process(refundSendInput.get()));
                                }
                            });
                            refundSend.start();
                        }

                        if (paymentRefundSendStep.get() != null && finalSendInput.get() == null) {
                            Log.d(TAG, "got request, received full tx, we are almost done");
                            finalSendInput.set(transceiveDER(isoDep, paymentRefundSendStep.get()));
                        }

                        if (finalSendInput.get() != null && sendFinalSignatureOutput.get() == null && finalSend == null) {
                            finalSend = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d(TAG, "final request");
                                    final PaymentFinalSignatureReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureReceiveStep(
                                            clientPublicKey, bitcoinURI.getAddress());
                                    sendFinalSignatureOutput.set(paymentFinalSignatureReceiveStep.process(finalSendInput.get()));
                                }
                            });
                            finalSend.start();
                        }

                        if (sendFinalSignatureOutput.get() != null) {
                            Log.d(TAG, "got final request, send over NFC ack");
                            DERObject response = transceiveDER(isoDep, sendFinalSignatureOutput.get());
                            Log.d(TAG, "we are done, response is: " + response.getPayload().length);
                            done = true;
                        } else {
                            byte[] response = isoDep.transceive(KEEPALIVE);
                            Log.d(TAG, "keep alive:" + response.length);
                        }
                    }

                    getPaymentRequestAuthorizer().onPaymentSuccess();
                    isoDep.close();
                } catch (Throwable e) {
                    Log.e(TAG, "error", e);
                    e.printStackTrace();
                    getPaymentRequestAuthorizer().onPaymentError(e.getMessage());
                }
            }

            public DERObject transceiveDER(IsoDep isoDep, DERObject input, boolean needsSelectAidApdu) throws IOException, InterruptedException {

                Log.d(TAG, "start transceive. ");
                byte[] derPayload = input.serializeToDER();
                byte[] derResponse = new byte[0];
                int fragmentByte = 0;

                Log.d(TAG, "have to send bytes:" + derPayload.length);
                while (fragmentByte < derPayload.length) {
                    byte[] fragment = new byte[0];
                    if (needsSelectAidApdu) {
                        isoDep.transceive(createSelectAidApdu(AID_ANDROID));
                        needsSelectAidApdu = false;
                    }

                    fragment = Utils.concatBytes(fragment, Arrays.copyOfRange(derPayload, fragmentByte, Math.min(derPayload.length, fragmentByte + 245)));

                    Log.d(TAG, "about to send fragment size:" + fragment.length);
                    derResponse = isoDep.transceive(fragment);
                    Log.d(TAG, "my client received payload" + Arrays.toString(derResponse));
                    fragmentByte += fragment.length;
                }

                while (Arrays.equals(derResponse, KEEPALIVE)) {
                    Log.d(TAG, "keep alive...");
                    derResponse = isoDep.transceive(KEEPALIVE);
                    Log.d(TAG, "keep alive done, got: " + derResponse.length);
                }

                int responseLength = DERParser.extractPayloadEndIndex(derResponse);
                Log.d(TAG, "expected response length:" + responseLength + ", actual length: " + derResponse.length);

                while (derResponse.length < responseLength) {
                    derResponse = Utils.concatBytes(derResponse, isoDep.transceive(KEEPALIVE));
                    Log.d(TAG, "had to ask for next bytes:" + derResponse.length);
                }
                Log.d(TAG, "end transceive");
                return DERParser.parseDER(derResponse);

            }

            private DERObject transceiveDER(IsoDep isoDep, DERObject input) throws IOException, InterruptedException {
                return this.transceiveDER(isoDep, input, false);
            }
        }, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, Bundle.EMPTY);

    }
}