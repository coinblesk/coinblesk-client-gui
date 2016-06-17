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
import com.coinblesk.payments.communications.steps.cltv.PaymentRequestSendStep;
import com.coinblesk.payments.communications.steps.cltv.PaymentResponseReceiveStep;

import org.bitcoinj.uri.BitcoinURI;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Andreas Albrecht
 */
public class InstantPaymentServerHandlerCLTV extends DERObjectStreamHandler {
    private final static String TAG = InstantPaymentServerHandlerCLTV.class.getSimpleName();

    private final BitcoinURI paymentRequestURI;
    private final PaymentRequestDelegate paymentRequestDelegate;
    private final WalletService.WalletServiceBinder walletServiceBinder;

    public InstantPaymentServerHandlerCLTV(InputStream inputStream, OutputStream outputStream,
                                                       BitcoinURI paymentRequestURI,
                                                       PaymentRequestDelegate paymentRequestDelegate,
                                                       WalletService.WalletServiceBinder walletServiceBinder) {
        super(inputStream, outputStream);
        this.paymentRequestURI = paymentRequestURI;
        this.paymentRequestDelegate = paymentRequestDelegate;
        this.walletServiceBinder = walletServiceBinder;
    }

    @Override
    public void run() {
        final long startTime = System.currentTimeMillis();
        try {

            final PaymentRequestSendStep paymentRequestSend = new PaymentRequestSendStep(paymentRequestURI, walletServiceBinder.getMultisigClientKey());
            DERObject input = readDERObject(); // from kick off
            DERObject paymentRequest = paymentRequestSend.process(input);
            writeDERObject(paymentRequest);

            DERObject paymentResponse = readDERObject();
            PaymentResponseReceiveStep paymentResponseReceive = new PaymentResponseReceiveStep(
                    paymentRequestURI, walletServiceBinder);
            DERObject serverSignatures = paymentResponseReceive.process(paymentResponse);
            writeDERObject(serverSignatures);

            DERObject finalAck = readDERObject();
            closeStreams();
            /*
            // TODO: assemble tx? should we get the tx full tx and broadcast it?
            Transaction transaction;
            walletServiceBinder.commitAndBroadcastTransaction(transaction);
            */
            Log.d(TAG, "payment was successful!");
            paymentRequestDelegate.onPaymentSuccess();
        } catch (Exception e) {
            Log.e(TAG, "Payment failed due to exception: ", e);
            paymentRequestDelegate.onPaymentError(e.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            Log.d(TAG, "InstantPaymentServerHandlerCLTV completed in " + duration + "ms");
        }
    }
}