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

import com.coinblesk.client.utils.DERPayloadParser;
import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;

import org.bitcoinj.uri.BitcoinURI;

import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class PaymentResponseReceiveCompactStep extends PaymentResponseReceiveStep {
    private final static String TAG = PaymentResponseReceiveCompactStep.class.getName();

    public PaymentResponseReceiveCompactStep(BitcoinURI bitcoinURI) {
        super(bitcoinURI);
    }

    @Override
    protected SignTO extractSignTO(DERPayloadParser parser) {
        long currentDate = parser.getLong();
        byte[] publicKey = parser.getBytes();
        byte[] serializedTx = parser.getBytes();
        long amountChange = parser.getLong();

        /*
        DERPayloadParser txInParser = new DERPayloadParser(parser.getDERSequence());
        List<byte[]> transactionInputs = new ArrayList<>(txInParser.size());
        for (int i = 0; i < txInParser.size(); ++i) {
            transactionInputs.add(txInParser.getBytes());
        }
        */

        List<TxSig> signatures = parser.getTxSigList();
        TxSig messageSig = parser.getTxSig();

        SignTO signTO = new SignTO()
                .currentDate(currentDate)
                .publicKey(publicKey)
                .transaction(serializedTx)
                .p2shAddressTo(getBitcoinURI().getAddress().toBase58())
                .amountToSpend(getBitcoinURI().getAmount().longValue())
                .amountChange(amountChange)
                //.transactionInputs(transactionInputs)
                .signatures(signatures)
                .messageSig(messageSig);
        return signTO;
    }
}
