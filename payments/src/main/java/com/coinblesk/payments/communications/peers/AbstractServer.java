package com.coinblesk.payments.communications.peers;

import android.content.Context;

import com.coinblesk.payments.WalletService;

import org.bitcoinj.uri.BitcoinURI;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class AbstractServer extends AbstractPeer {
    private BitcoinURI paymentRequestUri;
    private final WalletService.WalletServiceBinder walletServiceBinder;

    private PaymentRequestDelegate paymentRequestDelegate = PaymentRequestDelegate.ALLOW_DELEGATE;


    protected AbstractServer(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context);
        this.walletServiceBinder = walletServiceBinder;
    }

    public void setPaymentRequestUri(BitcoinURI paymentRequestUri) {
        this.paymentRequestUri = paymentRequestUri;
    }

    public WalletService.WalletServiceBinder getWalletServiceBinder() {
        return walletServiceBinder;
    }

    public BitcoinURI getPaymentRequestUri() {
        return paymentRequestUri;
    }

    public boolean hasPaymentRequestUri() {
        return paymentRequestUri != null;
    }

    public PaymentRequestDelegate getPaymentRequestDelegate() {
        return paymentRequestDelegate;
    }

    public void setPaymentRequestDelegate(PaymentRequestDelegate paymentRequestDelegate) {
        this.paymentRequestDelegate = paymentRequestDelegate;
    }
}
