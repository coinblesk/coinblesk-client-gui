package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.papers.payments.Constants;
import ch.papers.payments.Utils;
import ch.papers.payments.communications.messages.DERInteger;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERSequence;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRequestReceiveStep implements Step{
    private final static String TAG = PaymentRequestReceiveStep.class.getSimpleName();

    private final ECKey ecKey;
    private BitcoinURI bitcoinURI;

    public PaymentRequestReceiveStep(ECKey ecKey){
        this.ecKey = ecKey;
    }

    public BitcoinURI getBitcoinURI(){
        return bitcoinURI;
    }

    @Override
    public DERObject process(DERObject input) {
        final DERSequence derSequence = (DERSequence) input;
        final Coin amount = Coin.valueOf(((DERInteger) derSequence.getChildren().get(0)).getBigInteger().longValue());
        Log.d(TAG,"received amount:"+amount);
        final Address address = new Address(Constants.PARAMS,derSequence.getChildren().get(1).getPayload());
        Log.d(TAG,"received address:"+address);

        try {
            this.bitcoinURI = new BitcoinURI(BitcoinURI.convertToBitcoinURI(address,amount,"", ""));
            Log.d(TAG,"bitcoin uri complete:"+bitcoinURI);
        } catch (BitcoinURIParseException e) {
            e.printStackTrace();
        }

        final BigInteger timestamp = BigInteger.valueOf(System.currentTimeMillis());
        Log.d(TAG,"sign timestamp:"+timestamp.longValue());


        Log.d(TAG,"used used for signing"+ecKey.getPublicKeyAsHex());
        Sha256Hash inputHash = Sha256Hash.of(Utils.concatBytes(BigInteger.valueOf(amount.getValue()).toByteArray(),address.getHash160(),timestamp.toByteArray()));
        Log.d(TAG,"hash used for signing"+inputHash);
        final ECKey.ECDSASignature ecdsaSignature = this.ecKey.sign(inputHash);

        final List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERObject(this.ecKey.getPubKey()));
        derObjectList.add(new DERInteger(timestamp));
        derObjectList.add(new DERInteger(ecdsaSignature.r));
        derObjectList.add(new DERInteger(ecdsaSignature.s));

        final DERSequence payloadDerSequence = new DERSequence(derObjectList);
        Log.d(TAG,"responding with eckey and signature total size:"+payloadDerSequence.serializeToDER().length);
        return payloadDerSequence;
    }
}
