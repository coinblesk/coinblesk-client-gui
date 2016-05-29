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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.coinblesk.client.config.Constants;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.client.utils.DERPayloadParser;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentRequestReceiveStep extends AbstractStep {
    private final static String TAG = PaymentRequestReceiveStep.class.getName();

    public PaymentRequestReceiveStep() {
        super();
    }

    @Override
    @Nullable
    public DERObject process(@NonNull DERObject input) throws PaymentException {
        final NetworkParameters params = Constants.PARAMS;
        // input:
        final int protocolVersion;
        final String addressBase58;
        final Address addressTo;
        final Coin amount;

        /* deserialize payment request */
        try {
            DERSequence derSequence = (DERSequence) input;
            DERPayloadParser parser = new DERPayloadParser(derSequence);
            protocolVersion = parser.getInt();
            addressBase58 = parser.getString();
            amount = parser.getCoin();
        } catch (Exception e) {
            throw new PaymentException(PaymentError.DER_SERIALIZE_ERROR, e);
        }

        /* protocol version */
        Log.d(TAG, "Received protocol version: " + protocolVersion
                + ", my protocol version: " + getProtocolVersion());
        if (!isProtocolVersionSupported(protocolVersion)) {
            Log.w(TAG, String.format(
                    "Protocol version not supported. ours: %d - theirs: %d",
                    getProtocolVersion(), protocolVersion));
            throw new PaymentException(PaymentError.PROTOCOL_VERSION_NOT_SUPPORTED);
        }

        /* payment address */
        try {
            addressTo = Address.fromBase58(Constants.PARAMS, addressBase58);
            Log.d(TAG, "Received address: " + addressTo);
        } catch (WrongNetworkException e) {
            throw new PaymentException(PaymentError.WRONG_BITCOIN_NETWORK);
        } catch (AddressFormatException e) {
            throw new PaymentException(PaymentError.INVALID_PAYMENT_REQUEST);
        }

        /* payment amount */
        Log.d(TAG, "Received amount: " + amount);
        if (amount.isNegative()) {
            throw new PaymentException(PaymentError.INVALID_PAYMENT_REQUEST);
        }

        /* output: payment request as bitcoin URI */
        try {
            String bitcoinURIStr = BitcoinURI.convertToBitcoinURI(addressTo, amount, "", "");
            setBitcoinURI(new BitcoinURI(bitcoinURIStr));
        } catch (BitcoinURIParseException e) {
            throw new PaymentException(PaymentError.INVALID_PAYMENT_REQUEST, e);
        }

        Log.i(TAG, "Received payment request: " + getBitcoinURI());
        return null;
    }
}