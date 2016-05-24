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

package com.coinblesk.der;


import com.coinblesk.client.utils.ClientUtils;

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
            byte[] lengthBytes = ClientUtils.trimLeadingZeros(BigInteger.valueOf(payload.length).toByteArray());
            byte[] lengthByteSize = ClientUtils.trimLeadingZeros(BigInteger.valueOf(lengthBytes.length).setBit(7).toByteArray());
            return ClientUtils.concatBytes(typeBytes, lengthByteSize, lengthBytes, payload);
        } else {
            // short form
            byte[] header = new byte[] { getDERType(), (byte) payload.length };
            return ClientUtils.concatBytes(header, payload);
        }
    }
}
