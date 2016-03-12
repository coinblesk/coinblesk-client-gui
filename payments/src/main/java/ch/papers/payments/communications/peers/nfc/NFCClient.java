package ch.papers.payments.communications.peers.nfc;

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

import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ch.papers.payments.Utils;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERParser;
import ch.papers.payments.communications.peers.AbstractClient;
import ch.papers.payments.communications.peers.steps.PaymentFinalSignatureSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRefundSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestReceiveStep;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */

@TargetApi(Build.VERSION_CODES.KITKAT)
public class NFCClient extends AbstractClient {
    private final static String TAG = NFCClient.class.getSimpleName();

    private static final byte[] CLA_INS_P1_P2 = {(byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00};
    private static final byte[] AID_ANDROID = {(byte) 0xF0, 0x0C, 0x01, 0x04, 0x0B, 0x01, 0x03};
    private static final byte[] KEEPALIVE = {1,2,3,4};


    private final Activity activity;
    private final NfcAdapter nfcAdapter;

    private final PaymentRequestReceiveStep paymentRequestReceiveStep;

    public NFCClient(Activity activity, WalletService.WalletServiceBinder walletServiceBinder) {
        super(activity, walletServiceBinder);
        this.activity = activity;
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this.activity);
        this.paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);

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

    @Override
    public void onIsReadyForInstantPaymentChange() {

        if (this.isReadyForInstantPayment()) {
            Log.d(TAG, "enable reader for NFC");
            nfcAdapter.enableReaderMode(this.activity, new NfcAdapter.ReaderCallback() {
                        @Override
                        public void onTagDiscovered(Tag tag) {
                            try {
                                IsoDep isoDep = IsoDep.get(tag);
                                isoDep.connect();
                                boolean done = false;

                                Log.d(TAG, "first transmit, payment request");
                                final DERObject paymentRequestInput = transceiveDER(isoDep, DERObject.NULLOBJECT, true);
                                final AtomicReference<DERObject> refundSendInput = new AtomicReference<DERObject>();
                                final AtomicReference<DERObject> finalSendInput = new AtomicReference<DERObject>();

                                final AtomicReference<DERObject> authorizationResponseOutput = new AtomicReference<DERObject>();
                                final AtomicReference<DERObject> paymentRefundSendStep = new AtomicReference<DERObject>();
                                final AtomicReference<DERObject> sendFinalSignatureOutput = new AtomicReference<DERObject>();

                                final AtomicReference<Transaction> tx = new AtomicReference<Transaction>();
                                final AtomicReference<Transaction> refund = new AtomicReference<Transaction>();

                                final AtomicReference<BitcoinURI> bitcoinURI = new AtomicReference<BitcoinURI>();
                                final AtomicLong timestamp = new AtomicLong(-1);

                                Thread authorization = null;
                                Thread refundSend = null;
                                Thread finalSend = null;


                                while (!done) {
                                    if (authorizationResponseOutput.get() == null && authorization == null) {
                                        authorization = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d(TAG, "payment details request");
                                                authorizationResponseOutput.set(paymentRequestReceiveStep.process(paymentRequestInput));
                                                bitcoinURI.set(paymentRequestReceiveStep.getBitcoinURI());
                                                timestamp.set(paymentRequestReceiveStep.getTimestamp());
                                            }
                                        });
                                        authorization.start();
                                    }


                                    if (authorizationResponseOutput.get() != null && refundSendInput.get() == null) {
                                        Log.d(TAG, "got request, authorizing user");
                                        refundSendInput.set(transceiveDER(isoDep, authorizationResponseOutput.get()));
                                    }



                                    if (refundSendInput.get() != null && paymentRefundSendStep.get() == null
                                            && bitcoinURI!=null && timestamp.get()>=0 && refundSend == null) {
                                        refundSend = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d(TAG, "refund request");
                                                final PaymentRefundSendStep paymentRefundSendStep1 = new PaymentRefundSendStep(getWalletServiceBinder(),
                                                        bitcoinURI.get(), timestamp.get());
                                                paymentRefundSendStep.set(paymentRefundSendStep1.process(refundSendInput.get()));
                                                tx.set(paymentRefundSendStep1.getFullSignedTransaction());
                                                refund.set(paymentRefundSendStep1.getHalfSignedRefundTransaction());
                                            }
                                        });
                                        refundSend.start();
                                    }

                                    if (paymentRefundSendStep.get() != null && finalSendInput.get() == null) {
                                        Log.d(TAG, "got request refund, send over NFC");
                                        finalSendInput.set(transceiveDER(isoDep, paymentRefundSendStep.get()));
                                    }

                                    if (finalSendInput.get() != null && tx.get()!=null && refund.get()!=null
                                            && sendFinalSignatureOutput.get() == null && finalSend == null) {
                                        finalSend = new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Log.d(TAG, "final request");
                                                PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(getWalletServiceBinder(),
                                                        paymentRequestReceiveStep.getBitcoinURI().getAddress(), tx.get(), refund.get());
                                                sendFinalSignatureOutput.set(paymentFinalSignatureSendStep.process(finalSendInput.get()));
                                            }
                                        });
                                        finalSend.start();
                                    }

                                    if (sendFinalSignatureOutput.get() != null) {
                                        Log.d(TAG, "got final request, send over NFC");
                                        DERObject response = transceiveDER(isoDep, sendFinalSignatureOutput.get());
                                        Log.d(TAG, "we are done, response is: " + response.getPayload().length);
                                        done = true;
                                    } else {
                                        byte[] response = isoDep.transceive(KEEPALIVE);
                                        Log.d(TAG, "keep alive:"+response.length);
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
                                Log.d(TAG, "expected response length:" + responseLength+", actual length: "+ derResponse.length);

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
        } else {
            Log.d(TAG, "disable reader");
            nfcAdapter.disableReaderMode(this.activity);
        }
    }
}
