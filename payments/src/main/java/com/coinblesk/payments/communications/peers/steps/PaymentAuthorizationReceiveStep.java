package com.coinblesk.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.payments.communications.messages.DERInteger;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERParser;
import com.coinblesk.payments.communications.messages.DERSequence;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.coinblesk.payments.Constants;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentAuthorizationReceiveStep implements Step {
    private final static String TAG = PaymentAuthorizationReceiveStep.class.getSimpleName();

    final private BitcoinURI bitcoinURI;

    private ECKey clientPublicKey;


    public PaymentAuthorizationReceiveStep(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }

    @Override
    public DERObject process(DERObject input) {
        try {
            final DERSequence inputSequence = (DERSequence) input;
            clientPublicKey = ECKey.fromPublicOnly(inputSequence.getChildren().get(0).getPayload());
            byte[] transactionPayload = inputSequence.getChildren().get(1).getPayload();
            final BigInteger timestamp = ((DERInteger)inputSequence.getChildren().get(2)).getBigInteger();

            final TxSig txSig= new TxSig();
            txSig.sigR(((DERInteger) inputSequence.getChildren().get(3)).getBigInteger().toString());
            txSig.sigS(((DERInteger) inputSequence.getChildren().get(4)).getBigInteger().toString());


            Log.d(TAG,"key used for signing"+clientPublicKey.getPublicKeyAsHex());
            Log.d(TAG,"address used for signing"+bitcoinURI.getAddress());
            Log.d(TAG,"timestamp used for signing"+timestamp.longValue());

            SignTO refundTO = new SignTO()
                    .clientPublicKey(clientPublicKey.getPubKey())
                    .transaction(transactionPayload)
                    .messageSig(txSig)
                    .currentDate(timestamp.longValue());

            if (SerializeUtils.verifySig(refundTO,clientPublicKey)) {
                Log.d(TAG,"verify was successful!");
                refundTO.messageSig(txSig); //have to reset the txsig because verifySig is nulling it
                final CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
                // let server sign first
                final SignTO serverHalfSignTO = service.sign(refundTO).execute().body();

                List<DERObject> derObjectList = new ArrayList<DERObject>();
                for (TransactionSignature signature : SerializeUtils.deserializeSignatures(serverHalfSignTO.serverSignatures())) {
                    List<DERObject> signatureList = ImmutableList.<DERObject>of(new DERInteger(signature.r),new DERInteger(signature.s));
                    derObjectList.add(new DERSequence(signatureList));
                }

                byte[] dersequence = new DERSequence(derObjectList).serializeToDER();
                Log.d(TAG,"sending response"+dersequence.length);
                Log.d(TAG,"sending response exp"+ DERParser.extractPayloadEndIndex(dersequence));
                return new DERSequence(derObjectList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DERInteger(BigInteger.valueOf(-1));
    }

    public ECKey getClientPublicKey() {
        return this.clientPublicKey;
    }
}
