/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.coinblesk.payments.communications.steps.cltv;

import com.coinblesk.client.config.Constants;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.payments.communications.steps.Step;

import org.bitcoinj.uri.BitcoinURI;

/**
 * @author Andreas Albrecht
 */
abstract class AbstractStep implements Step {

    private BitcoinURI bitcoinURI;

    protected AbstractStep() {
        this(null);
    }

    protected AbstractStep(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }

    public BitcoinURI getBitcoinURI() {
        return bitcoinURI;
    }

    public void setBitcoinURI(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }

    @Override
    public abstract DERObject process(DERObject input) throws PaymentException;

    protected boolean isProtocolVersionSupported(int otherVersion) {
        // could add other supported versions here, e.g. with switch fall through
        switch (otherVersion) {
            case Constants.CLIENT_COMMUNICATION_PROTOCOL_VERSION:
                return true;
        }
        return false;
    }

    protected int getProtocolVersion() {
        return Constants.CLIENT_COMMUNICATION_PROTOCOL_VERSION;
    }

}
