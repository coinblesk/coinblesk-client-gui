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

import com.coinblesk.client.utils.DERPayloadBuilder;
import com.coinblesk.json.SignVerifyTO;
import com.coinblesk.payments.WalletService;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.uri.BitcoinURI;

import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class PaymentResponseSendCompactStep extends PaymentResponseSendStep {

    public PaymentResponseSendCompactStep(BitcoinURI bitcoinURI, WalletService.WalletServiceBinder walletService) {
        super(bitcoinURI, walletService);
    }

    @Override
    protected SignVerifyTO createSignTO(Transaction transaction, List<TransactionSignature> txSignatures, ECKey clientKey) {
        final NetworkParameters params = transaction.getParams();
        SignVerifyTO signTO = new SignVerifyTO()
                .publicKey(clientKey.getPubKey())
                .signatures(SerializeUtils.serializeSignatures(txSignatures));

        if (transaction.getOutputs().size() == 2) {
            Transaction smallTx = new Transaction(params, transaction.bitcoinSerialize());
            smallTx.clearOutputs();
            signTO.transaction(smallTx.unsafeBitcoinSerialize());

            signTO.addressTo(getBitcoinURI().getAddress().toBase58());
            signTO.amountToSpend(getBitcoinURI().getAmount().longValue());

            TransactionOutput firstTxOut = transaction.getOutputs().get(0);
            TransactionOutput secondTxOut = transaction.getOutputs().get(1);
            boolean changeIsFirstOutput = getBitcoinURI().getAddress().equals(
                    secondTxOut.getScriptPubKey().getToAddress(params));
            if (changeIsFirstOutput) {
                signTO.amountChange(firstTxOut.getValue().longValue());
            } else {
                signTO.amountChange(secondTxOut.getValue().longValue());
            }
        } else {
            signTO.transaction(transaction.unsafeBitcoinSerialize());
        }

        SerializeUtils.signJSON(signTO, clientKey);
        return signTO;
    }

    @Override
    protected DERPayloadBuilder appendSignTO(DERPayloadBuilder builder, SignVerifyTO signTO) {
        builder.add(signTO.publicKey())
                .add(signTO.transaction())
                .add(signTO.amountChange())
                .add(signTO.signatures())
                .add(signTO.messageSig());
        return builder;
    }
}