package com.coinblesk.payments.communications.messages;


import com.coinblesk.payments.Utils;

import java.math.BigInteger;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 01/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DERObject {
    public static final DERObject NULLOBJECT = new DERObject(new byte[0]);
    private final byte[] payload;

    public DERObject(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte getDERType() {
        //we treat this as an octet stream/byte array
        return 4;
    }

    public byte[] serializeToDER() {
        if (payload.length > 128) {
            // long form
            byte[] typeBytes = new byte[] { getDERType() };
            byte[] lengthBytes = Utils.trim(BigInteger.valueOf(payload.length).toByteArray());
            byte[] lengthByteSize = Utils.trim(BigInteger.valueOf(lengthBytes.length).setBit(7).toByteArray());
            return Utils.concatBytes(typeBytes, lengthByteSize, lengthBytes, payload);
        } else {
            // short form
            byte[] header = new byte[] { getDERType(), (byte) payload.length };
            return Utils.concatBytes(header, payload);
        }
    }
}
