package com.coinblesk.payments.communications.messages;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 01/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DERString extends DERObject {
    private final String string;

    public DERString(String string) {
        super(string.getBytes());
        this.string = string;
    }

    public DERString(byte[] payload) {
        super(payload);
        this.string=new String(this.getPayload());
    }

    public String getString() {
        return this.string;
    }

    public byte getDERType(){
        return 12; //we treat this as an octet stream/byte array
    }
}
