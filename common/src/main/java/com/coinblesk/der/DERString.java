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

import com.google.common.base.Charsets;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class DERString extends DERObject {
    private final String string;

    public DERString(String string) {
        super(string.getBytes(Charsets.UTF_8));
        this.string = string;
    }

    public DERString(byte[] payload) {
        super(payload);
        this.string = new String(getPayload(), Charsets.UTF_8);
    }

    public String getString() {
        return this.string;
    }

    public byte getDERType() {
        return 12; // we treat this as an octet stream/byte array
    }
}