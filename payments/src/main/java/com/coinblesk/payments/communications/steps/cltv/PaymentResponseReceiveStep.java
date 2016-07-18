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

import com.coinblesk.json.v1.SignVerifyTO;
import com.coinblesk.json.v1.TxSig;
import com.coinblesk.client.config.Constants;
import com.coinblesk.json.v1.Type;
import com.coinblesk.payments.WalletService;
import com.coinblesk.payments.communications.PaymentError;
import com.coinblesk.payments.communications.PaymentException;
import com.coinblesk.client.CoinbleskWebService;
import com.coinblesk.der.DERObject;
import com.coinblesk.client.utils.DERPayloadBuilder;
import com.coinblesk.client.utils.DERPayloadParser;
import com.coinblesk.der.DERSequence;
import com.coinblesk.util.SerializeUtils;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.uri.BitcoinURI;
import retrofit2.Response;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alessandro De Carli
 * @author Andreas Albrecht
 */
public class PaymentResponseReceiveStep extends AbstractStep {
    private final static String TAG = PaymentResponseReceiveStep.class.getName();
    private final WalletService.WalletServiceBinder walletService;
    private boolean verifyPayeeSig = true;

    public PaymentResponseReceiveStep(BitcoinURI bitcoinURI, WalletService.WalletServiceBinder walletService) {
        super(bitcoinURI);
        this.walletService = walletService;
    }

    @Override
    @NonNull
    public DERObject process(@NonNull DERObject input) throws PaymentException {
        final long startTime = System.currentTimeMillis();
        final SignVerifyTO signTO;
        final ECKey clientPublicKey;
        final ECKey myClientKey = walletService.getMultisigClientKey();
        try {
            DERSequence inputSequence = (DERSequence) input;
            DERPayloadParser parser = new DERPayloadParser(inputSequence);
            signTO = extractSignTO(parser);
            // TODO: check (1) address correct, (2) amount correct, (3) payee PubKey is mine.
            // add our public key and signature
            signSignTO(signTO, myClientKey);
            clientPublicKey = ECKey.fromPublicOnly(signTO.publicKey());
        } catch (Exception e) {
            throw new PaymentException(PaymentError.DER_SERIALIZE_ERROR, e);
        }

        Log.d(TAG, String.format("Got signTO: pubKey of message: %s, address(hex): %s, timestamp of signing: %s",
                clientPublicKey.getPublicKeyAsHex(), getBitcoinURI().getAddress(), signTO.currentDate()));

        /*
        // TODO: this verify fails because of payeeSig.
        if (!SerializeUtils.verifyJSONSignature(signTO, clientPublicKey)) {
            throw new PaymentException(PaymentError.MESSAGE_SIGNATURE_ERROR);
        }
        Log.d(TAG, "signTO - verify successful");
        */

        final SignVerifyTO serverSignTO = serverSignVerify(signTO);

        try {
            DERPayloadBuilder builder = new DERPayloadBuilder();
            appendSignTO(builder, serverSignTO);
            DERObject response = builder.getAsDERSequence();
            logStepProcess(startTime, input, response);
            return response;
        } catch (Exception e) {
            throw new PaymentException(PaymentError.DER_SERIALIZE_ERROR, e);
        }
    }

    private void signSignTO(SignVerifyTO signTO, ECKey signKey) {
        signTO.payeePublicKey(signKey.getPubKey());
        TxSig payeeSig = SerializeUtils.signJSONRaw(signTO, signKey);
        signTO.payeeMessageSig(payeeSig);
    }

    @NonNull
    private SignVerifyTO serverSignVerify(SignVerifyTO signTO) throws PaymentException {
        final Response<SignVerifyTO> serverResponse;
        long serverCallStart = System.currentTimeMillis();
        try {
            final CoinbleskWebService service = walletService.getCoinbleskService();
            serverResponse = service.signVerify(signTO).execute();
        } catch (IOException e) {
            throw new PaymentException(PaymentError.SERVER_ERROR, e.getMessage());
        } finally {
            long serverCallDuration = System.currentTimeMillis() - serverCallStart;
            Log.d(TAG, "Server call done in " + serverCallDuration + " ms");
        }

        if (!serverResponse.isSuccessful()) {
            throw new PaymentException(PaymentError.SERVER_ERROR,
                    "HTTP code: " + serverResponse.code());
        }

        final SignVerifyTO serverSignTO = serverResponse.body();

        // verify my signature (payee sig)
        TxSig payeeSig = serverSignTO.payeeMessageSig();
        serverSignTO.payeeMessageSig(null);
        if (verifyPayeeSig) {
            if(serverSignTO.payeePublicKey() == null) {
                Log.e(TAG, "server sig is null: "+serverSignTO.type());
            }
            ECKey payeeServerPubKey = ECKey.fromPublicOnly(serverSignTO.payeePublicKey());
            if (!Arrays.equals(
                    walletService.getMultisigServerKey().getPubKey(), payeeServerPubKey.getPubKey())) {
                throw new PaymentException(PaymentError.MESSAGE_SIGNATURE_ERROR);
            }
            if (!SerializeUtils.verifyJSONSignatureRaw(serverSignTO, payeeSig, payeeServerPubKey)) {
                throw new PaymentException(PaymentError.MESSAGE_SIGNATURE_ERROR);
            }
        }
        serverSignTO.payeePublicKey(null);

        if (!serverSignTO.isSuccess()) {
            throw new PaymentException(PaymentError.SERVER_ERROR,
                    "Code: " + serverSignTO.type().toString());
        }

        if (serverSignTO.type() == Type.SUCCESS_INSTANT) {
            String txHash = new Transaction(
                    walletService.getNetworkParameters(),
                    serverSignTO.transaction())
                    .getHashAsString();
            walletService.markTransactionInstant(txHash);
        }

        return serverSignTO;
    }

    private void appendSignTO(DERPayloadBuilder builder, SignVerifyTO signTO) {
        builder.add(signTO.type().nr())
                .add(signTO.signatures())
                .add(signTO.messageSig());
    }

    protected SignVerifyTO extractSignTO(DERPayloadParser parser) {
        long currentDate = parser.getLong();
        byte[] publicKey = parser.getBytes();
        byte[] serializedTx = parser.getBytes();
        List<TxSig> signatures = parser.getTxSigList();
        TxSig messageSig = parser.getTxSig();
        
        SignVerifyTO signTO = new SignVerifyTO()
                .currentDate(currentDate)
                .publicKey(publicKey)
                .transaction(serializedTx)
                .signatures(signatures)
                .messageSig(messageSig);

        Log.d(TAG, "sign TO: "+currentDate+" / "+publicKey+"/"+serializedTx+"/"+signatures+"/"+messageSig);

        return signTO;
    }

    public void verifyPayeeSig(boolean verifyPayeeSig) {
        this.verifyPayeeSig = verifyPayeeSig;
    }
}