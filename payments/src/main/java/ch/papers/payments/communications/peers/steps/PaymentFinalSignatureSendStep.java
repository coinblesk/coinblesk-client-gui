package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.CompleteSignTO;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;

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
public class PaymentFinalSignatureSendStep implements Step{
    private final static String TAG = PaymentFinalSignatureSendStep.class.getSimpleName();

    private final ECKey multisigClientKey;
    private final Address recipientAddress;
    private final Transaction transaction;

    public PaymentFinalSignatureSendStep(ECKey multisigClientKey, Address recipientAddress, Transaction transaction) {
        this.multisigClientKey = multisigClientKey;
        this.recipientAddress = recipientAddress;
        this.transaction = transaction;
    }


    @Override
    public DERObject process(DERObject input) {

        final BigInteger timestamp = BigInteger.valueOf(System.currentTimeMillis());
        CompleteSignTO completeSignTO = new CompleteSignTO()
                .clientPublicKey(multisigClientKey.getPubKey())
                .p2shAddressTo(recipientAddress.toString())
                .fullSignedTransaction(transaction.unsafeBitcoinSerialize())
                .currentDate(timestamp.longValue());
        if (completeSignTO.messageSig() == null) {
            SerializeUtils.sign(completeSignTO, multisigClientKey);
        }


        final List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERInteger(timestamp));
        derObjectList.add(new DERObject(transaction.unsafeBitcoinSerialize()));
        derObjectList.add(new DERInteger(new BigInteger(completeSignTO.messageSig().sigR())));
        derObjectList.add(new DERInteger(new BigInteger(completeSignTO.messageSig().sigS())));

        final DERSequence payloadDerSequence = new DERSequence(derObjectList);
        Log.d(TAG, "responding with eckey and signature total size:" + payloadDerSequence.serializeToDER().length);
        return payloadDerSequence;
    }
}
