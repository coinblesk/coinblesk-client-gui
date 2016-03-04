package ch.papers.payments.communications.peers.handlers;

import java.io.InputStream;
import java.io.OutputStream;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.listeners.OnResultListener;
import ch.papers.payments.Constants;
import ch.papers.payments.WalletService;
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
    private PaymentRequestReceiveStep paymentRequestReceiveStep;

    public InstantPaymentClientHandler(InputStream inputStream, OutputStream outputStream, WalletService.WalletServiceBinder walletServiceBinder) {
        super(inputStream, outputStream);
        this.walletServiceBinder = walletServiceBinder;
        UuidObjectStorage.getInstance().getFirstMatchEntry(new ECKeyWrapperFilter(Constants.MULTISIG_CLIENT_KEY_NAME), new OnResultListener<ECKeyWrapper>() {
            @Override
            public void onSuccess(ECKeyWrapper clientKey) {
                paymentRequestReceiveStep = new PaymentRequestReceiveStep(clientKey.getKey());
            }

            @Override
            public void onError(String s) {

            }
        }, ECKeyWrapper.class);
    }

    @Override
    public void run() {
        writeDERObject(paymentRequestReceiveStep.process(readDERObject()));
        final PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(this.walletServiceBinder,paymentRequestReceiveStep.getBitcoinURI());
        writeDERObject(paymentRefundSendStep.process(readDERObject()));
    }
}
