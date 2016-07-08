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

    public DERPayloadBuilder add(DERObject derObject) {
        derObjectList.add(derObject);
        return this;
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
        add(new DERInteger(bigInt));
        return this;
    }

    public DERPayloadBuilder add(boolean bool) {
        add(BigInteger.valueOf(bool ? 1 : 0));
        return this;
    }

    public DERPayloadBuilder add(byte[] data) {
        add(new DERObject(data));
        return this;
    }

    public DERPayloadBuilder add(Coin amount) {
        add(amount.getValue());
        return this;
    }

    public DERPayloadBuilder add(String string) {
        add(new DERString(string));
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
