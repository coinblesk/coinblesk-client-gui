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

import com.coinblesk.client.utils.ClientUtils;
import com.coinblesk.der.DERObject;
import com.coinblesk.client.utils.DERPayloadBuilder;
import com.coinblesk.json.v1.PaymentRequestTO;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.util.SerializeUtils;

import static com.google.common.base.Preconditions.checkState;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.uri.BitcoinURI;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentRequestSendStep extends AbstractStep {
    private final static String TAG = PaymentRequestSendStep.class.getName();
    private final ECKey signKey;

    public PaymentRequestSendStep(BitcoinURI requestURI, ECKey signKey) {
        super(requestURI);
        this.signKey = signKey;
    }

    @Override
    @NonNull
    public DERObject process(@Nullable DERObject input) throws PaymentException {
        final long startTime = System.currentTimeMillis();
        checkState(getBitcoinURI() != null && getBitcoinURI().getAddress() != null,
                "No Bitcoin request URI provided.");

        try {
            DERObject payload = createPaymentRequest();
            Log.d(TAG, String.format(
                    "Payment request - sending payment request: %s, (length=%d bytes)",
                    getBitcoinURI(), payload.serializeToDER().length));

            logStepProcess(startTime, input, payload);
            return payload;
        } catch (Exception e) {
            throw new PaymentException(PaymentError.DER_SERIALIZE_ERROR, e);
        }
    }

    private DERObject createPaymentRequest() {
        PaymentRequestTO request = new PaymentRequestTO()
                .publicKey(signKey.getPubKey())
                .version(getProtocolVersion())
                .address(getBitcoinURI().getAddress().toBase58())
                .amount(getBitcoinURI().getAmount().longValue());
        SerializeUtils.signJSON(request, signKey);

        DERPayloadBuilder builder = new DERPayloadBuilder()
                .add(request.publicKey())
                .add(request.version())
                .add(request.address())
                .add(request.amount())
                .add(request.messageSig());
        return builder.getAsDERSequence();
    }
}