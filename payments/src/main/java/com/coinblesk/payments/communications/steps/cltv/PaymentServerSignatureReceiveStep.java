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
import com.coinblesk.json.TxSig;
import com.coinblesk.json.Type;
import com.coinblesk.client.config.Constants;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.payments.communications.steps.AbstractStep;
import com.coinblesk.client.utils.DERPayloadParser;
import com.coinblesk.util.SerializeUtils;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class PaymentServerSignatureReceiveStep extends AbstractStep {
    private final static String TAG = PaymentServerSignatureReceiveStep.class.getName();

    private List<TransactionSignature> serverTransactionSignatures;

    public PaymentServerSignatureReceiveStep() {
        super();
    }

    public List<TransactionSignature> getServerTransactionSignatures() {
        return serverTransactionSignatures;
    }

    @Override
    @Nullable
    public DERObject process(@NonNull DERObject input) throws PaymentException {
        final DERSequence derSequence = (DERSequence) input;
        final DERPayloadParser parser = new DERPayloadParser(derSequence);

        Type responseType = Type.get(parser.getInt());
        if (responseType.isError()) {
            Log.w(TAG, "Server responded with an error: " + responseType);
            throw new PaymentException(
                    ResultCode.SERVER_ERROR.toString() + "/" + responseType.toString());
        }

        List<TxSig> serializedServerSigs = parser.getTxSigList();
        serverTransactionSignatures = SerializeUtils.deserializeSignatures(serializedServerSigs);

        return null;
    }
}
