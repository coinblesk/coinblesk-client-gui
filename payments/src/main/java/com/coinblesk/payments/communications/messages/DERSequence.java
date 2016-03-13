package com.coinblesk.payments.communications.messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.coinblesk.payments.Utils;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 01/03/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class DERSequence extends DERObject {
    final private List<DERObject> children = new ArrayList<DERObject>();

    public DERSequence(List<DERObject> children){
        super(listSerialize(children));
        this.children.addAll(children);
    }

    public DERSequence(byte[] payload) {
        super(payload);
        byte[] restPayload = Arrays.copyOf(getPayload(), getPayload().length);
        while (restPayload.length > 0) {
            this.children.add(DERParser.parseDER(restPayload));
            restPayload = Arrays.copyOfRange(restPayload, DERParser.extractPayloadEndIndex(restPayload), restPayload.length);
        }
    }

    public List<DERObject> getChildren() {
        return this.children;
    }

    public byte getDERType(){
        return 48;
    }

    public static byte[] listSerialize(List<DERObject> derObjects){
        byte[] payload = new byte[0];
        for (DERObject derObject:derObjects) {
            payload = Utils.concatBytes(payload,derObject.serializeToDER());
        }
        return payload;
    }
}