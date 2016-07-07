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
import com.coinblesk.client.utils.DERPayloadParser;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.json.v1.PaymentRequestTO;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
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

    private final NetworkParameters networkParameters;

    public PaymentRequestReceiveStep(NetworkParameters networkParameters) {
        super();
        this.networkParameters = networkParameters;
    }

    @Override
    @Nullable
    public DERObject process(@NonNull DERObject input) throws PaymentException {
        /* deserialize payment request */
        PaymentRequestTO request = null;
        try {
            DERSequence derSequence = (DERSequence) input;
            DERPayloadParser parser = new DERPayloadParser(derSequence);
            request = new PaymentRequestTO()
                    .publicKey(parser.getBytes())
                    .version(parser.getInt())
                    .address(parser.getString())
                    .amount(parser.getLong())
                    .messageSig(parser.getTxSig());
        } catch (Exception e) {
            throw new PaymentException(PaymentError.DER_SERIALIZE_ERROR, e);
        }

        /* message sig */
        final ECKey payeeKey = ECKey.fromPublicOnly(request.publicKey());
        if (!SerializeUtils.verifyJSONSignature(request, payeeKey)) {
            throw new PaymentException(PaymentError.MESSAGE_SIGNATURE_ERROR);
        }

        /* protocol version */
        if (!isProtocolVersionSupported(request.version())) {
            Log.w(TAG, String.format(
                    "Protocol version not supported. ours: %d - theirs: %d",
                    getProtocolVersion(), request.version()));
            throw new PaymentException(PaymentError.PROTOCOL_VERSION_NOT_SUPPORTED);
        }

        /* payment address */
        final Address addressTo;
        try {
            addressTo = Address.fromBase58(networkParameters, request.address());
            Log.d(TAG, "Received address: " + addressTo);
        } catch (WrongNetworkException e) {
            throw new PaymentException(PaymentError.WRONG_BITCOIN_NETWORK);
        } catch (AddressFormatException e) {
            throw new PaymentException(PaymentError.INVALID_PAYMENT_REQUEST);
        }

        /* payment amount */
        final Coin amount = Coin.valueOf(request.amount());
        Log.d(TAG, "Received amount: " + amount);
        if (amount.isNegative() || amount.isLessThan(Constants.MIN_PAYMENT_REQUEST_AMOUNT)) {
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