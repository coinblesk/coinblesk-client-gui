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

import com.coinblesk.client.utils.DERPayloadParser;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.json.v1.SignVerifyTO;
import com.coinblesk.json.v1.TxSig;
import com.coinblesk.json.v1.Type;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class PaymentServerSignatureReceiveStep extends AbstractStep {
    private final static String TAG = PaymentServerSignatureReceiveStep.class.getName();

    private final WalletService.WalletServiceBinder walletService;
    private SignVerifyTO serverResponse;
    private List<TransactionSignature> serverTransactionSignatures;

    public PaymentServerSignatureReceiveStep(WalletService.WalletServiceBinder walletService) {
        super();
        this.walletService = walletService;
    }

    public List<TransactionSignature> getServerTransactionSignatures() {
        return serverTransactionSignatures;
    }

    @Override
    @Nullable
    public DERObject process(@NonNull DERObject input) throws PaymentException {
        final long startTime = System.currentTimeMillis();
        final Type responseType;
        final List<TxSig> serializedServerSigs;
        final TxSig messageSig;
        try {
            DERSequence derSequence = (DERSequence) input;
            DERPayloadParser parser = new DERPayloadParser(derSequence);

            responseType = Type.get(parser.getInt());
            serializedServerSigs = parser.getTxSigList();
            messageSig = parser.getTxSig();
        } catch (Exception e) {
            throw new PaymentException(PaymentError.DER_SERIALIZE_ERROR, e);
        }

        final ECKey serverKey = walletService.getMultisigServerKey();

        serverResponse = new SignVerifyTO()
                .publicKey(serverKey.getPubKey())
                .type(responseType)
                .signatures(serializedServerSigs)
                .messageSig(messageSig);

        if (responseType.isSuccess()) {
            Log.d(TAG, "Server responded with success: " + responseType);
        } else {
            Log.w(TAG, "Server responded with an error: " + responseType);
            throw new PaymentException(PaymentError.SERVER_ERROR, "Code: " + responseType.toString());
        }

        serverTransactionSignatures = SerializeUtils.deserializeSignatures(serializedServerSigs);

        /*
        if (serverResponse.type() == Type.SUCCESS_INSTANT) {
            String txHash = new Transaction(
                    walletService.getNetworkParameters(),
                    serverResponse.transaction())
                    .getHashAsString();
            walletService.markTransactionInstant(txHash);
        }
        */

        logStepProcess(startTime, input, null);
        return null;
    }

    public SignVerifyTO serverResponse() {
        return serverResponse;
    }
}
