package ch.papers.payments.communications.peers;

import android.content.Context;

import org.bitcoinj.uri.BitcoinURI;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 27/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class BluetoothLEPeer extends AbstractPeer {
    public BluetoothLEPeer(Context context) {
        super(context);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public void broadcastPaymentRequest(BitcoinURI paymentUri) {

    }
}
