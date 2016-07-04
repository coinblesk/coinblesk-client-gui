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
import com.coinblesk.json.SignVerifyTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.payments.WalletService;

import org.bitcoinj.uri.BitcoinURI;

import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class PaymentResponseReceiveCompactStep extends PaymentResponseReceiveStep {
    private final static String TAG = PaymentResponseReceiveCompactStep.class.getName();

    public PaymentResponseReceiveCompactStep(BitcoinURI bitcoinURI, WalletService.WalletServiceBinder walletService) {
        super(bitcoinURI, walletService);
    }

    @Override
    protected SignVerifyTO extractSignTO(DERPayloadParser parser) {
        byte[] publicKey = parser.getBytes();
        byte[] serializedTx = parser.getBytes();
        long amountChange = parser.getLong();

        List<TxSig> signatures = parser.getTxSigList();
        TxSig messageSig = parser.getTxSig();

        SignVerifyTO signTO = new SignVerifyTO()
                .publicKey(publicKey)
                .transaction(serializedTx)
                .addressTo(getBitcoinURI().getAddress().toBase58())
                .amountToSpend(getBitcoinURI().getAmount().longValue())
                .amountChange(amountChange)
                .signatures(signatures)
                .messageSig(messageSig);
        return signTO;
    }
}
