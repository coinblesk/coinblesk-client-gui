package com.coinblesk.payments.communications.messages;

import java.math.BigInteger;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 01/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DERInteger extends DERObject{
    private final BigInteger bigInteger;

    public DERInteger(BigInteger bigInteger) {
        super(bigInteger.toByteArray());
        this.bigInteger = bigInteger;
    }

    public DERInteger(byte[] payload) {
        super(payload);
        this.bigInteger = new BigInteger(this.getPayload());
    }

    public BigInteger getBigInteger() {
        return this.bigInteger;
    }

    public byte getDERType(){
        return 2;
    }
}
