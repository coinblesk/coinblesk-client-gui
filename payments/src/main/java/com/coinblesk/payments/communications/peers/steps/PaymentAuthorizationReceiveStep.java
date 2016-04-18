package com.coinblesk.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.payments.communications.messages.DERInteger;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERSequence;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.uri.BitcoinURI;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentAuthorizationReceiveStep implements Step {
    private final static String TAG = PaymentAuthorizationReceiveStep.class.getSimpleName();

    final private BitcoinURI bitcoinURI;
    private List<TxSig> serverSignatures = new ArrayList<TxSig>();

    private ECKey clientPublicKey;

    public PaymentAuthorizationReceiveStep(BitcoinURI bitcoinURI) {
        this.bitcoinURI = bitcoinURI;
    }

    public List<TxSig> getServerSignatures() {
        return serverSignatures;
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
                this.serverSignatures = serverHalfSignTO.serverSignatures();

                List<DERObject> derObjectList = new ArrayList<DERObject>();
                for (TransactionSignature signature : SerializeUtils.deserializeSignatures(serverHalfSignTO.serverSignatures())) {
                    List<DERObject> signatureList = ImmutableList.<DERObject>of(new DERInteger(signature.r),new DERInteger(signature.s));
                    derObjectList.add(new DERSequence(signatureList));
                }

                DERObject dersequence = new DERSequence(derObjectList);
                Log.d(TAG,"payload size:"+dersequence.serializeToDER().length);
                Log.d(TAG,"time:"+System.currentTimeMillis());
                return dersequence;
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception in the authorization step: ", e);
        }
        return new DERInteger(BigInteger.valueOf(-1));
    }

    public ECKey getClientPublicKey() {
        return this.clientPublicKey;
    }
}
