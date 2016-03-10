package ch.papers.payments.communications.peers.nfc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

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
        if(this.isReadyForInstantPayment()) {
            nfcAdapter.enableReaderMode(this.activity, new NfcAdapter.ReaderCallback() {
                        @Override
                        public void onTagDiscovered(Tag tag) {
                            try {
                                IsoDep isoDep = IsoDep.get(tag);
                                isoDep.connect();
                                isoDep.setTimeout(5000);


                                DERObject paymentRequestInput = transceiveDER(isoDep, DERObject.NULLOBJECT, true);
                                DERObject authorizationResponseOutput = paymentRequestReceiveStep.process(paymentRequestInput);

                                Log.d(TAG, "got request, authorizing user");
                                //if (getPaymentRequestAuthorizer().isPaymentRequestAuthorized(paymentRequestReceiveStep.getBitcoinURI())) {
                                    DERObject refundSendInput = transceiveDER(isoDep, authorizationResponseOutput);
                                    PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(getWalletServiceBinder(), paymentRequestReceiveStep.getBitcoinURI(), paymentRequestReceiveStep.getTimestamp());
                                    DERObject refundSendOutput = paymentRefundSendStep.process(refundSendInput);


                                    DERObject finalSendInput = transceiveDER(isoDep, refundSendOutput);
                                    PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(getWalletServiceBinder(), paymentRequestReceiveStep.getBitcoinURI().getAddress(), paymentRefundSendStep.getFullSignedTransaction(), paymentRefundSendStep.getHalfSignedRefundTransaction());
                                    DERObject sendFinalSignatureOutput = paymentFinalSignatureSendStep.process(finalSendInput);

                                    transceiveDER(isoDep, sendFinalSignatureOutput);
                                    getPaymentRequestAuthorizer().onPaymentSuccess();
                                //} else {
                                //    getPaymentRequestAuthorizer().onPaymentError("payment not authorized");
                                //}

                                isoDep.close();
                            } catch (IOException e) {
                                getPaymentRequestAuthorizer().onPaymentError(e.getMessage());
                            }
                        }

                        public DERObject transceiveDER(IsoDep isoDep, DERObject input, boolean needsSelectAidApdu) throws IOException {
                            Log.d(TAG, "start transceive");
                            byte[] derPayload = input.serializeToDER();
                            byte[] derResponse = new byte[0];
                            int fragmentByte = 0;

                            Log.d(TAG, "have to send bytes:" + derPayload.length);
                            while (fragmentByte < derPayload.length) {
                                byte[] fragment = new byte[0];
                                if (needsSelectAidApdu) {
                                    fragment = createSelectAidApdu(AID_ANDROID);
                                }

                                fragment = Utils.concatBytes(fragment, Arrays.copyOfRange(derPayload, fragmentByte, Math.min(derPayload.length, fragmentByte + isoDep.getMaxTransceiveLength())));
                                derResponse = Utils.concatBytes(derResponse, isoDep.transceive(fragment));
                                fragmentByte += fragment.length;
                                Log.d(TAG, "fragment size:" + fragment.length);
                            }

                            int responseLength = DERParser.extractPayloadEndIndex(derResponse);
                            Log.d(TAG, "expected response lenght:" + responseLength);
                            Log.d(TAG, "actual response lenght:" + derResponse.length);

                            while (derResponse.length < responseLength) {
                                derResponse = Utils.concatBytes(derResponse, isoDep.transceive(DERObject.NULLOBJECT.serializeToDER()));
                                Log.d(TAG, "had to ask for next bytes:" + derResponse.length);
                            }
                            Log.d(TAG, "end transceive");
                            return DERParser.parseDER(derResponse);
                        }

                        private DERObject transceiveDER(IsoDep isoDep, DERObject input) throws IOException {
                            return this.transceiveDER(isoDep, input, false);
                        }
                    }, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null);
        } else {
            nfcAdapter.disableReaderMode(this.activity);
        }
    }
}
