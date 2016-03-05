package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import org.bitcoinj.uri.BitcoinURI;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.papers.payments.communications.messages.DERInteger;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERSequence;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRequestSendStep implements Step {
    private final static String TAG = PaymentRequestSendStep.class.getSimpleName();
    final private DERObject payload;

    public PaymentRequestSendStep(BitcoinURI bitcoinURI){
        Log.d(TAG,"sending" + bitcoinURI);
        List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERInteger(BigInteger.valueOf(bitcoinURI.getAmount().getValue())));
        derObjectList.add(new DERInteger(BigInteger.valueOf(bitcoinURI.getAddress().isP2SHAddress()?1:0)));
        derObjectList.add(new DERObject(bitcoinURI.getAddress().getHash160()));
        payload = new DERSequence(derObjectList);
        Log.d(TAG,"payload size" + payload.serializeToDER().length);
    }

    @Override
    public DERObject process(DERObject input) {
        return payload;
    }
}
