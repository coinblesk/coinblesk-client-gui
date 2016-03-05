package ch.papers.payments.communications.peers.handlers;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.peers.PaymentRequestAuthorizer;
import ch.papers.payments.communications.peers.steps.PaymentRefundSendStep;
import ch.papers.payments.communications.peers.steps.PaymentRequestReceiveStep;
import ch.papers.payments.models.ECKeyWrapper;
import ch.papers.payments.models.filters.ECKeyWrapperFilter;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class InstantPaymentClientHandler extends DERObjectStreamHandler{
    private final static String TAG = InstantPaymentClientHandler.class.getSimpleName();
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private final PaymentRequestAuthorizer paymentRequestAuthorizer;
    private PaymentRequestReceiveStep paymentRequestReceiveStep;

    public InstantPaymentClientHandler(InputStream inputStream, OutputStream outputStream, WalletService.WalletServiceBinder walletServiceBinder, PaymentRequestAuthorizer paymentRequestAuthorizer) {
        super(inputStream, outputStream);
        this.paymentRequestAuthorizer = paymentRequestAuthorizer;
        this.walletServiceBinder = walletServiceBinder;

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(Constants.MULTISIG_CLIENT_KEY_NAME), new OnResultListener<ECKeyWrapper>() {
            @Override
            public void onSuccess(ECKeyWrapper clientKey) {
                paymentRequestReceiveStep = new PaymentRequestReceiveStep(clientKey.getKey());
                countDownLatch.countDown();
            }

            @Override
            public void onError(String s) {

            }
        }, ECKeyWrapper.class);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Log.d(TAG,"kick off");
        writeDERObject(DERObject.NULLOBJECT); // we kick off the process
        DERObject paymentRequestResponse = paymentRequestReceiveStep.process(readDERObject());
        Log.d(TAG,"got request, authorizing user");
        if(paymentRequestAuthorizer.isPaymentRequestAuthorized(paymentRequestReceiveStep.getBitcoinURI())) {
            writeDERObject(paymentRequestResponse);
            final PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(this.walletServiceBinder, paymentRequestReceiveStep.getBitcoinURI());
            writeDERObject(paymentRefundSendStep.process(readDERObject()));
        }
    }
}
