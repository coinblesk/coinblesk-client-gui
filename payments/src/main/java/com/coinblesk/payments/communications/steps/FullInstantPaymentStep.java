package com.coinblesk.payments.communications.steps;

import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERObject;
import com.coinblesk.client.models.RefundTransactionWrapper;

import org.bitcoinj.uri.BitcoinURI;

import ch.papers.objectstorage.UuidObjectStorage;
import ch.papers.objectstorage.UuidObjectStorageException;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 16/04/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class FullInstantPaymentStep implements Step {
    final private WalletService.WalletServiceBinder walletServiceBinder;
    final private BitcoinURI paymentRequest;

    public FullInstantPaymentStep(WalletService.WalletServiceBinder walletServiceBinder, BitcoinURI paymentRequest) {
        this.walletServiceBinder = walletServiceBinder;
        this.paymentRequest = paymentRequest;
    }


    @Override
    public DERObject process(DERObject input) {
        PaymentRequestSendStep paymentRequestSendStep = new PaymentRequestSendStep(paymentRequest);
        PaymentRequestReceiveStep paymentRequestReceiveStep = new PaymentRequestReceiveStep(walletServiceBinder);
        PaymentAuthorizationReceiveStep paymentAuthorizationReceiveStep = new PaymentAuthorizationReceiveStep(paymentRequest);
        PaymentRefundSendStep paymentRefundSendStep = new PaymentRefundSendStep(walletServiceBinder, paymentRequest, paymentRequestReceiveStep.getTimestamp());
        PaymentRefundReceiveStep paymentRefundReceiveStep = new PaymentRefundReceiveStep(walletServiceBinder.getMultisigClientKey());


        DERObject output = paymentRequestSendStep.process(input);
        output = paymentRequestReceiveStep.process(output);
        output = paymentAuthorizationReceiveStep.process(output);
        output = paymentRefundSendStep.process(output);
        output = paymentRefundReceiveStep.process(output);

        PaymentFinalSignatureOutpointsSendStep paymentFinalSignatureSendStep = new PaymentFinalSignatureOutpointsSendStep(walletServiceBinder, paymentRequest.getAddress(), paymentRefundSendStep.getClientSignatures(), paymentRefundSendStep.getServerSignatures(), paymentRefundSendStep.getFullSignedTransaction(), paymentRefundSendStep.getHalfSignedRefundTransaction());
        PaymentFinalSignatureOutpointsReceiveStep paymentFinalSignatureReceiveStep = new PaymentFinalSignatureOutpointsReceiveStep(walletServiceBinder.getMultisigClientKey(), paymentAuthorizationReceiveStep.getServerSignatures(), paymentRequest);
        output = paymentFinalSignatureSendStep.process(output);
        output = paymentFinalSignatureReceiveStep.process(output);

        this.walletServiceBinder.commitAndBroadcastTransaction(paymentFinalSignatureReceiveStep.getFullSignedTransaction());

        try {
            UuidObjectStorage.getInstance().addEntry(new RefundTransactionWrapper(paymentFinalSignatureSendStep.getFullSignedRefundTransation()), RefundTransactionWrapper.class);
            UuidObjectStorage.getInstance().commit();
        } catch (UuidObjectStorageException e) {
            e.printStackTrace();
        }

        return output;
    }
}
