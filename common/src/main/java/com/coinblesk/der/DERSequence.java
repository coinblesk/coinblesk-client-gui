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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            children.add(DERParser.parseDER(restPayload));
            int start = DERParser.extractPayloadEndIndex(restPayload);
            restPayload = Arrays.copyOfRange(restPayload, start, restPayload.length);
        }
    }

    public List<DERObject> getChildren() {
        return children;
    }

    public byte getDERType(){
        return 48;
    }

    public static byte[] listSerialize(List<DERObject> derObjects){
        byte[] payload = new byte[0];
        for (DERObject derObject : derObjects) {
            payload = ClientUtils.concatBytes(payload, derObject.serializeToDER());
        }
        return payload;
    }
}
