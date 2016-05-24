package com.coinblesk.payments.communications.steps;

import android.util.Log;

import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.client.config.Constants;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.der.DERInteger;
import com.coinblesk.der.DERObject;
import com.coinblesk.der.DERSequence;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.TransactionSignature;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRefundReceiveStep implements Step {
    private final static String TAG = PaymentRefundReceiveStep.class.getSimpleName();

    private final ECKey multisigClientKey;

    public PaymentRefundReceiveStep(ECKey multisigClientKey) {
        this.multisigClientKey = multisigClientKey;
    }

    @Override
    public DERObject process(DERObject input) {
        Log.d(TAG,"received refund");
        Log.d(TAG,"my payload"+Arrays.toString(input.getPayload()));
        final DERSequence inputSequence = (DERSequence) input;
        byte[] transactionPayload = inputSequence.getChildren().get(0).getPayload();
        final BigInteger timestamp = ((DERInteger)inputSequence.getChildren().get(1)).getBigInteger();

        final TxSig txSig= new TxSig();
        txSig.sigR(((DERInteger) inputSequence.getChildren().get(2)).getBigInteger().toString());
        txSig.sigS(((DERInteger) inputSequence.getChildren().get(3)).getBigInteger().toString());

        SignTO refundTO = new SignTO()
                .publicKey(multisigClientKey.getPubKey())
                .transaction(transactionPayload)
                .messageSig(txSig)
                .currentDate(timestamp.longValue());

        if (SerializeUtils.verifyJSONSignature(refundTO,multisigClientKey)) {
            try {
                Log.d(TAG, "verify was successful!");
                refundTO.messageSig(txSig); //have to reset the txsig because verifySig is nulling it
                final CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
                // let server sign first
                final SignTO serverHalfSignTO = service.sign(refundTO).execute().body();

                List<DERObject> derObjectList = new ArrayList<DERObject>();
                for (TransactionSignature signature : SerializeUtils.deserializeSignatures(serverHalfSignTO.signatures())) {
                    List<DERObject> signatureList = ImmutableList.<DERObject>of(new DERInteger(signature.r), new DERInteger(signature.s));
                    derObjectList.add(new DERSequence(signatureList));
                }

                DERObject dersequence = new DERSequence(derObjectList);
                Log.d(TAG,"payload size:"+dersequence.serializeToDER().length);
                Log.d(TAG,"time:"+System.currentTimeMillis());
                return dersequence;
            } catch (IOException e){
                Log.e(TAG, "Exception in the refund step: ", e);
            }
        }

        return DERObject.NULLOBJECT;
    }
}
