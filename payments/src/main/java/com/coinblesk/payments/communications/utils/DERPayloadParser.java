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
public class DERPayloadParser {

    private final DERSequence input;
    private int currentIndex;

    public DERPayloadParser(DERSequence derSequence) {
        input = derSequence;
        currentIndex = 0;
    }

    public int size() {
        return input.getChildren().size();
    }

    public int currentIndex() {
        return currentIndex;
    }

    private DERObject getDERObject(int index) {
        return input.getChildren().get(index);
    }

    private DERInteger getDERInteger(int index) {
        return (DERInteger) getDERObject(index);
    }

    public DERSequence getDERSequence() {
        return getDERSequence(currentIndex++);
    }

    private DERSequence getDERSequence(int index) {
        return (DERSequence) getDERObject(index);
    }

    public int getInt() {
        return getInt(currentIndex++);
    }

    private int getInt(int index) {
        return getBigInt(index).intValue();
    }

    public long getLong() {
        return getLong(currentIndex++);
    }

    private long getLong(int index) {
        long value = getBigInt(index).longValue();
        return value;
    }

    public BigInteger getBigInt() {
        return getBigInt(currentIndex++);
    }

    private BigInteger getBigInt(int index) {
        DERInteger derInt = getDERInteger(index);
        BigInteger bigInt = derInt.getBigInteger();
        return bigInt;
    }

    public boolean getBoolean() {
        return getBoolean(currentIndex++);
    }

    private boolean getBoolean(int index) {
        long bool = getBigInt(index).longValue();
        return (bool == 1);
    }

    public byte[] getBytes() {
        return getBytes(currentIndex++);
    }

    private byte[] getBytes(int index) {
        byte[] data = getDERObject(index).getPayload();
        return data;
    }

    public Coin getCoin() {
        return getCoin(currentIndex++);
    }

    private Coin getCoin(int index) {
        long value = getLong(index);
        Coin amount = Coin.valueOf(value);
        return amount;
    }

    public TxSig getTxSig() {
        // Note: do not use in a loop to get multiple signatures! use getTxSigList() for that
        return getTxSig(currentIndex++);
    }

    private TxSig getTxSig(int index) {
        DERSequence sigSequence = getDERSequence(index);
        DERPayloadParser sigParser = new DERPayloadParser(sigSequence);
        TxSig sig = new TxSig();
        sig.sigR(sigParser.getBigInt().toString());
        sig.sigS(sigParser.getBigInt().toString());
        return sig;
    }

    public List<TxSig> getTxSigList() {
        // unwrap "2D sequence"
        DERSequence txSigSequences = getDERSequence();
        DERPayloadParser txSigParser = new DERPayloadParser(txSigSequences);
        List<TxSig> signatures = new ArrayList<>(txSigParser.size());
        for (int i = 0; i < txSigParser.size(); ++i) {
            TxSig txSig = txSigParser.getTxSig();
            signatures.add(txSig);
        }
        return signatures;
    }
}
