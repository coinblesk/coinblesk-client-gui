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
import android.util.Log;

import com.coinblesk.client.CoinbleskWebService;
import com.coinblesk.client.config.Constants;
import com.coinblesk.der.DERObject;
import com.coinblesk.json.v1.SignVerifyTO;
import com.coinblesk.json.v1.TxSig;
import com.coinblesk.json.v1.Type;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.util.CoinbleskException;
import com.coinblesk.util.InsufficientFunds;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import retrofit2.Response;

import static com.google.common.base.Preconditions.checkState;


public class CLTVInstantPaymentStep extends AbstractStep {
    private static final String TAG = CLTVInstantPaymentStep.class.getName();

    private final WalletService.WalletServiceBinder walletServiceBinder;
    private Transaction transaction;
    private List<TransactionSignature> clientTransactionSignatures;
    private List<TransactionSignature> serverTransactionSignatures;

    public CLTVInstantPaymentStep(WalletService.WalletServiceBinder walletServiceBinder, BitcoinURI paymentRequest) {
        super(paymentRequest);
        this.walletServiceBinder = walletServiceBinder;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    @Nullable
    public DERObject process(@Nullable DERObject input) throws PaymentException {
        final long startTime = System.currentTimeMillis();
        try {
            checkState(walletServiceBinder != null, "WalletService not provided.");
            checkState(getBitcoinURI() != null, "Payment request (bitcoinURI) not provided.");

            NetworkParameters params = walletServiceBinder.getNetworkParameters();

            /* payment address */
            final Address addressTo = getBitcoinURI().getAddress();
            /* payment amount */
            final Coin amount = getBitcoinURI().getAmount();
            if (amount.isNegative() || amount.isLessThan(Constants.MIN_PAYMENT_REQUEST_AMOUNT)) {
                throw new PaymentException(PaymentError.INVALID_PAYMENT_REQUEST);
            }

            /* create Tx and client signatures */
            final ECKey clientKey = walletServiceBinder.getMultisigClientKey();
            try {
                transaction = walletServiceBinder.createTransaction(addressTo, amount);
                clientTransactionSignatures = walletServiceBinder.signTransaction(transaction);
            } catch (CoinbleskException e) {
                throw new PaymentException(PaymentError.TRANSACTION_ERROR, e);
            } catch (InsufficientFunds e) {
                throw new PaymentException(PaymentError.INSUFFICIENT_FUNDS, e);
            }

            /* prepare server request */
            final SignVerifyTO signTO = new SignVerifyTO()
                    .currentDate(System.currentTimeMillis())
                    .publicKey(clientKey.getPubKey())
                    .transaction(transaction.unsafeBitcoinSerialize())
                    .signatures(SerializeUtils.serializeSignatures(clientTransactionSignatures));
            SerializeUtils.signJSON(signTO, clientKey);

            Log.i(TAG, "Transaction size: " + signTO.transaction().length);


            /* execute server request and handle response */
            final Response<SignVerifyTO> serverResponse;
            try {
                final CoinbleskWebService service = walletServiceBinder.getCoinbleskService();
                serverResponse = service.signVerify(signTO).execute();
            } catch (IOException e) {
                throw new PaymentException(PaymentError.SERVER_ERROR, e.getMessage());
            }

            if (!serverResponse.isSuccessful()) {
                throw new PaymentException(PaymentError.SERVER_ERROR,
                        "HTTP code: " + serverResponse.code());
            }

            final SignVerifyTO serverSignTO = serverResponse.body();
            if (!serverSignTO.isSuccess()) {
                throw new PaymentException(PaymentError.SERVER_ERROR,
                        "Code: " + serverSignTO.type().toString());
            }

            /* verify signature */
            final ECKey serverKey = walletServiceBinder.getMultisigServerKey();
            if (!Arrays.equals(
                    serverKey.getPubKey(), serverSignTO.publicKey())) {
                throw new PaymentException(PaymentError.MESSAGE_SIGNATURE_ERROR);
            }
            if (!SerializeUtils.verifyJSONSignature(serverSignTO, serverKey)) {
                throw new PaymentException(PaymentError.MESSAGE_SIGNATURE_ERROR);
            }

            serverTransactionSignatures = SerializeUtils.deserializeSignatures(serverSignTO.signatures());

            /* Payment Finalize */
            // note: We could access serverSignTO.transaction(), but this step does additional checks.
            PaymentFinalizeStep finalizeStep = new PaymentFinalizeStep(
                    getBitcoinURI(),
                    transaction,
                    clientTransactionSignatures,
                    serverTransactionSignatures,
                    walletServiceBinder);
            finalizeStep.process(null);

            transaction = finalizeStep.getTransaction();

            if (serverSignTO.type() == Type.SUCCESS_INSTANT) {
                String txHash = transaction.getHashAsString();
                walletServiceBinder.markTransactionInstant(txHash);
            }

        } catch (PaymentException pex) {
            Log.e(TAG, "Exception: ", pex);
            throw pex;
        } catch (Exception e) {
            Log.e(TAG, "Exception: ", e);
            throw new PaymentException(PaymentError.ERROR, e);
        }

        logStepProcess(startTime, input, null);
        return null;
    }
}