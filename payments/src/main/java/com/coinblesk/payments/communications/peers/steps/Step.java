package com.coinblesk.payments.communications.peers.steps;

import com.coinblesk.der.DERObject;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public interface Step {
    public DERObject process(DERObject input);
}
