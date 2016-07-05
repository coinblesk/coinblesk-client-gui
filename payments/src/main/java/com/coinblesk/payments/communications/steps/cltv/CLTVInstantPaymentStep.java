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

import com.coinblesk.der.DERObject;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;

import static com.google.common.base.Preconditions.checkState;


public class CLTVInstantPaymentStep extends AbstractStep {
    private static final String TAG = CLTVInstantPaymentStep.class.getName();

    private final WalletService.WalletServiceBinder walletServiceBinder;
    private Transaction transaction;

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
        try {
            checkState(walletServiceBinder != null, "WalletService not provided.");
            checkState(getBitcoinURI() != null, "Payment request (bitcoinURI) not provided.");

            /* Payment Request */
            PaymentRequestSendStep sendRequest = new PaymentRequestSendStep(getBitcoinURI(), walletServiceBinder.getMultisigClientKey());
            DERObject requestOutput = sendRequest.process(null);
            PaymentRequestReceiveStep receiveRequest = new PaymentRequestReceiveStep(walletServiceBinder.networkParameters());
            receiveRequest.process(requestOutput);

            /* Payment Response */
            PaymentResponseSendStep sendResponse = new PaymentResponseSendStep(
                    receiveRequest.getBitcoinURI(), walletServiceBinder);
            DERObject responseOutput = sendResponse.process(null);

            Log.i(TAG, "PaymentResponseSend: " + responseOutput.serializeToDER().length);
            Log.i(TAG, "Transaction size: " + sendResponse.getTransaction().unsafeBitcoinSerialize().length);

            PaymentResponseReceiveStep receiveResponse = new PaymentResponseReceiveStep(
                    getBitcoinURI(), walletServiceBinder);
            receiveResponse.verifyPayeeSig(false); // since not user-to-user payment
            DERObject serverOutput = receiveResponse.process(responseOutput);

            /* Server Signatures */
            PaymentServerSignatureReceiveStep receiveSignatures = new PaymentServerSignatureReceiveStep();
            receiveSignatures.process(serverOutput);

            /* Payment Finalize */
            PaymentFinalizeStep finalizeStep = new PaymentFinalizeStep(
                    receiveRequest.getBitcoinURI(),
                    sendResponse.getTransaction(),
                    sendResponse.getClientTransactionSignatures(),
                    receiveSignatures.getServerTransactionSignatures(),
                    walletServiceBinder);
            finalizeStep.process(null);

            transaction = finalizeStep.getTransaction();


        } catch (PaymentException pex) {
            Log.e(TAG, "Exception: ", pex);
            throw pex;
        } catch (Exception e) {
            Log.e(TAG, "Exception: ", e);
            throw new PaymentException(PaymentError.ERROR, e);
        }
        return null;
    }
}
