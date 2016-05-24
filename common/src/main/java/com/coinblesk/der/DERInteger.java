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
