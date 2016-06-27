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

import android.support.annotation.Nullable;

import com.coinblesk.bitcoin.TimeLockedAddress;
import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.client.models.LockTime;
import static com.google.common.base.Preconditions.checkState;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.uri.BitcoinURI;

import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class PaymentFinalizeStep extends AbstractStep {

    private final Transaction transaction;
    private final List<TransactionSignature> clientTxSignatures;
    private final List<TransactionSignature> serverTxSignatures;
    private WalletService.WalletServiceBinder walletService;

    public PaymentFinalizeStep(BitcoinURI bitcoinURI, Transaction transaction,
                                                        List<TransactionSignature> clientTxSignatures,
                                                        List<TransactionSignature> serverTxSignatures,
                                                        WalletService.WalletServiceBinder walletService) {
        super(bitcoinURI);
        this.transaction = transaction;
        this.clientTxSignatures = clientTxSignatures;
        this.serverTxSignatures = serverTxSignatures;
        this.walletService = walletService;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    @Nullable
    public DERObject process(@Nullable DERObject input) throws PaymentException {
        checkState(transaction != null, "No transaction provided.");
        checkState(clientTxSignatures != null, "Client signatures not provided.");
        checkState(serverTxSignatures != null, "Server signatures not provided.");

        assembleTransaction();

        return null;
    }

    private void assembleTransaction() throws PaymentException {
        int numClientSigs = clientTxSignatures.size();
        int numServerSigs = serverTxSignatures.size();
        if (numClientSigs != numServerSigs) {
            throw new PaymentException(PaymentError.TRANSACTION_ERROR);
        }

        for (int i = 0; i < numClientSigs; ++i) {
            TransactionInput txIn = transaction.getInput(i);
            byte[] hash = txIn.getConnectedOutput().getScriptPubKey().getPubKeyHash();
            TimeLockedAddress tla = walletService.findTimeLockedAddressByHash(hash);
            Script scriptSig = tla.createScriptSigBeforeLockTime(
                    clientTxSignatures.get(i), serverTxSignatures.get(i));
            txIn.setScriptSig(scriptSig);
            txIn.verify();
        }
        transaction.verify();
    }
}
