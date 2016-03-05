package ch.papers.payments.communications.peers;

import android.content.Context;

import org.bitcoinj.uri.BitcoinURI;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class AbstractServer extends AbstractPeer {
    private BitcoinURI paymentRequestUri;
    protected AbstractServer(Context context) {
        super(context);
    }

    public void setPaymentRequestUri(BitcoinURI paymentRequestUri) {
        this.paymentRequestUri = paymentRequestUri;
        this.onChangePaymentRequest();
    }

    public BitcoinURI getPaymentRequestUri() {
        return paymentRequestUri;
    }

    public abstract void onChangePaymentRequest();

    public boolean hasPaymentRequestUri() {
        return paymentRequestUri != null;
    }
}
