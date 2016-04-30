package com.coinblesk.payments.communications.peers.steps.cltv;

import android.util.Log;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.peers.steps.AbstractStep;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;


public class CLTVInstantPaymentStep extends AbstractStep {
    private static final String TAG = CLTVInstantPaymentStep.class.getName();

    private final WalletService.WalletServiceBinder walletServiceBinder;
    private Transaction transaction;

    public CLTVInstantPaymentStep(WalletService.WalletServiceBinder walletServiceBinder, BitcoinURI paymentRequest) {
        super(paymentRequest);
        this.walletServiceBinder = walletServiceBinder;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public DERObject process(DERObject input) {
        try {
            /* Payment Request */
            DERObject output = DERObject.NULLOBJECT;
            PaymentRequestSendStep sendRequest = new PaymentRequestSendStep(getBitcoinURI());
            output = sendRequest.process(output);
            PaymentRequestReceiveStep receiveRequest = new PaymentRequestReceiveStep();
            output = receiveRequest.process(output);

            /* Payment Response */
            PaymentResponseSendStep sendResponse = new PaymentResponseSendStep(receiveRequest.getBitcoinURI(), walletServiceBinder);
            output = sendResponse.process(output);
            PaymentResponseReceiveStep receiveResponse = new PaymentResponseReceiveStep(getBitcoinURI());
            output = receiveResponse.process(output);

            /* Server Signatures */
            // PaymentServerSignatureSendStep sendSignatures = new PaymentServerSignatureSendStep();
            PaymentServerSignatureReceiveStep receiveSignatures = new PaymentServerSignatureReceiveStep();
            output = receiveSignatures.process(output);

            /* Payment Finalize */
            PaymentFinalizeStep finalizeStep = new PaymentFinalizeStep(
                    receiveRequest.getBitcoinURI(),
                    sendResponse.getTransaction(),
                    sendResponse.getClientTransactionSignatures(),
                    receiveSignatures.getServerTransactionSignatures(),
                    walletServiceBinder);
            output = finalizeStep.process(output);

            transaction = finalizeStep.getTransaction();
            setSuccess();
        } catch (Exception e) {
            Log.w(TAG, "Exception: ", e);
            setError();
        }
        return null;
    }
}
