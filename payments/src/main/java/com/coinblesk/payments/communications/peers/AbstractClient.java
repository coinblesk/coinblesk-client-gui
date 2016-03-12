package com.coinblesk.payments.communications.peers;

import android.content.Context;

import com.coinblesk.payments.WalletService;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class AbstractClient extends AbstractPeer {
    private boolean isReadyForInstantPayment = false;
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private PaymentRequestAuthorizer paymentRequestAuthorizer = PaymentRequestAuthorizer.ALLOW_AUTHORIZER;

    protected AbstractClient(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context);
        this.walletServiceBinder = walletServiceBinder;
    }

    public boolean isReadyForInstantPayment() {
        return isReadyForInstantPayment;
    }

    public void setReadyForInstantPayment(boolean readyForInstantPayment) {
        isReadyForInstantPayment = readyForInstantPayment;
        this.onIsReadyForInstantPaymentChange();
    }

    public WalletService.WalletServiceBinder getWalletServiceBinder(){
        return this.walletServiceBinder;
    }

    public abstract void onIsReadyForInstantPaymentChange();

    public PaymentRequestAuthorizer getPaymentRequestAuthorizer() {
        return paymentRequestAuthorizer;
    }

    public void setPaymentRequestAuthorizer(PaymentRequestAuthorizer paymentRequestAuthorizer) {
        this.paymentRequestAuthorizer = paymentRequestAuthorizer;
    }
}
