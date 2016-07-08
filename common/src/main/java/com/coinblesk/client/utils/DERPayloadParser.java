/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.client.utils;

import com.coinblesk.der.DERInteger;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.der.DERString;
import com.coinblesk.json.v1.TxSig;

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

    private DERString getDERString(int index) {
        return (DERString) getDERObject(index);
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

    public String getString() {
        return getString(currentIndex++);
    }

    private String getString(int index) {
        return getDERString(index).getString();
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
