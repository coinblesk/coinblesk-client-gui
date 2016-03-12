package ch.papers.payments.communications.peers.handlers;

import android.util.Log;

import org.bitcoinj.core.Transaction;

import java.io.InputStream;
import java.io.OutputStream;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;
import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.peers.PaymentRequestAuthorizer;
import ch.papers.payments.communications.peers.steps.PaymentFinalSignatureSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRefundSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestReceiveStep;
import ch.papers.payments.models.RefundTransactionWrapper;

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
            PaymentRequestReceiveStep paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);
            final DERObject paymentRequestResponse = paymentRequestReceiveStep.process(readDERObject());
            Log.d(TAG, "got request, authorizing user");
            if (paymentRequestAuthorizer.isPaymentRequestAuthorized(paymentRequestReceiveStep.getBitcoinURI())) {
                writeDERObject(paymentRequestResponse);
                final PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(this.walletServiceBinder, paymentRequestReceiveStep.getBitcoinURI(), paymentRequestReceiveStep.getTimestamp());
                writeDERObject(paymentRefundSendStep.process(readDERObject()));
                final PaymentFinalSignatureSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureSendStep(walletServiceBinder, paymentRequestReceiveStep.getBitcoinURI().getAddress(), paymentRefundSendStep.getFullSignedTransaction(), paymentRefundSendStep.getHalfSignedRefundTransaction());
                writeDERObject(paymentFinalSignatureSendStep.process(readDERObject()));
                Log.d(TAG, "payment was successful, we're done!");
                // payment was successful in every way, commit that tx
                walletServiceBinder.commitTransaction(paymentRefundSendStep.getFullSignedTransaction());
                final Transaction fullsignedRefundTransaction = paymentFinalSignatureSendStep.getFullSignedRefundTransation();
                UuidObjectStorage.getInstance().addEntry(new RefundTransactionWrapper(fullsignedRefundTransaction), new OnResultListener<RefundTransactionWrapper>() {
                    @Override
                    public void onSuccess(RefundTransactionWrapper refundTransactionWrapper) {
                        try {
                            UuidObjectStorage.getInstance().commit();
                        } catch (UuidObjectStorageException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(String s){
                        Log.d(TAG,s);
                    }
                }, RefundTransactionWrapper.class);

                paymentRequestAuthorizer.onPaymentSuccess();
            }
        } catch (Exception e){
            paymentRequestAuthorizer.onPaymentError(e.getMessage());
        }
    }
}
