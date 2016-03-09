package ch.papers.payments.communications.peers.handlers;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

import ch.papers.payments.WalletService;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.peers.PaymentRequestAuthorizer;
import ch.papers.payments.communications.peers.steps.PaymentFinalSignatureSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRefundSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestReceiveStep;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class InstantPaymentClientHandler extends DERObjectStreamHandler{
    private final static String TAG = InstantPaymentClientHandler.class.getSimpleName();
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private final PaymentRequestAuthorizer paymentRequestAuthorizer;

    public InstantPaymentClientHandler(InputStream inputStream, OutputStream outputStream, WalletService.WalletServiceBinder walletServiceBinder, PaymentRequestAuthorizer paymentRequestAuthorizer) {
        super(inputStream, outputStream);
        this.paymentRequestAuthorizer = paymentRequestAuthorizer;
        this.walletServiceBinder = walletServiceBinder;
    }

    @Override
    public void run() {
        Log.d(TAG,"kick off");
        try {
            writeDERObject(DERObject.NULLOBJECT); // we kick off the process
            PaymentRequestReceiveStep paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder.getMultisigClientKey(),walletServiceBinder.getUnspentInstantOutputs());
            final DERObject paymentRequestResponse = paymentRequestReceiveStep.process(readDERObject());
            Log.d(TAG, "got request, authorizing user");
            if (paymentRequestAuthorizer.isPaymentRequestAuthorized(paymentRequestReceiveStep.getBitcoinURI())) {
                writeDERObject(paymentRequestResponse);
                final PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(this.walletServiceBinder, paymentRequestReceiveStep.getBitcoinURI());
                writeDERObject(paymentRefundSendStep.process(readDERObject()));
                final PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(walletServiceBinder.getMultisigClientKey(), paymentRequestReceiveStep.getBitcoinURI().getAddress(), paymentRefundSendStep.getFullSignedTransaction());
                writeDERObject(paymentFinalSignatureSendStep.process(readDERObject()));
                Log.d(TAG, "payment was successful, we're done!");
                paymentRequestAuthorizer.onPaymentSuccess();
            }
        } catch (Exception e){
            paymentRequestAuthorizer.onPaymentError(e.getMessage());
        }
    }
}
