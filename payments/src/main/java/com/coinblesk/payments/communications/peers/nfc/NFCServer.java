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

import com.coinblesk.json.TxSig;
import com.coinblesk.payments.Utils;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERParser;
import com.coinblesk.payments.communications.peers.AbstractServer;
import com.coinblesk.payments.communications.peers.steps.PaymentAuthorizationReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentFinalSignatureOutpointsReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRefundReceiveStep;
import com.coinblesk.payments.communications.peers.steps.PaymentRequestSendStep;

import org.bitcoinj.core.ECKey;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCServer extends AbstractServer {
    private final static String TAG = NFCServer.class.getSimpleName();

    private static final byte[] CLA_INS_P1_P2 = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};
    private static final byte[] AID_ANDROID = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x03};
    private static final byte[] KEEPALIVE = {1, 2, 3, 4};


    private final Activity activity;
    private final NfcAdapter nfcAdapter;

    private ECKey clientPublicKey;
    private final List<TxSig> serverSignatures = new ArrayList<TxSig>();

    public NFCServer(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, walletServiceBinder);
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.activity);
    }

    @Override
    public boolean isSupported() {
        return (this.nfcAdapter != null);
    }

    @Override
    public void onStart() {
        if (!nfcAdapter.isEnabled()) {
            this.activity.startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }

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

                    Thread authorization = null;
                    Thread refundSend = null;
                    Thread finalSend = null;

                    PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(getPaymentRequestUri());
                    paymentRequestSendStepOutput.set(paymentRequestSendStep.process(null));
                    Log.d(TAG, "got request, received tx, reply with signatures for tx");
                    final DERObject paymentRequestOutput = transceiveDER(isoDep, paymentRequestSendStepOutput.get(), true);


                    while (!done) {
                        if (paymentAuthorizationReceiveOutput.get() == null && paymentRequestOutput != null && authorization == null) {
                            authorization = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Log.d(TAG, "payment details send, sign tx");
                                        //now we have the tx, that we need to sign, send back ok
                                        PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(getPaymentRequestUri());
                                        paymentAuthorizationReceiveOutput.set(paymentAuthorizationReceiveStep.process(paymentRequestOutput));
                                        clientPublicKey = paymentAuthorizationReceiveStep.getClientPublicKey();
                                        serverSignatures.addAll(paymentAuthorizationReceiveStep.getServerSignatures());
                                    } catch (Exception e) {
                                        Log.w(TAG, "Exception at authorization step: ", e);
                                    }
                                }
                            }, "NFCServer.Authorization");
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
                                    try {
                                        Log.d(TAG, "got request, sign refund");
                                        final PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(clientPublicKey);
                                        paymentRefundSendStep.set(paymentRefundReceiveStep.process(refundSendInput.get()));
                                    } catch (Exception e) {
                                        Log.w(TAG, "Exception at refund step: ", e);
                                    }
                                }
                            }, "NFCServer.Refund");
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
                                    try {
                                        Log.d(TAG, "final request");
                                        final PaymentFinalSignatureOutpointsReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureOutpointsReceiveStep(
                                                clientPublicKey, serverSignatures, getPaymentRequestUri());
                                        getWalletServiceBinder().commitAndBroadcastTransaction(paymentFinalSignatureReceiveStep.getFullSignedTransaction());
                                        sendFinalSignatureOutput.set(paymentFinalSignatureReceiveStep.process(finalSendInput.get()));

                                    } catch (Exception e) {
                                        Log.w(TAG, "Exception at signature step: ", e);
                                    }
                                }
                            }, "NFCServer.Sign");
                            finalSend.start();
                        }

                        if (sendFinalSignatureOutput.get() != null) {
                            Log.d(TAG, "got final request, send over NFC ack");
                            isoDep.transceive(sendFinalSignatureOutput.get().serializeToDER());
                            done = true;
                        } else {
                            byte[] response = isoDep.transceive(KEEPALIVE);
                            Log.d(TAG, "keep alive:" + response.length);
                        }
                    }

                    getPaymentRequestDelegate().onPaymentSuccess();
                    isoDep.close();
                } catch (Throwable e) {
                    Log.e(TAG, "Exception in onTagDiscovered", e);
                    getPaymentRequestDelegate().onPaymentError(e.getMessage());
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

    @Override
    public void onStop() {
        nfcAdapter.disableReaderMode(this.activity);
    }

    private byte[] createSelectAidApdu(byte[] aid) {
        byte[] result = new byte[6 + aid.length];
        System.arraycopy(CLA_INS_P1_P2, 0, result, 0, CLA_INS_P1_P2.length);
        result[4] = (byte) aid.length;
        System.arraycopy(aid, 0, result, 5, aid.length);
        result[result.length - 1] = 0;
        return result;
    }
}