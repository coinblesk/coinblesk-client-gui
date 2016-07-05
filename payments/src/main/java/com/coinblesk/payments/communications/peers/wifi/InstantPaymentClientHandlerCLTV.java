/*
 * Copyright 2016 The Coinblesk team and the CSG Group at University of Zurich
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.coinblesk.payments.communications.peers.wifi;

import android.util.Log;

import com.coinblesk.payments.WalletService;
import com.coinblesk.der.DERObject;
import com.coinblesk.payments.communications.peers.PaymentRequestDelegate;
import com.coinblesk.payments.communications.peers.handlers.DERObjectStreamHandler;
import com.coinblesk.payments.communications.steps.cltv.PaymentFinalizeStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentRequestReceiveStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentResponseSendStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentServerSignatureReceiveStep;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Andreas Albrecht
 */
public class InstantPaymentClientHandlerCLTV extends DERObjectStreamHandler {
    private final static String TAG = InstantPaymentClientHandlerCLTV.class.getSimpleName();
    private final WalletService.WalletServiceBinder walletServiceBinder;
    private final PaymentRequestDelegate paymentRequestDelegate;

    public InstantPaymentClientHandlerCLTV(InputStream inputStream, OutputStream outputStream,
                                           WalletService.WalletServiceBinder walletServiceBinder,
                                           PaymentRequestDelegate paymentRequestDelegate) {
        super(inputStream, outputStream);
        this.paymentRequestDelegate = paymentRequestDelegate;
        this.walletServiceBinder = walletServiceBinder;
    }

    @Override
    public void run() {
        Log.d(TAG, "run: kick off");
        try {
            writeDERObject(DERObject.NULLOBJECT); // kick off the process

            /* 1. RECEIVE PAYMENT REQUEST */
            NetworkParameters params = walletServiceBinder.getNetworkParameters();
            PaymentRequestReceiveStep paymentRequestReceive = new PaymentRequestReceiveStep(params);
            DERObject paymentRequest = readDERObject();
            paymentRequestReceive.process(paymentRequest);

            /* 2. AUTHORIZE REQUEST (by user) */
            BitcoinURI paymentRequestURI = paymentRequestReceive.getBitcoinURI();
            Log.d(TAG, "got request, authorizing user: " + paymentRequestURI);
            boolean isAuthorized = paymentRequestDelegate.isPaymentRequestAuthorized(paymentRequestURI);
            if (!isAuthorized) {
                Log.d(TAG, "Payment not authorized.");
                // TODO: we should notify server about this?
                return;
            }

            /* 3. SEND PAYMENT RESPONSE */
            PaymentResponseSendStep paymentResponseSend = new PaymentResponseSendStep(paymentRequestURI, walletServiceBinder);
            DERObject paymentResponse = paymentResponseSend.process(DERObject.NULLOBJECT);
            writeDERObject(paymentResponse);

            /* 4. RECEIVE SIGANTURES */
            DERObject serverSignatures = readDERObject();
            PaymentServerSignatureReceiveStep paymentServerSignatures = new PaymentServerSignatureReceiveStep();
            paymentServerSignatures.process(serverSignatures);

            /* 5. FINALIZE PAYMENT (TX) */
            PaymentFinalizeStep finalizeStep = new PaymentFinalizeStep(
                    paymentRequestURI,
                    paymentResponseSend.getTransaction(),
                    paymentResponseSend.getClientTransactionSignatures(),
                    paymentServerSignatures.getServerTransactionSignatures(),
                    walletServiceBinder);
            finalizeStep.process(DERObject.NULLOBJECT);


            writeDERObject(DERObject.NULLOBJECT); // ack
            closeStreams();

            Transaction transaction = finalizeStep.getTransaction();

            walletServiceBinder.maybeCommitAndBroadcastTransaction(transaction);

            Log.d(TAG, "payment successful!");
            paymentRequestDelegate.onPaymentSuccess();

        } catch (Exception e) {
            Log.e(TAG, "Payment failed due to exception: ", e);
            paymentRequestDelegate.onPaymentError(e.getMessage());
        }
    }
}