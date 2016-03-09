package ch.papers.payments.communications.peers.steps;

import android.util.Log;

import com.coinblesk.json.PrepareHalfSignTO;
import com.coinblesk.util.SerializeUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.BloomFilter;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.papers.payments.Constants;
import ch.papers.payments.communications.messages.DERInteger;
import ch.papers.payments.communications.messages.DERObject;
import ch.papers.payments.communications.messages.DERSequence;

/**
 * Created by Alessandro De Carli (@a_d_c_) on 28/02/16.
 * Papers.ch
 * a.decarli@papers.ch
 */
public class PaymentRequestReceiveStep implements Step {
    private final static String TAG = PaymentRequestReceiveStep.class.getSimpleName();

    private final ECKey ecKey;
    private BitcoinURI bitcoinURI;
    private final List<TransactionOutput> unspentOutputs;

    public PaymentRequestReceiveStep(ECKey ecKey, List<TransactionOutput> unspentOutputs) {
        this.ecKey = ecKey;
        this.unspentOutputs = unspentOutputs;
    }

    public BitcoinURI getBitcoinURI() {
        return bitcoinURI;
    }

    @Override
    public DERObject process(DERObject input) {
        final DERSequence derSequence = (DERSequence) input;
        final Coin amount = Coin.valueOf(((DERInteger) derSequence.getChildren().get(0)).getBigInteger().longValue());
        Log.d(TAG, "received amount:" + amount);

        Address address;
        if (((DERInteger) derSequence.getChildren().get(1)).getBigInteger().longValue() == 1) {
            address = Address.fromP2SHHash(Constants.PARAMS, derSequence.getChildren().get(2).getPayload());
        } else {
            address = new Address(Constants.PARAMS, derSequence.getChildren().get(2).getPayload());
        }
        Log.d(TAG, "received address:" + address);


        try {
            this.bitcoinURI = new BitcoinURI(BitcoinURI.convertToBitcoinURI(address, amount, "", ""));
            Log.d(TAG, "bitcoin uri complete:" + bitcoinURI);
        } catch (BitcoinURIParseException e) {
            e.printStackTrace();
        }

        final BigInteger timestamp = BigInteger.valueOf(System.currentTimeMillis());
        Log.d(TAG, "sign timestamp:" + timestamp.longValue());

        final BloomFilter bloomFilter = new BloomFilter(unspentOutputs.size(), 0.001, 42);
        for (TransactionOutput transactionOutput:unspentOutputs) {
            bloomFilter.insert(transactionOutput.getOutPointFor().unsafeBitcoinSerialize());
        }

        PrepareHalfSignTO prepareHalfSignTO = new PrepareHalfSignTO()
                .amountToSpend(amount.longValue())
                .clientPublicKey(this.ecKey.getPubKey())
                .p2shAddressTo(address.toString())
                .bloomFilter(bloomFilter.unsafeBitcoinSerialize())
                .messageSig(null)
                .currentDate(timestamp.longValue());
        SerializeUtils.sign(prepareHalfSignTO, ecKey);

        final List<DERObject> derObjectList = new ArrayList<DERObject>();
        derObjectList.add(new DERObject(this.ecKey.getPubKey()));
        derObjectList.add(new DERInteger(timestamp));
        derObjectList.add(new DERInteger(new BigInteger(prepareHalfSignTO.messageSig().sigR())));
        derObjectList.add(new DERInteger(new BigInteger(prepareHalfSignTO.messageSig().sigS())));
        derObjectList.add(new DERObject(bloomFilter.unsafeBitcoinSerialize()));

        final DERSequence payloadDerSequence = new DERSequence(derObjectList);
        Log.d(TAG, "responding with eckey and signature total size:" + payloadDerSequence.serializeToDER().length);
        return payloadDerSequence;
    }
}
