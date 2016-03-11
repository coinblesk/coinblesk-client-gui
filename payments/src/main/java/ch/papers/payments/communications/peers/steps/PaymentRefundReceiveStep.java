package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.SignTO;
import com.coinblesk.json.TxSig;
import com.coinblesk.util.SerializeUtils;
import com.google.common.collect.ImmutableList;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.TransactionSignature;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.papers.payments.Constants;
import ch.papers.payments.communications.http.CoinbleskWebService;
import ch.papers.payments.communications.messages.DERInteger;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERParser;
import ch.papers.payments.communications.messages.DERSequence;

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
                .clientPublicKey(multisigClientKey.getPubKey())
                .transaction(transactionPayload)
                .messageSig(txSig)
                .currentDate(timestamp.longValue());

        if (SerializeUtils.verifySig(refundTO,multisigClientKey)) {
            try {
                Log.d(TAG, "verify was successful!");
                refundTO.messageSig(txSig); //have to reset the txsig because verifySig is nulling it
                final CoinbleskWebService service = Constants.RETROFIT.create(CoinbleskWebService.class);
                // let server sign first
                final SignTO serverHalfSignTO = service.sign(refundTO).execute().body();

                List<DERObject> derObjectList = new ArrayList<DERObject>();
                for (TransactionSignature signature : SerializeUtils.deserializeSignatures(serverHalfSignTO.serverSignatures())) {
                    List<DERObject> signatureList = ImmutableList.<DERObject>of(new DERInteger(signature.r), new DERInteger(signature.s));
                    derObjectList.add(new DERSequence(signatureList));
                }

                byte[] dersequence = new DERSequence(derObjectList).serializeToDER();
                Log.d(TAG, "sending response" + dersequence.length);
                Log.d(TAG, "sending response exp" + DERParser.extractPayloadEndIndex(dersequence));
                return new DERSequence(derObjectList);
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        return DERObject.NULLOBJECT;
    }
}
