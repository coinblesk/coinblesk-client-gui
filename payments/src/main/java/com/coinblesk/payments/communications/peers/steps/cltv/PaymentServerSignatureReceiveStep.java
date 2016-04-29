package com.coinblesk.payments.communications.peers.steps.cltv;

import android.util.Log;
import com.coinblesk.json.TxSig;
import com.coinblesk.json.Type;
import com.coinblesk.payments.Constants;
import com.coinblesk.payments.communications.messages.DERObject;
import com.coinblesk.payments.communications.messages.DERSequence;
import com.coinblesk.payments.communications.peers.steps.AbstractStep;
import com.coinblesk.payments.communications.utils.DERPayloadParser;
import com.coinblesk.util.SerializeUtils;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.List;

/**
 * @author Andreas Albrecht
 */
public class PaymentServerSignatureReceiveStep extends AbstractStep {
    private final static String TAG = PaymentServerSignatureReceiveStep.class.getName();

    private List<TransactionSignature> serverTransactionSignatures;

    public PaymentServerSignatureReceiveStep() {
        super();
    }

    public List<TransactionSignature> getServerTransactionSignatures() {
        return serverTransactionSignatures;
    }

    @Override
    public DERObject process(DERObject input) {
        final DERSequence derSequence = (DERSequence) input;
        final DERPayloadParser parser = new DERPayloadParser(derSequence);
        final NetworkParameters params = Constants.PARAMS;
        final int protocolVersion = parser.getInt();
        isProtocolVersionSupported(protocolVersion); // TODO: compatibility check!

        Type responseType = Type.get(parser.getInt());
        if (responseType.isError()) {
            Log.w(TAG, "Server responded with an error: " + responseType);
            // TODO: error!
        }

        List<TxSig> serializedServerSigs = parser.getTxSigList();
        serverTransactionSignatures = SerializeUtils.deserializeSignatures(serializedServerSigs);
        setSuccess();
        return DERObject.NULLOBJECT;
    }
}
