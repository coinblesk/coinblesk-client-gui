package com.coinblesk.payments.communications.peers;

import android.content.Context;

import org.bitcoinj.uri.BitcoinURI;

import com.coinblesk.payments.WalletService;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class AbstractServer extends AbstractPeer {
    private static BitcoinURI paymentRequestUri;
    private final WalletService.WalletServiceBinder walletServiceBinder;

    private PaymentRequestAuthorizer paymentRequestAuthorizer = PaymentRequestAuthorizer.ALLOW_AUTHORIZER;


    protected AbstractServer(Context context, WalletService.WalletServiceBinder walletServiceBinder) {
        super(context);
        this.walletServiceBinder = walletServiceBinder;
    }

    public void setPaymentRequestUri(BitcoinURI paymentRequestUri) {
        this.paymentRequestUri = paymentRequestUri;
        this.onChangePaymentRequest();
    }

    public WalletService.WalletServiceBinder getWalletServiceBinder() {
        return walletServiceBinder;
    }

    public BitcoinURI getPaymentRequestUri() {
        return paymentRequestUri;
    }

    public abstract void onChangePaymentRequest();

    public boolean hasPaymentRequestUri() {
        return paymentRequestUri != null;
    }

    public PaymentRequestAuthorizer getPaymentRequestAuthorizer() {
        return paymentRequestAuthorizer;
    }

    public void setPaymentRequestAuthorizer(PaymentRequestAuthorizer paymentRequestAuthorizer) {
        this.paymentRequestAuthorizer = paymentRequestAuthorizer;
    }
}
