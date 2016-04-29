package com.coinblesk.payments.communications.utils;

import com.coinblesk.json.TxSig;
import com.coinblesk.payments.communications.messages.DERInteger;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERSequence;
import org.bitcoinj.core.Coin;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class DERPayloadBuilder {

    private final List<DERObject> derObjectList;

    public DERPayloadBuilder() {
        derObjectList = new ArrayList<>();
    }

    public List<DERObject> getDERObjectList() {
        return derObjectList;
    }

    public DERSequence getAsDERSequence() {
        return new DERSequence(derObjectList);
    }

    public DERPayloadBuilder add(int value) {
        add(BigInteger.valueOf(value));
        return this;
    }

    public DERPayloadBuilder add(long value) {
        add(BigInteger.valueOf(value));
        return this;
    }

    public DERPayloadBuilder add(BigInteger bigInt) {
        derObjectList.add(new DERInteger(bigInt));
        return this;
    }

    public DERPayloadBuilder add(boolean bool) {
        add(BigInteger.valueOf(bool ? 1 : 0));
        return this;
    }

    public DERPayloadBuilder add(byte[] data) {
        derObjectList.add(new DERObject(data));
        return this;
    }

    public DERPayloadBuilder add(Coin amount) {
        add(amount.getValue());
        return this;
    }

    public DERPayloadBuilder add(DERSequence sequence) {
        derObjectList.add(sequence);
        return this;
    }

    public DERPayloadBuilder add(TxSig signature) {
        // Note: do not add in a loop! use add(List<TxSig>) for multiple signatures.
        DERPayloadBuilder builder = new DERPayloadBuilder()
            .add(new BigInteger(signature.sigR()))
            .add(new BigInteger(signature.sigS()));
        DERSequence sigSequence = builder.getAsDERSequence();
        add(sigSequence);
        return this;
    }

    public DERPayloadBuilder add(List<TxSig> signatures) {
        // wrap as "2D sequence"
        DERPayloadBuilder txSigBuilder = new DERPayloadBuilder();
        for (TxSig txSig : signatures) {
            txSigBuilder.add(txSig);
        }
        add(txSigBuilder.getAsDERSequence());
        return this;
    }
}
