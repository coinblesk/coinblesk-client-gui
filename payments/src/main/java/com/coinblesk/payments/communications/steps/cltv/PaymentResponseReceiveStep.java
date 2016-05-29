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
import android.util.Log;
import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.der.DERObject;
import com.coinblesk.client.utils.DERPayloadBuilder;
import com.coinblesk.client.utils.DERPayloadParser;
import com.coinblesk.der.DERSequence;
import com.coinblesk.util.SerializeUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.uri.BitcoinURI;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentResponseReceiveStep extends AbstractStep {
    private final static String TAG = PaymentResponseReceiveStep.class.getName();

    public PaymentResponseReceiveStep(BitcoinURI bitcoinURI) {
        super(bitcoinURI);
    }

    @Override
    @NonNull
    public DERObject process(@NonNull DERObject input) throws PaymentException {
        final SignTO signTO;
        final ECKey clientPublicKey;
        try {
            DERSequence inputSequence = (DERSequence) input;
            DERPayloadParser parser = new DERPayloadParser(inputSequence);
            signTO = extractSignTO(parser);
            clientPublicKey = ECKey.fromPublicOnly(signTO.publicKey());
        } catch (Exception e) {
            throw new PaymentException(PaymentError.DER_SERIALIZE_ERROR, e);
        }

        Log.d(TAG, String.format("Got signTO: pubKey of message: %s, address(hex): %s, timestamp of signing: %s",
                clientPublicKey.getPublicKeyAsHex(), getBitcoinURI().getAddress(), signTO.currentDate()));

        if (!SerializeUtils.verifyJSONSignature(signTO, clientPublicKey)) {
            throw new PaymentException(PaymentError.MESSAGE_SIGNATURE_ERROR);
        }
        Log.d(TAG, "signTO - verify successful");

        final SignTO serverSignTO = serverSignVerify(signTO);

        try {
            DERPayloadBuilder builder = new DERPayloadBuilder();
            appendSignTO(builder, serverSignTO);
            DERObject response = builder.getAsDERSequence();
            return response;
        } catch (Exception e) {
            throw new PaymentException(PaymentError.DER_SERIALIZE_ERROR, e);
        }
    }

    @NonNull
    private SignTO serverSignVerify(SignTO signTO) throws PaymentException {
        final Response<SignTO> serverResponse;
        SignTO serverSignTO;
        try {
            final CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
            serverResponse = service.signVerify(signTO).execute();
        } catch (IOException e) {
            throw new PaymentException(PaymentError.SERVER_ERROR, e.getMessage());
        }

        // TODO: IF payment was successful but tag is lost now --> payment went through even tough tag is lost exception is thrown (but client will not get Tx/signatures from server).
        if (!serverResponse.isSuccess()) {
            throw new PaymentException(PaymentError.SERVER_ERROR,
                    "HTTP code: " + serverResponse.code());
        }

        serverSignTO = serverResponse.body();
        if (!SerializeUtils.verifyJSONSignature(serverSignTO,
                                ECKey.fromPublicOnly(serverSignTO.publicKey()))) {
            throw new PaymentException(PaymentError.MESSAGE_SIGNATURE_ERROR);
        }

        if (!serverSignTO.isSuccess()) {
            throw new PaymentException(PaymentError.SERVER_ERROR,
                    "Code: " + serverSignTO.type().toString());
        }

        return serverSignTO;
    }

    private void appendSignTO(DERPayloadBuilder builder, SignTO signTO) {
        builder
            .add(signTO.type().nr())
            .add(signTO.signatures());
    }

    protected SignTO extractSignTO(DERPayloadParser parser) {
        long currentDate = parser.getLong();
        byte[] publicKey = parser.getBytes();
        byte[] serializedTx = parser.getBytes();
        List<TxSig> signatures = parser.getTxSigList();
        TxSig messageSig = parser.getTxSig();

        // TODO: check that (1) payment is sent to my address and (2) amount is correct.
        // maybe do it after receiving request from server.
        // Transaction tx;
        // TransactioOutput = txOut;
        // if (txOut.getValue() != getBitcoinURI().getAmount().longValue() || !txOut.get...equals(getBitcoinURI().getAddress()) {
        //      fail!
        // }


        SignTO signTO = new SignTO()
                .currentDate(currentDate)
                .publicKey(publicKey)
                .transaction(serializedTx)
                .signatures(signatures)
                .messageSig(messageSig);
        return signTO;
    }
}