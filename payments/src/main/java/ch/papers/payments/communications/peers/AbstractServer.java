package ch.papers.payments.communications.peers;

import android.content.Context;

import org.bitcoinj.uri.BitcoinURI;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 04/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public abstract class AbstractServer extends AbstractPeer {
    protected AbstractServer(Context context) {
        super(context);
    }

    public abstract void broadcastPaymentRequest(BitcoinURI paymentUri);

    public abstract void cancelPaymentRequest();
}
