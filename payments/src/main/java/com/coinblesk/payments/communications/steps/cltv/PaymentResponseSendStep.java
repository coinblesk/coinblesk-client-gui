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
import com.coinblesk.json.SignTO;
import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.payments.communications.steps.AbstractStep;
import com.coinblesk.client.utils.DERPayloadBuilder;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsufficientFunds;
import com.coinblesk.util.SerializeUtils;
import static com.google.common.base.Preconditions.*;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;

import java.util.List;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentResponseSendStep extends AbstractStep {
    private final static String TAG = PaymentResponseSendStep.class.getName();

    private final WalletService.WalletServiceBinder walletService;
    private Transaction transaction;
    private List<TransactionSignature> clientTransactionSignatures;

    public PaymentResponseSendStep(BitcoinURI bitcoinURI, WalletService.WalletServiceBinder walletService) {
        super(bitcoinURI);
        this.walletService = walletService;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public List<TransactionSignature> getClientTransactionSignatures() {
        return clientTransactionSignatures;
    }

    @Override
    @NonNull
    public DERObject process(@Nullable DERObject input) throws PaymentException {
        checkState(getBitcoinURI() != null, "BitcoinURI not provided.");

        // input is the bitcoinURI (constructor)
        createTxAndSign(getBitcoinURI().getAddress(), getBitcoinURI().getAmount());
        DERObject response = createDERResponse();
        return response;
    }

    private void createTxAndSign(final Address addressTo, final Coin amount) throws PaymentException {
        try {
            transaction = walletService.createTransaction(addressTo, amount);
            clientTransactionSignatures = walletService.signTransaction(transaction);
        } catch (CoinbleskException e) {
            throw new PaymentException(ResultCode.TRANSACTION_ERROR.toString(), e);
        } catch (InsufficientFunds e) {
            throw new PaymentException(ResultCode.INSUFFICIENT_FUNDS.toString(), e);
        }
    }

    private DERObject createDERResponse() {
        checkNotNull(walletService.getMultisigClientKey(), "Client key does not exist");
        final ECKey clientKey = walletService.getMultisigClientKey();
        final SignTO signTO = createSignTO(transaction, clientTransactionSignatures, clientKey);
        DERPayloadBuilder builder = new DERPayloadBuilder();
        appendSignTO(builder, signTO);
        return builder.getAsDERSequence();
    }

    private SignTO createSignTO(Transaction transaction, List<TransactionSignature> txSignatures, ECKey clientKey) {
        SignTO signTO = new SignTO()
                .currentDate(System.currentTimeMillis())
                .publicKey(clientKey.getPubKey())
                .transaction(transaction.unsafeBitcoinSerialize())
                .signatures(SerializeUtils.serializeSignatures(txSignatures));
        SerializeUtils.signJSON(signTO, clientKey);
        return signTO;
    }

    private DERPayloadBuilder appendSignTO(DERPayloadBuilder builder, SignTO signTO) {
        builder.add(signTO.currentDate())
                .add(signTO.publicKey())
                .add(signTO.transaction())
                .add(signTO.signatures())
                .add(signTO.messageSig());
        return builder;
    }
}