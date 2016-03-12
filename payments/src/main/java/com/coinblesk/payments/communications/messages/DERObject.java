package com.coinblesk.payments.communications.messages;


import java.math.BigInteger;

import com.coinblesk.payments.Utils;

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

    public byte getDERType(){
        return 4; //we treat this as an octet stream/byte array
    }

    public byte[] serializeToDER(){
        if(this.payload.length>128){
            // long form
            byte[] lengthBytes = Utils.trim(BigInteger.valueOf(this.payload.length).toByteArray());
            byte[] lengthByteSize = Utils.trim(BigInteger.valueOf(lengthBytes.length).setBit(7).toByteArray());
            return Utils.concatBytes(new byte[]{this.getDERType()},lengthByteSize,lengthBytes, this.payload);
        } else {
            // short form
            return Utils.concatBytes(new byte[]{this.getDERType(), (byte) this.payload.length}, this.payload);
        }
    }
}
