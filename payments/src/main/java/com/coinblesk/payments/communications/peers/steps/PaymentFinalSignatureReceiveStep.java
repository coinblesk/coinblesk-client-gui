package com.coinblesk.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.TxSig;
import com.coinblesk.json.VerifyTO;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.communications.http.CoinbleskWebService;
import com.coinblesk.payments.communications.messages.DERInteger;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERSequence;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentFinalSignatureReceiveStep implements Step {
    private final static String TAG = PaymentFinalSignatureReceiveStep.class.getSimpleName();


    private final ECKey multisigClientKey;
    private final Address recipientAddress;
    private Transaction fullSignedTransaction;

    public PaymentFinalSignatureReceiveStep(ECKey multisigClientKey, Address recipientAddress) {
        this.multisigClientKey = multisigClientKey;
        this.recipientAddress = recipientAddress;
    }

    @Override
    public DERObject process(DERObject input) {
        try {
            final DERSequence derSequence = (DERSequence) input;
            this.fullSignedTransaction = new Transaction(Constants.PARAMS,derSequence.getChildren().get(0).getPayload());
            final BigInteger timestamp = ((DERInteger) derSequence.getChildren().get(1)).getBigInteger();
            final TxSig txSig = new TxSig();
            txSig.sigR(((DERInteger) derSequence.getChildren().get(2)).getBigInteger().toString());
            txSig.sigS(((DERInteger) derSequence.getChildren().get(3)).getBigInteger().toString());

            VerifyTO completeSignTO = new VerifyTO()
                    .clientPublicKey(multisigClientKey.getPubKey())
                    .fullSignedTransaction(fullSignedTransaction.unsafeBitcoinSerialize())
                    .messageSig(txSig)
                    .currentDate(timestamp.longValue());

            final CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
            VerifyTO responseCompleteSignTO = service.verify(completeSignTO).execute().body();
            Log.d(TAG, "instant payment was " + responseCompleteSignTO.type());
            return DERObject.NULLOBJECT;
        } catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }

    public Transaction getFullSignedTransaction() {
        return fullSignedTransaction;
    }
}
