package com.coinblesk.payments.communications.peers;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 26/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface Peer {
    void start();
    void stop();
    boolean isSupported();
}
