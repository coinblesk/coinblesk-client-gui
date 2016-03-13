package com.coinblesk.payments.communications.peers.nfc;

/**
 * Created by draft on 10.03.16.
 */
public interface NFCClientACSCallback {
    void tagDiscovered(ACSTransceiver transceiver);

    void tagFailed();

    void nfcTagLost();
}