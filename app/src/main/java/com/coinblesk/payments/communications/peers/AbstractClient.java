package com.coinblesk.payments.communications.peers;

import android.content.Context;

import com.coinblesk.payments.WalletService;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class AbstractClient extends AbstractPeer {
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private PaymentRequestDelegate paymentRequestDelegate = PaymentRequestDelegate.ALLOW_DELEGATE;

    protected AbstractClient(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context);
        this.walletServiceBinder = walletServiceBinder;
    }

    public WalletService.WalletServiceBinder getWalletServiceBinder() {
        return this.walletServiceBinder;
    }

    public PaymentRequestDelegate getPaymentRequestDelegate() {
        return paymentRequestDelegate;
    }

    public void setPaymentRequestDelegate(PaymentRequestDelegate paymentRequestDelegate) {
        this.paymentRequestDelegate = paymentRequestDelegate;
    }
}
