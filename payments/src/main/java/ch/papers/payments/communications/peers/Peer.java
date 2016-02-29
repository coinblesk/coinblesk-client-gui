package ch.papers.payments.communications.peers;

import org.bitcoinj.uri.BitcoinURI;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface Peer {
    public void start();
    public void stop();
    public boolean isSupported();

    public void broadcastPaymentRequest(BitcoinURI paymentUri);
}
